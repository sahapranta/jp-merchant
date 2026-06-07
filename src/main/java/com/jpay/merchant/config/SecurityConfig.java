package com.jpay.merchant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Permit H2 console, static assets, and all application endpoints without auth
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**", "/css/**", "/js/**", "/favicon.ico").permitAll()
                .anyRequest().permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            // Allow H2 console frames
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            // Disable CSRF for H2 console and API endpoints (re-enable in prod with token)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/h2-console/**", "/bills/draft", "/bills/*/draft",
                                         "/bills/*/publish", "/bills/*")
            );

        return http.build();
    }

    /**
     * TODO: Replace with DB-backed UserDetailsService using portal_user table.
     * For now: in-memory user for dev.
     * Password: admin123
     */
    @Bean
    public UserDetailsService userDetailsService(PasswordEncoder encoder) {
        var user = User.builder()
            .username("admin")
            .password(encoder.encode("admin123"))
            .roles("ADMIN")
            .build();
        return new InMemoryUserDetailsManager(user);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}