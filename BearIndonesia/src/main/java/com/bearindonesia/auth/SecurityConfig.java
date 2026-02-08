package com.bearindonesia.auth;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import jakarta.servlet.DispatcherType;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    /**
     * 1) 공개 엔드포인트 전용 체인 (JWT 필터를 아예 적용하지 않음)
     *    - 여기서 permitAll로 끝내면 JwtAuthFilter가 끼어들 여지가 없음
     */
    @Bean
    @Order(1)
    public SecurityFilterChain publicApiChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(
                "/auth/**",
                "/ingest/**",
                "/api/news/**",
                "/api/articles/**",
                "/api/newsletter/**",
                "/api/search",
                "/api/wordcloud"
            )
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/ingest/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/news/**", "/api/articles/**", "/api/newsletter/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/search").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/wordcloud").permitAll()
                .anyRequest().permitAll()
            );

        return http.build();
    }

    /**
     * 2) 나머지 전부 보호 체인 (여기서만 JWT 필터 적용)
     */
    @Bean
    @Order(2)
    public SecurityFilterChain securedChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
                .requestMatchers("/error").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // 운영 서버 주소도 추가하는 게 좋아 (브라우저에서만 문제 생김)
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "http://10.100.2.100"
            // 필요하면: "http://<회사고정IP>"
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
