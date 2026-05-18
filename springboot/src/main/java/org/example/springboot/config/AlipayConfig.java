package org.example.springboot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import lombok.Data;

@Data
@Configuration
@ConfigurationProperties(prefix = "alipay")
public class AlipayConfig {
    private String appId;
    private String privateKey;
    private String publicKey;
    private String gateway;
    private String returnUrl;
    private String notifyUrl;
    private String charset;
    private String format;
    private String signType;
} 