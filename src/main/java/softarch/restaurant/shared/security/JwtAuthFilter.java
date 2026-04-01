package softarch.restaurant.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.annotation.Nonnull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;

/**
 * Intercepts every request, extracts the Bearer JWT from the Authorization header,
 * validates it, and populates the Spring Security context.
 *
 * Runs once per request (OncePerRequestFilter).
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService             jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtService jwtService,
                         UserDetailsServiceImpl userDetailsService) {
        this.jwtService        = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest  request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain         filterChain
    ) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Skip if no Bearer token present
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt      = authHeader.substring(7);
        final String username = jwtService.extractUsername(jwt);

        // Only authenticate if not already authenticated
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails.getUsername())) {
                UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(
                    new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // Propagate userId header so controllers can read it without re-parsing JWT
                Long userId = jwtService.extractUserId(jwt);
                request.setAttribute("X-User-Id", userId);
            }
        }

        filterChain.doFilter(request, response);
    }
}
