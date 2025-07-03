package ai.functionals.api.neura.config;

import ai.functionals.api.neura.model.commons.AppException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.IOException;
import java.util.List;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;

@Configuration
@EnableAsync
@EnableScheduling
public class AppConfig {
    @Value("${app.env.host}" )
    private String appEnvHost;

    /**
     * Swagger OpenAPI configuration
     *
     * @return OpenAPI
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // http://localhost:8080/swagger-ui/index.html
        // http://localhost:8080/v3/api-docs
        return new OpenAPI()
                .servers(List.of(
                        new Server().url(appEnvHost).description("Default" )
                ))
                .info(new Info()
                        .title("Jo4 API" )
                        .version("1.0" )
                        .description("API Information for Jo4." )
                )
                .externalDocs(new ExternalDocumentation()
                        .description("Get your JWT token" )
                        .url(appEnvHost + "/login" )
                ).components(new Components().addSecuritySchemes("token-auth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .in(SecurityScheme.In.HEADER)
                        .description("To get your own JWT token, click on 'Get your JWT token' above" )
                        .scheme("bearer" )
                        .bearerFormat("JWT" )
                ));
    }

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(FAIL_ON_EMPTY_BEANS, false);
        // Adds a type field to the JSON eg: "@class": "org.springframework.data.domain.PageImpl" | "@class": "org.springframework.data.domain.Sort
        /*objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);*/
        return objectMapper;
    }

    @Bean
    public ObjectMapper llmObjectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.build();
        objectMapper
                .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
                .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true)
                .configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true)
                .configure(JsonParser.Feature.ALLOW_NON_NUMERIC_NUMBERS, true)
                .configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true)
                .configure(JsonParser.Feature.ALLOW_MISSING_VALUES, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Bean
    public PDFTextStripper pdfStripper() {
        try {
            PDFTextStripper pdfStripper = new PDFTextStripper();
            return pdfStripper;
        } catch (IOException e) {
            throw new AppException("Error creating PDFTextStripper: " + e.getMessage(), e);
        }
    }

}
