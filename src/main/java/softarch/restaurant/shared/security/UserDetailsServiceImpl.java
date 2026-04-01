package softarch.restaurant.shared.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.*;
import java.util.List;

/**
 * Loads UserDetails from the users + user_roles + roles tables for Spring Security.
 * Uses a raw JPQL query via EntityManager to avoid a dependency on a dedicated
 * User domain that hasn't been modelled in the system diagrams.
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final EntityManager em;

    public UserDetailsServiceImpl(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // Fetch the user record
        List<?> rows = em.createNativeQuery(
                "SELECT u.id, u.password_hash, r.name AS role " +
                "FROM users u " +
                "JOIN user_roles ur ON ur.user_id = u.id " +
                "JOIN roles r       ON r.id = ur.role_id " +
                "WHERE u.username = :username AND u.is_active = true")
            .setParameter("username", username)
            .getResultList();

        if (rows.isEmpty()) {
            throw new UsernameNotFoundException("User not found: " + username);
        }

        // First row gives us id + password hash
        Object[] first = (Object[]) rows.get(0);
        String passwordHash = (String) first[1];

        // Collect all roles as granted authorities (e.g. ROLE_MANAGER)
        List<SimpleGrantedAuthority> authorities = rows.stream()
            .map(r -> new SimpleGrantedAuthority("ROLE_" + ((Object[]) r)[2]))
            .toList();

        return User.builder()
            .username(username)
            .password(passwordHash)
            .authorities(authorities)
            .build();
    }
}
