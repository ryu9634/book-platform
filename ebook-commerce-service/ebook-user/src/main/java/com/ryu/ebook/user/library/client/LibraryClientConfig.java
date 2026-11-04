package com.ryu.ebook.user.library.client;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

// 도서관 서비스 호출 설정. 다른 서비스가 느려지면 이 앱의 요청 스레드까지 묶이지 않도록 짧은 타임아웃을 겁니다.
@Configuration
@EnableConfigurationProperties(LibraryClientProperties.class)
public class LibraryClientConfig {

    @Bean
    public LibraryBookClient libraryBookClient(RestClient.Builder builder, LibraryClientProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(properties.connectTimeout()).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        RestClient restClient = builder.baseUrl(properties.baseUrl()).requestFactory(requestFactory).build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
                .createClient(LibraryBookClient.class);
    }
}
