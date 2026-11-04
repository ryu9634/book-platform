package com.ryu.ebook.ebook.storage;

import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

// 전자책 본문 파일 저장소(로컬 디스크). 관리자 앱은 저장하고 회원 앱은 뷰어에서 읽습니다.
// 학습용으로 UTF-8 텍스트 파일만 받고, 일정 글자 수로 나눠 페이지를 만듭니다. S3로 바꾸려면 이 클래스만 바꾸면 됩니다.
@Slf4j
@Component
@EnableConfigurationProperties(EbookStorageProperties.class)
public class EbookFileStorage {

    private final EbookStorageProperties properties;

    public EbookFileStorage(EbookStorageProperties properties) {
        this.properties = properties;
    }

    public StoredFile store(String originalFilename, long size, InputStream content) {
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".txt")) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "텍스트(.txt) 파일만 올릴 수 있습니다.");
        }
        if (size <= 0 || size > properties.maxFileSize().toBytes()) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "파일 크기는 " + properties.maxFileSize().toMegabytes() + "MB 이하여야 합니다.");
        }
        try {
            byte[] bytes = content.readAllBytes();
            String text = decodeUtf8(bytes);
            // 저장 이름은 원래 파일 이름 대신 무작위 키를 씁니다(경로 조작과 이름 충돌 방지).
            String fileKey = UUID.randomUUID() + ".txt";
            Path dir = Path.of(properties.dir());
            Files.createDirectories(dir);
            Files.write(dir.resolve(fileKey), bytes);
            int pageCount = Math.max(1, (int) Math.ceil(text.length() / (double) properties.charsPerPage()));
            log.info("ebook file stored. fileKey={}, size={}, pageCount={}", fileKey, bytes.length, pageCount);
            return new StoredFile(fileKey, bytes.length, pageCount);
        } catch (IOException e) {
            log.error("ebook file store failed", e);
            throw new BusinessException(ErrorCode.INVALID_FILE, "파일을 저장하지 못했습니다.");
        }
    }

    // page는 1부터 시작합니다. 범위를 벗어나면 첫 페이지나 마지막 페이지로 맞춥니다.
    public Page readPage(String fileKey, int page) {
        String text = readAll(fileKey);
        int perPage = properties.charsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil(text.length() / (double) perPage));
        int current = Math.min(Math.max(page, 1), totalPages);
        int from = (current - 1) * perPage;
        return new Page(current, totalPages, text.substring(from, Math.min(text.length(), from + perPage)));
    }

    private String readAll(String fileKey) {
        Path path = Path.of(properties.dir()).resolve(fileKey).normalize();
        if (!path.startsWith(Path.of(properties.dir()).normalize()) || !Files.exists(path)) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "본문 파일을 찾을 수 없습니다.");
        }
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("ebook file read failed. fileKey={}", fileKey, e);
            throw new BusinessException(ErrorCode.INVALID_FILE, "본문 파일을 읽지 못했습니다.");
        }
    }

    private static String decodeUtf8(byte[] bytes) {
        try {
            return StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .decode(java.nio.ByteBuffer.wrap(bytes))
                    .toString();
        } catch (CharacterCodingException e) {
            throw new BusinessException(ErrorCode.INVALID_FILE, "UTF-8 텍스트 파일만 올릴 수 있습니다.");
        }
    }

    public record StoredFile(String fileKey, long size, int pageCount) {
    }

    public record Page(int page, int totalPages, String content) {
    }
}
