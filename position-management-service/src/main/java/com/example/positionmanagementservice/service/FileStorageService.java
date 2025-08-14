package com.example.positionmanagementservice.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String save(MultipartFile file, String fileName) throws IOException;
    Resource load(String path) throws IOException;
    void delete(String path) throws IOException;
}

