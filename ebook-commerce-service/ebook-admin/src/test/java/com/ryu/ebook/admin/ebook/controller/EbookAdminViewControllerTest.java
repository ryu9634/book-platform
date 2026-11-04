package com.ryu.ebook.admin.ebook.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ryu.ebook.admin.ebook.service.EbookAdminService;
import com.ryu.ebook.admin.support.AdminWebSecurityTest;
import com.ryu.ebook.global.exception.BusinessException;
import com.ryu.ebook.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EbookAdminViewController.class)
@AdminWebSecurityTest
class EbookAdminViewControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    EbookAdminService ebookAdminService;

    @Test
    @DisplayName("필수 값이 빠지면 등록하지 않고 폼에 오류를 보여 준다")
    void register_필수값누락() throws Exception {
        mockMvc.perform(post("/admin/ebooks").with(user("admin").roles("ADMIN")).with(csrf()).param("title", ""))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("ebookForm", "title", "author", "price", "salePrice"));
        verify(ebookAdminService, never()).register(any());
    }

    @Test
    @DisplayName("본문 파일을 multipart로 올리고, 거부되면 상세 화면에 이유를 보여 준다")
    void uploadFile_성공_거부() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("file", "본문.txt", "text/plain", "본문".getBytes());
        mockMvc.perform(multipart("/admin/ebooks/1/file").file(txt).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/ebooks/1"))
                .andExpect(flash().attribute("message", "본문 파일을 올렸습니다."));

        willThrow(new BusinessException(ErrorCode.INVALID_FILE, "텍스트(.txt) 파일만 올릴 수 있습니다."))
                .given(ebookAdminService).uploadFile(eq(2L), any(), anyLong(), any());
        mockMvc.perform(multipart("/admin/ebooks/2/file").file(txt).with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(redirectedUrl("/admin/ebooks/2"))
                .andExpect(flash().attribute("errorMessage", "텍스트(.txt) 파일만 올릴 수 있습니다."));
    }

    @Test
    @DisplayName("CSRF 토큰 없이 파일을 올리면 처리하지 않고 로그인 화면으로 보낸다")
    void uploadFile_CSRF없음_처리안함() throws Exception {
        MockMultipartFile txt = new MockMultipartFile("file", "본문.txt", "text/plain", "본문".getBytes());
        mockMvc.perform(multipart("/admin/ebooks/1/file").file(txt).with(user("admin").roles("ADMIN")))
                .andExpect(redirectedUrl("/admin/login?expired"));
        then(ebookAdminService).shouldHaveNoInteractions();
    }
}
