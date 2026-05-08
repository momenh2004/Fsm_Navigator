package com.fsm.navigator.backend.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/admin/stats/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/blocs/**").permitAll()
                .requestMatchers("/api/salles/**").permitAll()
                .requestMatchers("/api/fingerprints/**").permitAll()
                .requestMatchers("/api/poi/**").permitAll()
                .requestMatchers("/api/admin/**").permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable());
        return http.build();
    }
}