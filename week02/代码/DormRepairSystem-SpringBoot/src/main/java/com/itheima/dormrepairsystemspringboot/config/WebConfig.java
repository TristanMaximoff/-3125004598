package com.itheima.dormrepairsystemspringboot.config;

import com.itheima.dormrepairsystemspringboot.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired  //自动注入登录拦截器
    private LoginInterceptor loginInterceptor;

    @Value("${upload.path}")
    private String uploadPath;

    //配置登录拦截器
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截所有请求，放行逻辑在拦截器内部处理
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**");  //拦截所有后端接口，在拦截器内部方形
    }

    //配置静态资源访问（让图片能被看到）
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = uploadPath;
        if (!path.startsWith("file:")) {
            path = "file:" + path;
        }
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(path);
    }
}