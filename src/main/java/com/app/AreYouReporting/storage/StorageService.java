package com.app.AreYouReporting.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    String storeFile(MultipartFile file, String subFolder);

    Resource loadFileAsResource(String fileKey);

    void deleteFile(String fileKey);
}
