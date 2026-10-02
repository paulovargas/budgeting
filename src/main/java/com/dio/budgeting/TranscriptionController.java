package com.dio.budgeting;

import org.springframework.ai.audio.transcription.TranscriptionModel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.dio.budgeting.infrastructure.http.AudioOperations;

@RestController
@RequestMapping("/api")
public class TranscriptionController {
    private final AudioOperations audioOperations;

    public TranscriptionController(AudioOperations audioOperations) {
        this.audioOperations = audioOperations;
    }

    @PostMapping(value = "/transcribe", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    String transcribe(@RequestParam("file") MultipartFile file){

        return audioOperations.transcribe(file);
    }
}
