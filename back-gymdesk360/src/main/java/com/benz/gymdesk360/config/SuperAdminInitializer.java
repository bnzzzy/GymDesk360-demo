package com.benz.gymdesk360.config;

import com.benz.gymdesk360.model.enums.Role;
import com.benz.gymdesk360.repository.UserRepository;
import com.benz.gymdesk360.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el SUPER_ADMIN al arrancar si se definieron SUPER_ADMIN_EMAIL y SUPER_ADMIN_PASSWORD
 * y ese email todavía no existe. Si ya existe no toca nada (no cambia la contraseña).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${app.bootstrap.super-admin.email:}")
    private String email;

    @Value("${app.bootstrap.super-admin.password:}")
    private String password;

    @Value("${app.bootstrap.super-admin.full-name:Super Admin}")
    private String fullName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(UserService.normalizeEmail(email))) {
            return;
        }
        if (password.length() < 12) {
            log.warn("SUPER_ADMIN_PASSWORD debe tener al menos 12 caracteres; no se creó el super admin");
            return;
        }
        userService.createUser(null, email, password, fullName, Role.SUPER_ADMIN);
        log.info("Super admin creado: {}", email);
    }
}
