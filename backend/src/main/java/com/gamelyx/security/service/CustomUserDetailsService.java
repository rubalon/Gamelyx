package com.gamelyx.security.service;

import com.gamelyx.repository.UserRepository;
import com.gamelyx.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Buscar usuario por username o email (login flexible como especificado en el reporte)
        User user = userRepository.findByEmailOrUsername(usernameOrEmail)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado con username o email: " + usernameOrEmail));

        // Verificar que el usuario esté verificado (si usamos verificación por email)
        if (!user.getEmailVerified()) {
            throw new UsernameNotFoundException("Cuenta no verificada. Verifica tu email.");
        }

        return new CustomUserPrincipal(user);
    }

    /**
     * Carga usuario por ID (útil para operaciones internas)
     */
    public UserDetails loadUserById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con ID: " + id));

        return new CustomUserPrincipal(user);
    }

    /**
     * Implementación personalizada de UserDetails
     * Wrappea nuestro User entity para que funcione con Spring Security
     */
    public static class CustomUserPrincipal implements UserDetails {
        private final User user;

        public CustomUserPrincipal(User user) {
            this.user = user;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            List<GrantedAuthority> authorities = new ArrayList<>();

            // Por ahora, todos los usuarios tienen rol USER
            // En futuras fases puedes añadir roles desde la base de datos
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

            // Si el usuario es administrador (puedes añadir campo en User entity)
            // if (user.isAdmin()) {
            //     authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            // }

            return authorities;
        }

        @Override
        public String getPassword() {
            return user.getPasswordHash();
        }

        @Override
        public String getUsername() {
            // Retornamos el username, no el email
            return user.getUsername();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true; // Por ahora las cuentas no expiran
        }

        @Override
        public boolean isAccountNonLocked() {
            return true; // Por ahora no bloqueamos cuentas
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true; // Por ahora las credenciales no expiran
        }

        @Override
        public boolean isEnabled() {
            // La cuenta está habilitada si el email está verificado
            return user.getEmailVerified();
        }

        // Método adicional para obtener el User entity completo
        public User getUser() {
            return user;
        }

        // Métodos de conveniencia para el frontend
        public UUID getId() {
            return user.getId();
        }

        public String getEmail() {
            return user.getEmail();
        }
    }
}