package softarch.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .anyRequest().permitAll() // Cho phép tất cả request
            )
            .csrf(csrf -> csrf.disable()) // Tắt CSRF
            .formLogin(login -> login.disable()) // Tắt login form mặc định
            .httpBasic(basic -> basic.disable()); // Tắt HTTP Basic

        return http.build();
    }
}