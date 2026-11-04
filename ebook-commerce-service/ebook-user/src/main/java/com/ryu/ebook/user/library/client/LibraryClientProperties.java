package com.ryu.ebook.user.library.client;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ebook.library-api")
public record LibraryClientProperties(String baseUrl, Duration connectTimeout, Duration readTimeout, Duration cacheTtl) {
}
