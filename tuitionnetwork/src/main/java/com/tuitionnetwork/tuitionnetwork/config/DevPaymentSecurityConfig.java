package com.tuitionnetwork.tuitionnetwork.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile("dev")
public class DevPaymentSecurityConfig {
    @Bean
    @Order(1)
    SecurityFilterChain devPaymentEndpoints(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/v1/receipts/**")
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(
                        org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
                .build();
    }
}
