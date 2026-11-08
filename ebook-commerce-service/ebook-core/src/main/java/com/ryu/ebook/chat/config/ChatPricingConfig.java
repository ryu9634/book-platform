package com.ryu.ebook.chat.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChatPricing.class)
public class ChatPricingConfig {
}
