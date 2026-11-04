package com.ryu.ebook.admin.support;

import com.ryu.ebook.admin.global.config.SecurityConfig;
import com.ryu.ebook.admin.global.security.LoginAdminDetailsService;
import com.ryu.ebook.global.config.PasswordEncoderConfig;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// @WebMvcTest에 관리자 앱의 실제 보안 설정을 함께 올립니다.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, PasswordEncoderConfig.class})
@MockitoBean(types = LoginAdminDetailsService.class)
public @interface AdminWebSecurityTest {
}
