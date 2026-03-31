package org.venus.lin.xin.mgr.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LinXin Manager API")
                        .description("LinXin Manager Backend API Documentation")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Venus Team")
                                .email("support@venus.org")));
    }

}
