package com.app.AreYouReporting.storage;

import com.app.AreYouReporting.exceptions.InvalidOperationException;
import com.app.AreYouReporting.exceptions.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.UUID;

@Service
@Slf4j
public class LocalStorageService implements StorageService {

    private final Path rootLocation;

    public LocalStorageService(@Value("${app.storage.upload-dir:uploads}") String uploadDir) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (Exception ex) {
            log.error("Could not create the directory where the uploaded files will be stored: {}", ex.getMessage());
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subFolder) {
        if (file == null || file.isEmpty()) {
            throw new InvalidOperationException("Failed to store empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "proof");
        if (originalFilename.contains("..")) {
            throw new InvalidOperationException("Cannot store file with relative path outside current directory: " + originalFilename);
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex);
        }

        String uniqueFileName = UUID.randomUUID() + extension;
        Path targetDir = subFolder != null && !subFolder.isBlank()
                ? this.rootLocation.resolve(subFolder).normalize()
                : this.rootLocation;

        try {
            Files.createDirectories(targetDir);
            Path targetLocation = targetDir.resolve(uniqueFileName);

            // Verify security (prevent path traversal)
            if (!targetLocation.startsWith(this.rootLocation)) {
                throw new InvalidOperationException("Cannot store file outside current storage directory");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return subFolder != null && !subFolder.isBlank()
                    ? subFolder + "/" + uniqueFileName
                    : uniqueFileName;
        } catch (IOException ex) {
            log.error("Failed to store file {}: {}", originalFilename, ex.getMessage());
            throw new InvalidOperationException("Failed to store file: " + originalFilename);
        }
    }

    @Override
    public Resource loadFileAsResource(String fileKey) {
        try {
            Path filePath = this.rootLocation.resolve(fileKey).normalize();
            if (!filePath.startsWith(this.rootLocation)) {
                throw new InvalidOperationException("Cannot access file outside current directory");
            }
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found: " + fileKey);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found: " + fileKey);
        }
    }

    @Override
    public void deleteFile(String fileKey) {
        try {
            Path filePath = this.rootLocation.resolve(fileKey).normalize();
            if (filePath.startsWith(this.rootLocation)) {
                Files.deleteIfExists(filePath);
            }
        } catch (IOException ex) {
            log.warn("Failed to delete file {}: {}", fileKey, ex.getMessage());
        }
    }
}
