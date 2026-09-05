package com.tuitionnetwork.tuitionnetwork.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI tuitionNetworkOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Tuition & Services Fees Collection Network API")
                .version("v1")
                .description("Backend APIs for tuition fee lookup, payments, "
                        + "receipts, institution operations and reconciliation."));
    }
}
