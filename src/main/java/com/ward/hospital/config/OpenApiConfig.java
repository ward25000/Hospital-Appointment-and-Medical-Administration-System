package com.ward.hospital.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI hospitalOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hospital Appointment and Medical Administration System API")
                        .version("1.0.0")
                        .description("""
                                REST API для управления пациентами, врачами, расписанием, записями на приём
                                и отчётностью. Авторизация выполняется через сессионную cookie:
                                1) GET /api/auth/csrf — получить CSRF-токен;
                                2) POST /api/auth/login — войти;
                                3) передавать X-XSRF-TOKEN для POST/PUT/DELETE запросов.
                                Демо-пользователи: admin / Admin123!, reception / Reception123!,
                                doctor.ivanov / Doctor123!.
                                """)
                        .contact(new Contact().name("Hospital Administration System")));
    }
}
