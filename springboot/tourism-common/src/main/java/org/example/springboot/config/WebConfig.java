package org.example.springboot.config;

import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String API_PREFIX = "/api";

    private static final String[] PUBLIC_PATHS = {
        "/user/login",
        "/user/forget",
        "/user/add",
        "/email/**",
        "/img/**",
        "/file/**",
        "/alipay/return",
        "/alipay/notify",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/doc.html",
        "/webjars/**",
        "/favicon.ico"
    };

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", clazz ->
                clazz.isAnnotationPresent(RestController.class) &&
                        !clazz.getPackage().getName().contains("springfox") &&
                        !clazz.getPackage().getName().contains("swagger") &&
                        !clazz.getPackage().getName().contains("doc")
        );
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns(API_PREFIX + "/**")
                .excludePathPatterns(
                    "/api/user/login",
                    "/api/user/forget",
                    "/api/user/add",
                    "/api/email/**",
                    "/api/img/**",
                    "/api/file/**",
                    "/api/alipay/return",
                    "/api/alipay/notify",
                    "/api/v3/api-docs/**",
                    "/api/swagger-ui.html",
                    "/api/swagger-ui/**",
                    "/api/doc.html",
                    "/api/webjars/**",
                    "/api/favicon.ico"
                );
    }
}
