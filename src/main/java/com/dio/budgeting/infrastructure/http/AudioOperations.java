package com.dio.budgeting.infrastructure.http;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class AudioOperations {
    private final TranscriptionModel transcription;
    private final TextToSpeechModel speech;

    public AudioOperations(TranscriptionModel transcription, TextToSpeechModel speech) {
        this.transcription = transcription;
        this.speech = speech;
    }

    public String transcribe(MultipartFile file) {
        AudioValidation.validate(file);
        String text;
        try {
            text = transcription.transcribe(file.getResource());
        } catch (RuntimeException error) {
            throw failure("TRANSCRIPTION_FAILED", "Não foi possível transcrever o áudio.", false);
        }
        if (text == null || text.isBlank()) {
            throw failure("EMPTY_TRANSCRIPTION", "A transcrição não retornou texto.", false);
        }
        return text;
    }

    public byte[] synthesize(String text, boolean mayBeSaved) {
        if (text == null || text.isBlank()) {
            if (mayBeSaved) throw failure("EMPTY_RESPONSE", "A IA não retornou texto. Consulte as transações antes de reenviar.", true);
            throw new IllegalArgumentException("O texto para síntese é obrigatório.");
        }
        try {
            byte[] audio = speech.call(text);
            if (audio == null || audio.length == 0) throw new IllegalStateException();
            return audio;
        } catch (RuntimeException error) {
            throw failure("SPEECH_FAILED", mayBeSaved
                    ? "Falha ao gerar áudio. A transação pode ter sido salva; consulte as transações antes de reenviar."
                    : "Não foi possível gerar o áudio.", mayBeSaved);
        }
    }

    public byte[] process(MultipartFile file, ChatClient client) {
        String text = transcribe(file);
        String response;
        try {
            response = client.prompt().user(text).call().content();
        } catch (RuntimeException error) {
            throw failure("INTERPRETATION_FAILED", "Não foi possível concluir a interpretação. Uma ferramenta pode ter sido executada; consulte as transações antes de reenviar.", true);
        }
        return synthesize(response, true);
    }

    private ApiOperationException failure(String code, String message, boolean mayBeSaved) {
        return new ApiOperationException(HttpStatus.BAD_GATEWAY, code, message, mayBeSaved);
    }
}
