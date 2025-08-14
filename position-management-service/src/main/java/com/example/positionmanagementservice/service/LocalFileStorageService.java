package com.example.positionmanagementservice.service;

import com.example.positionmanagementservice.config.FileStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

@Slf4j
@Service
@Profile("!cloud") // active when cloud profile is NOT set
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private final FileStorageProperties fileStorageProperties;

    private Path rootDirectory;

    @PostConstruct
    public void init() throws IOException {
        String path = fileStorageProperties.getLocal().getPath();
        this.rootDirectory = Paths.get(path);
        Files.createDirectories(rootDirectory);
        log.info("Initialized local file storage at: {}", rootDirectory.toAbsolutePath());
    }

    @Override
    public String save(MultipartFile file, String fileName) throws IOException {
        Path targetPath = rootDirectory.resolve(fileName).normalize();
        file.transferTo(targetPath);
        log.info("File saved to local path: {}", targetPath.toAbsolutePath());
        return targetPath.toAbsolutePath().toString();
    }

    @Override
    public Resource load(String path) throws IOException {
        Path file = Paths.get(path);
        if (!Files.exists(file)) {
            throw new NoSuchFileException("File not found at path: " + path);
        }
        try {
            return new UrlResource(file.toUri());
        } catch (MalformedURLException e) {
            throw new IOException("Invalid file path URI", e);
        }
    }

    @Override
    public void delete(String path) throws IOException {
        Path filePath = Paths.get(path);
        Files.deleteIfExists(filePath);
        log.info("Deleted file at path: {}", filePath.toAbsolutePath());
    }
}
