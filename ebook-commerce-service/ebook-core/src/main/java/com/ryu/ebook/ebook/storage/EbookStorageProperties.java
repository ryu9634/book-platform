package com.ryu.ebook.ebook.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties(prefix = "ebook.storage")
public record EbookStorageProperties(String dir, DataSize maxFileSize, int charsPerPage) {
}
