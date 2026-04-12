package softarch.restaurant.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import softarch.restaurant.shared.dto.ApiResponse;
import softarch.restaurant.shared.exception.RestaurantException;
import softarch.restaurant.shared.security.JwtService;

import jakarta.persistence.*;
import java.util.List;

/**
 * Authentication controller — cung cấp endpoint đăng nhập để lấy JWT.
 *
 * Endpoint này là PUBLIC (không cần JWT) — được whitelist trong SecurityConfig.
 *
 * Flow:
 *  1. Client gửi username + password
 *  2. AuthenticationManager xác thực qua UserDetailsServiceImpl
 *  3. Nếu hợp lệ → sinh JWT với userId + role
 *  4. Client dùng JWT này cho các request tiếp theo (Header: Authorization: Bearer <token>)
 */
@RestController
@RequestMapping("/api/auth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Auth", description = "Xác thực — đăng nhập, lấy JWT token")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService            jwtService;
    private final EntityManager         entityManager;

    public AuthController(AuthenticationManager authenticationManager,
                          JwtService jwtService,
                          EntityManager entityManager) {
        this.authenticationManager = authenticationManager;
        this.jwtService            = jwtService;
        this.entityManager         = entityManager;
    }

    // ── DTOs ─────────────────────────────────────────────────────────────────

    public record LoginRequest(
        @NotBlank(message = "username là bắt buộc") String username,
        @NotBlank(message = "password là bắt buộc") String password
    ) {}

    public record LoginResponse(
        String token,
        String username,
        String role,
        long   expiresInSeconds
    ) {}

    // ── POST /api/auth/login ──────────────────────────────────────────────────

    /**
     * Đăng nhập và lấy JWT token.
     *
     * Ví dụ request:
     * <pre>
     * {
     *   "username": "admin",
     *   "password": "password123"
     * }
     * </pre>
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        try {
            // Xác thực credentials
            Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.username(), request.password())
            );

            // Lấy role đầu tiên (hệ thống này mỗi user có một role chính)
            String role = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.replace("ROLE_", ""))
                .findFirst()
                .orElse("UNKNOWN");

            // Lấy userId từ DB
            Long userId = resolveUserId(request.username());

            // Sinh JWT
            String token = jwtService.generateToken(request.username(), userId, role);

            return ResponseEntity.ok(ApiResponse.ok(
                new LoginResponse(token, request.username(), role, 86400L),
                "Đăng nhập thành công"
            ));

        } catch (BadCredentialsException e) {
            throw RestaurantException.badRequest("Tên đăng nhập hoặc mật khẩu không đúng.");
        }
    }

    // ── POST /api/auth/validate ───────────────────────────────────────────────

    /**
     * Kiểm tra JWT token còn hiệu lực không.
     * Dùng để frontend check token expiry mà không cần re-login.
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<Boolean>> validate(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.ok(ApiResponse.ok(false, "Token không hợp lệ"));
        }
        String token = authHeader.substring(7);
        boolean valid = !jwtService.isTokenExpired(token);
        return ResponseEntity.ok(ApiResponse.ok(valid,
            valid ? "Token còn hiệu lực" : "Token đã hết hạn"));
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Long resolveUserId(String username) {
        List<?> result = entityManager
            .createNativeQuery("SELECT id FROM users WHERE username = :u")
            .setParameter("u", username)
            .getResultList();
        if (result.isEmpty()) return -1L;
        Object id = result.get(0);
        return id instanceof Number n ? n.longValue() : -1L;
    }
}
