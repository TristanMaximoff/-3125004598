package com.itheima.dormrepairsystemspringboot.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override  //重写父类填上配置规则
    public void addCorsMappings(CorsRegistry registry) {  //sping框架自带类
        registry.addMapping("/**")  // 对所有接口生效
                .allowedOriginPatterns("http://localhost:5173")  //后端允许这个域名的访问
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")  //允许这些 HTTP方法
                .allowedHeaders("*")  //允许携带任何请求头
                .allowCredentials(true)  //允许携带凭证（如 token）
                .maxAge(3600);  //预检请求的有效期（秒），避免每次都发预检
    }
}