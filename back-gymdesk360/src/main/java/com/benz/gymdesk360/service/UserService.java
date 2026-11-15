package com.benz.gymdesk360.service;

import com.benz.gymdesk360.demo.DemoGuard;
import com.benz.gymdesk360.dto.request.AdminUserRequest;
import com.benz.gymdesk360.dto.request.CreateUserRequest;
import com.benz.gymdesk360.dto.response.StatsResponse;
import com.benz.gymdesk360.dto.response.UserResponse;
import com.benz.gymdesk360.exception.ApiException;
import com.benz.gymdesk360.model.Gym;
import com.benz.gymdesk360.model.User;
import com.benz.gymdesk360.model.enums.Role;
import com.benz.gymdesk360.repository.GymRepository;
import com.benz.gymdesk360.repository.MemberRepository;
import com.benz.gymdesk360.repository.RefreshTokenRepository;
import com.benz.gymdesk360.repository.UserRepository;
import com.benz.gymdesk360.security.AuthUser;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final GymRepository gymRepository;
    private final MemberRepository memberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final DemoGuard demoGuard;

    // ---------------------------------------------------------------- usuarios del propio gimnasio

    /**
     * Alta de un usuario dentro del gimnasio de quien lo crea.
     * ADMIN puede crear ADMIN, STAFF y MEMBER; STAFF solo MEMBER.
     */
    @Transactional
    public UserResponse createInOwnGym(AuthUser actor, CreateUserRequest req) {
        Long gymId = requireGym(actor);
        boolean allowed = switch (actor.role()) {
            case ADMIN -> req.role() != Role.SUPER_ADMIN;
            case STAFF -> req.role() == Role.MEMBER;
            default -> false;
        };
        if (!allowed) {
            throw ApiException.forbidden("No puedes crear usuarios con rol " + req.role());
        }
        Gym gym = gymRepository.getReferenceById(gymId);
        return UserResponse.from(createUser(gym, req.email(), req.password(), req.fullName(), req.role()));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> listOwnGym(AuthUser actor) {
        return userRepository.findAllByGym_IdOrderByFullNameAsc(requireGym(actor)).stream()
                .map(UserResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse getById(Long id) {
        return UserResponse.from(get(id));
    }

    // ---------------------------------------------------------------- administración global (SUPER_ADMIN)

    @Transactional(readOnly = true)
    public List<UserResponse> adminList(Long gymId) {
        List<User> users = gymId == null
                ? userRepository.findAllByOrderByFullNameAsc()
                : userRepository.findAllByGym_IdOrderByFullNameAsc(gymId);
        return users.stream().map(UserResponse::from).toList();
    }

    @Transactional
    public UserResponse adminCreate(AdminUserRequest req) {
        if (req.password() == null || req.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria");
        }
        Gym gym = resolveGym(req.role(), req.gymId());
        return UserResponse.from(createUser(gym, req.email(), req.password(), req.fullName(), req.role()));
    }

    @Transactional
    public UserResponse adminUpdate(AuthUser actor, Long id, AdminUserRequest req) {
        User user = get(id);
        demoGuard.protectUser(user);
        if (Objects.equals(actor.id(), id) && req.role() != user.getRole()) {
            throw ApiException.forbidden("No puedes cambiar tu propio rol");
        }
        String email = normalizeEmail(req.email());
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflict("Ya existe un usuario con ese email");
        }
        Gym gym = resolveGym(req.role(), req.gymId());
        Long newGymId = gym == null ? null : gym.getId();
        boolean sessionChanged = user.getRole() != req.role() || !Objects.equals(user.getGymId(), newGymId);

        user.setEmail(email);
        user.setFullName(req.fullName().trim());
        user.setRole(req.role());
        user.setGym(gym);
        if (sessionChanged) {
            // El rol / gimnasio va dentro del JWT: forzar un login nuevo
            refreshTokenRepository.revokeAllForUser(id);
        }
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse adminSetEnabled(AuthUser actor, Long id, boolean enabled) {
        if (Objects.equals(actor.id(), id)) {
            throw ApiException.forbidden("No puedes desactivarte a ti mismo");
        }
        User user = get(id);
        demoGuard.protectUser(user);
        user.setEnabled(enabled);
        if (!enabled) {
            refreshTokenRepository.revokeAllForUser(id);
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void adminResetPassword(Long id, String newPassword) {
        User user = get(id);
        demoGuard.protectUser(user);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        refreshTokenRepository.revokeAllForUser(id);
    }

    @Transactional
    public void adminDelete(AuthUser actor, Long id) {
        if (Objects.equals(actor.id(), id)) {
            throw ApiException.forbidden("No puedes eliminar tu propia cuenta");
        }
        User user = get(id);
        demoGuard.protectUser(user);
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        Map<Role, Long> byRole = new EnumMap<>(Role.class);
        for (Role r : Role.values()) {
            byRole.put(r, 0L);
        }
        long total = 0;
        for (Object[] row : userRepository.countUsersPerRole()) {
            byRole.put((Role) row[0], (Long) row[1]);
            total += (Long) row[1];
        }
        return new StatsResponse(gymRepository.count(), gymRepository.countByActiveTrue(), total, memberRepository.count(), byRole);
    }

    // ---------------------------------------------------------------- comunes

    @Transactional
    public User createUser(Gym gym, String email, String rawPassword, String fullName, Role role) {
        String normalized = normalizeEmail(email);
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            throw ApiException.conflict("Ya existe un usuario con ese email");
        }
        User u = new User();
        u.setGym(gym);
        u.setEmail(normalized);
        u.setPasswordHash(passwordEncoder.encode(rawPassword));
        u.setFullName(fullName.trim());
        u.setRole(role);
        return userRepository.save(u);
    }

    public static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** SUPER_ADMIN no tiene gimnasio; el resto debe tener uno existente. */
    private Gym resolveGym(Role role, Long gymId) {
        if (role == Role.SUPER_ADMIN) {
            return null;
        }
        if (gymId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Debes indicar el gimnasio del usuario");
        }
        return gymRepository.findById(gymId).orElseThrow(() -> ApiException.notFound("Gimnasio no encontrado"));
    }

    private User get(Long id) {
        return userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Usuario no encontrado"));
    }

    private static Long requireGym(AuthUser actor) {
        if (actor.gymId() == null) {
            throw ApiException.forbidden("Este usuario no pertenece a un gimnasio");
        }
        return actor.gymId();
    }
}
