package com.rippo.backend.cache.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rippo.cache")
public class CacheProperties {

    private Duration defaultTtl = Duration.ofMinutes(10);

    private Duration repositoryMetadataTtl = Duration.ofHours(1);

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
}
