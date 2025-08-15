package com.example.assumptionmanagementservice.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String save(MultipartFile file, String assumptionSetName, String key) throws IOException;
    Resource load(String path) throws IOException;
    void delete(String path) throws IOException;
}

