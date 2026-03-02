package com.example.grocery.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FilterConfig {

    private final AuthTokenFilter authTokenFilter;

    @Bean
    public FilterRegistrationBean<AuthTokenFilter> authTokenFilterRegistration() {
        FilterRegistrationBean<AuthTokenFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(authTokenFilter);
        reg.addUrlPatterns("/*");
        reg.setOrder(1);
        return reg;
    }
}
