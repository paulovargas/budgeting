package com.dio.budgeting.infrastructure.http;

import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public final class AudioValidation {
    private static final Set<String> EXTENSIONS = Set.of("mp3", "mp4", "mpeg", "mpga", "m4a", "wav", "webm");
    public static final long MAX_BYTES = 10 * 1024 * 1024;

    private AudioValidation() {}

    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Envie um arquivo de áudio não vazio no campo file.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ApiOperationException(HttpStatus.PAYLOAD_TOO_LARGE, "AUDIO_TOO_LARGE",
                    "O áudio deve ter no máximo 10 MB.", false);
        }
        String name = file.getOriginalFilename();
        String extension = name == null ? "" : name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!EXTENSIONS.contains(extension)) {
            throw new ApiOperationException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "UNSUPPORTED_AUDIO",
                    "Formato aceito: mp3, mp4, mpeg, mpga, m4a, wav ou webm.", false);
        }
    }
}
