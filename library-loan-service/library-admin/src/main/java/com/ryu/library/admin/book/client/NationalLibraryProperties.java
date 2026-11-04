package com.ryu.library.admin.book.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "library.nl-api")
public record NationalLibraryProperties(String baseUrl, String certKey, Duration connectTimeout, Duration readTimeout) {

    public boolean isConfigured() {
        return certKey != null && !certKey.isBlank();
    }
}
