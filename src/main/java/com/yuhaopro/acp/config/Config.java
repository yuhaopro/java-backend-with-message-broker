package com.yuhaopro.acp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.yuhaopro.acp.data.RuntimeEnvironment;

@Configuration
public class Config {
    @Bean
    public RuntimeEnvironment CurrentRuntimeEnvironment() {
        return RuntimeEnvironment.getEnvironment();
    }
}
