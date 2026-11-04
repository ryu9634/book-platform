package com.ryu.ebook.ebook.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ryu.ebook.global.exception.ErrorCode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.unit.DataSize;

class EbookFileStorageTest {

    @TempDir
    Path dir;

    private EbookFileStorage storage() {
        return new EbookFileStorage(new EbookStorageProperties(dir.toString(), DataSize.ofMegabytes(1), 10));
    }

    @Test
    @DisplayName("텍스트 파일을 무작위 이름으로 저장하고, 글자 수로 나눠 페이지를 읽는다")
    void store_readPage() {
        // given
        byte[] bytes = "가나다라마바사아자차카타파하".getBytes(StandardCharsets.UTF_8);

        // when
        EbookFileStorage.StoredFile stored = storage().store("본문.txt", bytes.length, new ByteArrayInputStream(bytes));

        // then
        assertThat(stored.pageCount()).isEqualTo(2);
        assertThat(stored.fileKey()).endsWith(".txt").doesNotContain("본문");
        assertThat(storage().readPage(stored.fileKey(), 2).content()).isEqualTo("카타파하");
        assertThat(storage().readPage(stored.fileKey(), 99).page()).isEqualTo(2);
    }

    @Test
    @DisplayName("텍스트가 아닌 파일, 너무 큰 파일, UTF-8이 아닌 파일, 저장소 밖 경로는 거부한다")
    void 거부() {
        byte[] bytes = "본문".getBytes(StandardCharsets.UTF_8);
        assertThatThrownBy(() -> storage().store("본문.pdf", bytes.length, new ByteArrayInputStream(bytes)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
        assertThatThrownBy(() -> storage().store("본문.txt", DataSize.ofMegabytes(2).toBytes(), new ByteArrayInputStream(bytes)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
        byte[] notUtf8 = {(byte) 0xC3, (byte) 0x28};
        assertThatThrownBy(() -> storage().store("본문.txt", 2, new ByteArrayInputStream(notUtf8)))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
        assertThatThrownBy(() -> storage().readPage("../../etc/passwd", 1))
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_FILE);
    }
}
