package com.dio.budgeting;

import com.dio.budgeting.application.PersistTransactionUseCase;
import com.dio.budgeting.application.input.PersistTransactionInput;
import com.dio.budgeting.domain.Category;
import com.dio.budgeting.domain.TransactionRepository;
import com.dio.budgeting.infrastructure.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class InputValidationTest {
    @Test
    void doesNotRetryACompletedToolWhenSpeechFails() {
        var transcription = mock(TranscriptionModel.class);
        var speech = mock(TextToSpeechModel.class);
        var client = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        var repository = mock(TransactionRepository.class);
        var useCase = new PersistTransactionUseCase(repository);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(transcription.transcribe(any())).thenReturn("Gastei 80 reais no mercado");
        when(client.prompt().user("Gastei 80 reais no mercado").call().content()).thenAnswer(invocation -> {
            useCase.execute(new PersistTransactionInput("Mercado", 8000, Category.GROCERIES));
            return "Registrado";
        });
        when(speech.call("Registrado")).thenThrow(new RuntimeException("indisponível"));
        var operations = new AudioOperations(transcription, speech);
        assertThatThrownBy(() -> operations.process(new MockMultipartFile("file", "file.wav", "audio/wav", new byte[]{1}), client))
                .satisfies(error -> assertThat(((ApiOperationException) error).transactionMayHaveBeenSaved()).isTrue());
        verify(repository, times(1)).save(any());
    }

    @Test
    void rejectsInvalidTransactionsBeforePersistence() {
        var repository = mock(TransactionRepository.class);
        var useCase = new PersistTransactionUseCase(repository);
        PersistTransactionInput[] inputs = {
                null, new PersistTransactionInput(null, 1, Category.AUTO),
                new PersistTransactionInput(" ", 1, Category.AUTO),
                new PersistTransactionInput("x".repeat(256), 1, Category.AUTO),
                new PersistTransactionInput("Compra", 0, Category.AUTO),
                new PersistTransactionInput("Compra", -1, Category.AUTO),
                new PersistTransactionInput("Compra", 1, null)
        };
        for (var input : inputs) assertThatThrownBy(() -> useCase.execute(input)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void validatesAudioBeforeCallingProviders() {
        var transcription = mock(TranscriptionModel.class);
        var speech = mock(TextToSpeechModel.class);
        var operations = new AudioOperations(transcription, speech);
        var chat = mock(ChatClient.class);
        assertThatThrownBy(() -> operations.process(new MockMultipartFile("file", new byte[0]), chat))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> operations.process(new MockMultipartFile("file", "file.txt", "text/plain", new byte[]{1}), chat))
                .isInstanceOf(ApiOperationException.class);
        assertThatThrownBy(() -> operations.process(new MockMultipartFile("file", "file.mp3", "audio/mpeg",
                new byte[(int) AudioValidation.MAX_BYTES + 1]), chat)).isInstanceOf(ApiOperationException.class);
        AudioValidation.validate(new MockMultipartFile("file", "recording.M4A", "audio/mp4", new byte[]{1}));
        verifyNoInteractions(transcription, speech, chat);
    }

    @Test
    void reportsEmptyTranscriptionAndSpeechFailureWithoutLeakingProviderErrors() {
        var transcription = mock(TranscriptionModel.class);
        var speech = mock(TextToSpeechModel.class);
        var operations = new AudioOperations(transcription, speech);
        when(transcription.transcribe(any())).thenReturn(" ");
        assertThatThrownBy(() -> operations.transcribe(new MockMultipartFile("file", "file.wav", "audio/wav", new byte[]{1})))
                .isInstanceOf(ApiOperationException.class).hasMessage("A transcrição não retornou texto.");
        when(speech.call("Registrado")).thenThrow(new RuntimeException("provider-secret"));
        assertThatThrownBy(() -> operations.synthesize("Registrado", true)).satisfies(error -> {
            var failure = (ApiOperationException) error;
            assertThat(failure.transactionMayHaveBeenSaved()).isTrue();
            assertThat(failure.getMessage()).contains("consulte").doesNotContain("provider-secret");
        });
        assertThatThrownBy(() -> operations.synthesize(null, true)).isInstanceOf(ApiOperationException.class);
    }

    @Test
    void returnsJsonErrorsForInvalidHttpInputs() throws Exception {
        var operations = new AudioOperations(mock(TranscriptionModel.class), mock(TextToSpeechModel.class));
        MockMvc mvc = standaloneSetup(new TranscriptionController(operations), new TextToSpeechController(operations))
                .setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(multipart("/api/transcribe").file(new MockMultipartFile("file", new byte[0])))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(multipart("/api/transcribe"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(post("/api/sinthesize").accept("audio/mp3").contentType("application/json").content("{\"text\":\" \"}"))
                .andExpect(status().isBadRequest()).andExpect(content().contentType("application/json"));
        mvc.perform(post("/api/sinthesize").contentType("application/json").content("{"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        mvc.perform(post("/api/sinthesize").accept("application/json").contentType("application/json")
                .content("{\"text\":\"Teste\"}"))
                .andExpect(status().isNotAcceptable()).andExpect(jsonPath("$.code").value("NOT_ACCEPTABLE"));
    }

    @Test
    void rejectsInvalidRestTransactionsAndCategory() throws Exception {
        var repository = mock(TransactionRepository.class);
        var transcription = mock(TranscriptionModel.class);
        var speech = mock(TextToSpeechModel.class);
        var controller = new TransactionController(new PersistTransactionUseCase(repository),
                new com.dio.budgeting.application.ListTransactionsByCategoryUseCase(repository),
                transcription, new ClassPathResource("prompts/system-message.st"),
                mock(ChatClient.Builder.class, RETURNS_DEEP_STUBS), speech,
                new AudioOperations(transcription, speech));
        var mvc = standaloneSetup(controller).setControllerAdvice(new ApiExceptionHandler()).build();
        mvc.perform(post("/transactions").contentType("application/json")
                .content("{\"description\":\"Compra\",\"amount\":0,\"category\":\"AUTO\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_INPUT"));
        mvc.perform(get("/transactions/INVALID"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
        verifyNoInteractions(repository);
    }
}
