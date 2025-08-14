package com.example.positionmanagementservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    private String type; // local or s3

    private Local local = new Local();
    private S3 s3 = new S3();

    @Data
    public static class Local {
        private String path; // e.g., position_files
    }

    @Data
    public static class S3 {
        private String bucketName;
        private String region;
        private String prefix;
    }
}
