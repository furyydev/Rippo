package com.rippo.backend.cache.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rippo.cache")
public class CacheProperties {

    private Duration defaultTtl = Duration.ofMinutes(10);

    private Duration repositoryMetadataTtl = Duration.ofHours(1);

    private Duration readmeTtl = Duration.ofHours(6);

    private Duration directoryTtl = Duration.ofHours(1);

    private Duration fileTtl = Duration.ofHours(6);

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Duration getRepositoryMetadataTtl() {
        return repositoryMetadataTtl;
    }

    public void setRepositoryMetadataTtl(Duration repositoryMetadataTtl) {
        this.repositoryMetadataTtl = repositoryMetadataTtl;
    }

    public Duration getReadmeTtl() {
        return readmeTtl;
    }

    public void setReadmeTtl(Duration readmeTtl) {
        this.readmeTtl = readmeTtl;
    }

    public Duration getDirectoryTtl() {
        return directoryTtl;
    }

    public void setDirectoryTtl(Duration directoryTtl) {
        this.directoryTtl = directoryTtl;
    }

    public Duration getFileTtl() {
        return fileTtl;
    }

    public void setFileTtl(Duration fileTtl) {
        this.fileTtl = fileTtl;
    }
}
