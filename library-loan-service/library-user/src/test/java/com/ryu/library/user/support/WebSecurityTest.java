package com.ryu.library.user.support;

import com.ryu.library.global.config.ClockConfig;
import com.ryu.library.global.config.PasswordEncoderConfig;
import com.ryu.library.user.global.config.SecurityConfig;
import com.ryu.library.user.global.security.JwtConfig;
import com.ryu.library.user.global.security.LoginMemberDetailsService;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

// @WebMvcTest에 실제 보안 설정(세션 로그인 + JWT)을 함께 올립니다. JWT 서명 키는 실행할 때마다 무작위로 만듭니다.
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({SecurityConfig.class, PasswordEncoderConfig.class, JwtConfig.class, ClockConfig.class})
@TestPropertySource(properties = "library.jwt.secret=${random.uuid}${random.uuid}")
@MockitoBean(types = LoginMemberDetailsService.class)
public @interface WebSecurityTest {
}
