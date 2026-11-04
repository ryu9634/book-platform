package com.ryu.library.global.policy;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LibraryPolicy.class)
public class LibraryPolicyConfig {
}
