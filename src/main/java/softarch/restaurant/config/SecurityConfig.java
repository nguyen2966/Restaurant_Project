package softarch.restaurant.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import softarch.restaurant.shared.security.JwtAuthFilter;
import softarch.restaurant.shared.security.UserDetailsServiceImpl;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Security configuration.
 *
 * Strategy:
 *  - Stateless session (JWT only, no HttpSession)
 *  - CSRF disabled (REST API, no browser form submissions)
 *  - Role-based route protection per functional domain
 *  - Swagger UI + OpenAPI endpoints whitelisted as public
 *  - Custom 401 / 403 responses return ApiResponse JSON (không redirect)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity   // enables @PreAuthorize, @PostAuthorize on methods if needed
public class SecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final JwtAuthFilter          jwtAuthFilter;
    private final ObjectMapper           objectMapper;

    public SecurityConfig(UserDetailsServiceImpl userDetailsService,
                          JwtAuthFilter jwtAuthFilter,
                          ObjectMapper objectMapper) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthFilter      = jwtAuthFilter;
        this.objectMapper       = objectMapper;
    }

    // ─── Security Filter Chain ────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // ── Stateless REST — disable CSRF and sessions ────────────────────
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── Route authorisation ───────────────────────────────────────────
            .authorizeHttpRequests(auth -> auth

                // Public: auth endpoints
                .requestMatchers("/api/auth/**").permitAll()

                // Public: Swagger UI & OpenAPI spec (no auth required for docs)
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/v3/api-docs.yaml"
                ).permitAll()

                // Public: health/actuator (restrict in prod via management.endpoints config)
                .requestMatchers("/actuator/health").permitAll()

                // ── Menu ─────────────────────────────────────────────────────
                .requestMatchers(HttpMethod.GET,    "/api/menu/**").authenticated()
                .requestMatchers(HttpMethod.POST,   "/api/menu/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PUT,    "/api/menu/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.DELETE, "/api/menu/**").hasRole("MANAGER")
                .requestMatchers(HttpMethod.PATCH,  "/api/menu/**").hasRole("MANAGER")

                // ── Orders ───────────────────────────────────────────────────
                .requestMatchers("/api/orders/**").hasAnyRole("SERVER", "MANAGER")

                // ── Kitchen ──────────────────────────────────────────────────
                .requestMatchers("/api/kitchen/**").hasAnyRole("CHEF", "MANAGER")

                // ── Payments ─────────────────────────────────────────────────
                .requestMatchers("/api/payments/**").hasAnyRole("CASHIER", "MANAGER")

                // ── Promotions ───────────────────────────────────────────────
                .requestMatchers("/api/promotions/**").hasRole("MANAGER")

                // ── Inventory ────────────────────────────────────────────────
                .requestMatchers("/api/inventory/**").hasAnyRole("SERVER", "CHEF", "MANAGER")

                // ── Seating ──────────────────────────────────────────────────
                .requestMatchers("/api/seating/**").hasAnyRole("SERVER", "MANAGER")

                // ── Reports ──────────────────────────────────────────────────
                .requestMatchers("/api/reports/**").hasRole("MANAGER")

                // Catch-all: require authentication
                .anyRequest().authenticated()
            )

            // ── Custom 401 / 403 responses ────────────────────────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
            )

            // ── JWT filter ────────────────────────────────────────────────────
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ─── Custom 401 — unauthenticated (missing/expired JWT) ──────────────────

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                AuthenticationException authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success",   false);
            body.put("data",      null);
            body.put("message",   "Unauthenticated: " + authException.getMessage());
            body.put("timestamp", LocalDateTime.now().toString());
            objectMapper.writeValue(response.getWriter(), body);
        };
    }

    // ─── Custom 403 — authenticated but insufficient role ────────────────────

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request,
                HttpServletResponse response,
                org.springframework.security.access.AccessDeniedException accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success",   false);
            body.put("data",      null);
            body.put("message",   "Access denied: you do not have the required role for this action.");
            body.put("timestamp", LocalDateTime.now().toString());
            objectMapper.writeValue(response.getWriter(), body);
        };
    }

    // ─── Authentication beans ─────────────────────────────────────────────────

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config)
            throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}