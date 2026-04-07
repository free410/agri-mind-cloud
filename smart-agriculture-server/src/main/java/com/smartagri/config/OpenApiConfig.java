package com.smartagri.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI smartAgriOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("智慧农业管理系统接口文档")
                        .description("用于比赛展示的后端接口文档，覆盖管理员登录、农田管理、作物管理、设备管理、环境数据、预警管理和 AI 对话模块。")
                        .version("v1.0.0")
                        .contact(new Contact().name("智慧农业项目组"))
                        .license(new License().name("仅用于本地开发与比赛演示")))
                .externalDocs(new ExternalDocumentation()
                        .description("项目本地接口访问地址")
                        .url("http://localhost:8080/api/swagger-ui.html"));
    }
}
