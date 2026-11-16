package com.crsms.controller;

import com.crsms.dto.ApiDtos.UploadResponse;
import com.crsms.exception.ApiException;
import com.crsms.service.CurrentUserService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {
    private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private final CurrentUserService current;
    private final Path directory;

    public UploadController(CurrentUserService current, @Value("${crsms.uploads-dir:data/uploads}") String directory) {
        this.current = current;
        this.directory = Path.of(directory);
    }

    @PostMapping
    public UploadResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
        current.require();
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "Choose an image to upload");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = extension(original);
        if (!ALLOWED.contains(ext)) {
            throw new ApiException(400, "Photographs must be JPG, PNG, GIF, or WEBP");
        }
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!type.startsWith("image/")) {
            throw new ApiException(400, "Only image uploads are accepted");
        }
        Files.createDirectories(directory);
        String name = UUID.randomUUID() + "." + ext;
        file.transferTo(directory.resolve(name));
        return new UploadResponse("/uploads/" + name);
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
