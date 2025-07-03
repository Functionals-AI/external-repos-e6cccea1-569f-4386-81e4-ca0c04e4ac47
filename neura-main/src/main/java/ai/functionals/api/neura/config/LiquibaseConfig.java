package ai.functionals.api.neura.config;

import liquibase.integration.spring.SpringLiquibase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

@Slf4j
@Configuration
public class LiquibaseConfig {
    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.liquibase.default-schema}")
    private String schema;

    @Bean
    public SpringLiquibase liquibase(DataSource dataSource) {
        createSchema();
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setDefaultSchema(schema);
        liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
        return liquibase;
    }

    private void createSchema() {
        log.info("Connecting to database to create schema: {}, url:{}, username:{}, password:{}", schema, jdbcUrl, username, password);
        String publicUrl = jdbcUrl.replaceAll("(?i)currentSchema=[^&]*", "currentSchema=public");
        try (Connection conn = DriverManager.getConnection(publicUrl, username, password);
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS " + schema);
        } catch (Exception e) {
            log.error("Failed to create schema with: [{} / {}] - {}", username, password, e.getMessage(), e);
        }
    }
}
