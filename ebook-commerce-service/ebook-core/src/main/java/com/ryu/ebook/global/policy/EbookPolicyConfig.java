package com.ryu.ebook.global.policy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EbookPolicy.class)
public class EbookPolicyConfig {
}
