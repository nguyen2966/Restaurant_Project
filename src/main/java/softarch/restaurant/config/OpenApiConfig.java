package softarch.restaurant.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger / SpringDoc OpenAPI configuration.
 *
 * Swagger UI: http://localhost:8080/swagger-ui/index.html
 * OpenAPI spec: http://localhost:8080/v3/api-docs
 *
 * Authentication flow in Swagger:
 *  1. Call POST /api/auth/login → copy the token from response
 *  2. Click "Authorize" button (🔒) in Swagger UI
 *  3. Enter: Bearer <your-token>
 *  4. All subsequent requests will include Authorization header automatically
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI restaurantOpenAPI() {
        // JWT Bearer security scheme
        final String securitySchemeName = "bearerAuth";

        return new OpenAPI()
            // ── API Info ──────────────────────────────────────────────────────
            .info(new Info()
                .title("Restaurant Management System API")
                .description("""
                    Hệ thống quản lý nhà hàng trung tâm.
                    
                    **Xác thực:**
                    1. Gọi `POST /api/auth/login` để lấy JWT token
                    2. Click nút **Authorize** 🔒 ở góc trên phải
                    3. Nhập `Bearer <token>` vào ô `bearerAuth`
                    
                    **Roles:**
                    - `MANAGER` — Toàn quyền
                    - `SERVER` — Orders, Menu (read), Seating, Inventory
                    - `CHEF` — Kitchen, Menu (read)
                    - `CASHIER` — Payments, Orders (read)
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("SoftArch Team")
                    .email("dev@softarch.restaurant"))
                .license(new License()
                    .name("Proprietary")
                    .url("https://softarch.restaurant"))
            )

            // ── Servers ───────────────────────────────────────────────────────
            .servers(List.of(
                new Server()
                    .url("http://localhost:" + serverPort)
                    .description("Local development server"),
                new Server()
                    .url("https://api.restaurant.example.com")
                    .description("Production server")
            ))

            // ── Security: JWT Bearer ──────────────────────────────────────────
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Nhập JWT token lấy từ POST /api/auth/login. Format: Bearer <token>")
                )
            );
    }
}
