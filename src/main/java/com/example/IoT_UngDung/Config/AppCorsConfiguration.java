package com.example.IoT_UngDung.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class AppCorsConfiguration {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        // Cho phép các origin cụ thể (bạn có thể thêm nếu cần)
        config.setAllowedOrigins(Arrays.asList(
                "http://127.0.0.1:5500",
                "http://localhost:5500"
        ));

        // Cho phép tất cả headers và methods
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        // Cho phép gửi cookie / credentials (nếu cần sau này)
        config.setAllowCredentials(true);

        // Cho phép đọc các header phản hồi (nếu backend có gửi token)
        config.addExposedHeader("Authorization");

        // Áp dụng cho toàn bộ API
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
