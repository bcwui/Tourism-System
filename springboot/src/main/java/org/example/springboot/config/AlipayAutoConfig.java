package org.example.springboot.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "alipay", name = "appId")
@EnableConfigurationProperties(AlipayConfig.class)
public class AlipayAutoConfig {
}
