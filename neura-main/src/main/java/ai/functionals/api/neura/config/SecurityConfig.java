package ai.functionals.api.neura.config;

import ai.functionals.api.neura.filter.SecurityFilter;
import ai.functionals.api.neura.model.enums.UserRoles;
import lombok.Builder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Set;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Value("${app.auth0.audiences}" )
    private Set<String> audiences;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}" )
    private String issuer;

    @Autowired
    private SecurityFilter securityFilter;

    @Bean
    @Order(1)
    public SecurityFilterChain publicChainWithoutAuth(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/pub/**", "/health/**", "/swagger-ui/**", "/swagger-resources/*", "/v3/api-docs/**", "/api/designer/*/events" )
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .cors(c -> c.configurationSource(corsConfigSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http.securityMatcher("/adm/**", "/api/**" )

                .authorizeHttpRequests(requests -> requests

                        .requestMatchers(HttpMethod.OPTIONS, "/**" ).permitAll()

                        .requestMatchers("/adm/**" )
                        .hasAnyAuthority(UserRoles.ROLE_FUN_ADMIN.name(),
                                UserRoles.ROLE_FUN_ROOT.name())


                        .requestMatchers("/api/**" )
                        .hasAnyAuthority(UserRoles.ROLE_FUN_ADMIN.name(),
                                UserRoles.ROLE_FUN_ROOT.name(),
                                UserRoles.ROLE_FUN_USER.name(),
                                UserRoles.ROLE_FUN_SUBSCRIBED.name())


                        .anyRequest().authenticated()
                ).sessionManagement((smc) -> smc.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .oauth2ResourceServer(httpSecurityOAuth2ResourceServerConfigurer ->
                        httpSecurityOAuth2ResourceServerConfigurer.jwt(Customizer.withDefaults()))
                .cors(c -> c.configurationSource(corsConfigSource()))
                .csrf(CsrfConfigurer::disable)
                .addFilterBefore(securityFilter, BasicAuthenticationFilter.class).build();
    }

    @Bean
    public CorsConfigurationSource corsConfigSource() {
        var cors = new CorsConfiguration();
        cors.setAllowedOriginPatterns(List.of("https://*.functionals.ai", "https://functionals.ai", "https://*.cloudfront.net", "http://localhost:*" ));
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS" ));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With" ));
        cors.setExposedHeaders(List.of("Authorization", "Location" ));
        cors.setAllowCredentials(true);
        cors.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }

    @Bean
    JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = JwtDecoders.fromOidcIssuerLocation(issuer);
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefaultWithIssuer(issuer),
                AudienceValidator.builder().audiences(audiences).build()));
        return jwtDecoder;
    }

    @Data
    @Builder
    static class AudienceValidator implements OAuth2TokenValidator<Jwt> {
        private Set<String> audiences;

        public OAuth2TokenValidatorResult validate(Jwt jwt) {
            for (String aud : jwt.getAudience()) {
                if (audiences.contains(aud)) {
                    return OAuth2TokenValidatorResult.success();
                }
            }
            return OAuth2TokenValidatorResult.failure(new OAuth2Error("audience_mismatch", "expected audience: " + audiences, null));
        }
    }
}
