package softarch.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

/**
 * JPA / Hibernate configuration.
 *
 * Responsibilities:
 *  1. Enable JPA repository scanning across all domain packages
 *  2. Enable @Transactional management
 *  3. Enable JPA Auditing so @CreatedBy / @LastModifiedBy fields
 *     are populated automatically from the JWT principal
 *
 * Hibernate settings (in application.properties):
 *  - ddl-auto=validate        : schema managed by Flyway only, Hibernate validates
 *  - dialect=PostgreSQLDialect: full PostgreSQL feature support (JSONB, etc.)
 *  - format_sql=true          : readable SQL in logs
 *
 * JSONB support:
 *  - io.hypersistence:hypersistence-utils-hibernate-63 registered via
 *    @Type(JsonBinaryType.class) on individual entity fields — no global
 *    registration needed here.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "softarch.restaurant.domain")
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")
public class JpaConfig {

    /**
     * AuditorAware implementation for @CreatedBy / @LastModifiedBy.
     *
     * Reads the currently authenticated username from Spring Security's
     * SecurityContext — populated by JwtAuthFilter on every request.
     *
     * Returns "system" for unauthenticated contexts (e.g. scheduled tasks,
     * Flyway migrations, or tests without a security context).
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()
                    || "anonymousUser".equals(auth.getPrincipal())) {
                return Optional.of("system");
            }
            return Optional.of(auth.getName());
        };
    }
}
