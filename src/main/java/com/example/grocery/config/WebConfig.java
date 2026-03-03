package com.example.grocery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 1) เอารูปที่คุณ commit ไว้ใน src/main/resources/static/uploads/ (อยู่ใน JAR)
        // 2) เผื่อรูปที่อัปโหลดจริงใน runtime (อยู่ในโฟลเดอร์ uploads/)
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(
                        "classpath:/static/uploads/",
                        "file:uploads/"
                );
    }
}