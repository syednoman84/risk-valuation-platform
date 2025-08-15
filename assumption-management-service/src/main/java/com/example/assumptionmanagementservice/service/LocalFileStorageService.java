package com.example.assumptionmanagementservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.*;

@Slf4j
@Service
@Profile("!cloud") // Use this unless 'cloud' profile is active
public class LocalFileStorageService implements FileStorageService {

    private final Path rootDirectory;

    public LocalFileStorageService(@Value("${storage.assumptions.directory:assumption_files}") String baseDir) throws IOException {
        this.rootDirectory = Paths.get(baseDir);
        Files.createDirectories(rootDirectory); // Ensure base directory exists
    }

    @Override
    public String save(MultipartFile file, String assumptionSetName, String key) throws IOException {
        // Create a subdirectory for each assumption set
        Path assumptionDir = rootDirectory.resolve(assumptionSetName);
        Files.createDirectories(assumptionDir);

        Path targetPath = assumptionDir.resolve(key + ".csv").normalize();
        file.transferTo(targetPath);

        log.info("Saved file under: {}", targetPath.toAbsolutePath());
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
        log.info("Deleted file at: {}", filePath.toAbsolutePath());
    }
}

