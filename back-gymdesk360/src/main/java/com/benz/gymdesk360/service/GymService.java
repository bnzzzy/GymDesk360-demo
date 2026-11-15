package com.benz.gymdesk360.service;

import com.benz.gymdesk360.demo.DemoGuard;
import com.benz.gymdesk360.dto.request.CreateGymRequest;
import com.benz.gymdesk360.dto.request.UpdateGymRequest;
import com.benz.gymdesk360.dto.response.GymResponse;
import com.benz.gymdesk360.exception.ApiException;
import com.benz.gymdesk360.model.Gym;
import com.benz.gymdesk360.model.enums.Role;
import com.benz.gymdesk360.repository.GymRepository;
import com.benz.gymdesk360.repository.RefreshTokenRepository;
import com.benz.gymdesk360.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GymService {

    private final GymRepository gymRepository;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserService userService;
    private final DemoGuard demoGuard;

    /** Crea el gimnasio junto con su primer usuario ADMIN. */
    @Transactional
    public GymResponse create(CreateGymRequest req) {
        if (gymRepository.existsBySlug(req.slug())) {
            throw ApiException.conflict("Ya existe un gimnasio con ese slug");
        }
        Gym gym = new Gym();
        gym.setName(req.name().trim());
        gym.setSlug(req.slug());
        gym = gymRepository.save(gym);

        userService.createUser(gym, req.adminEmail(), req.adminPassword(), req.adminFullName(), Role.ADMIN);
        return GymResponse.from(gym, 1);
    }

    @Transactional(readOnly = true)
    public List<GymResponse> findAll() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] row : userRepository.countUsersPerGym()) {
            counts.put((Long) row[0], (Long) row[1]);
        }
        return gymRepository.findAllByOrderByNameAsc().stream()
                .map(g -> GymResponse.from(g, counts.getOrDefault(g.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public GymResponse findById(Long id) {
        Gym gym = get(id);
        return GymResponse.from(gym, userRepository.countByGym_Id(id));
    }

    @Transactional
    public GymResponse update(Long id, UpdateGymRequest req) {
        Gym gym = get(id);
        demoGuard.protectGym(gym);
        if (gymRepository.existsBySlugAndIdNot(req.slug(), id)) {
            throw ApiException.conflict("Ya existe un gimnasio con ese slug");
        }
        gym.setName(req.name().trim());
        gym.setSlug(req.slug());
        return GymResponse.from(gym, userRepository.countByGym_Id(id));
    }

    /** Al desactivar, los usuarios del gimnasio pierden sus sesiones y no pueden volver a entrar. */
    @Transactional
    public GymResponse setActive(Long id, boolean active) {
        Gym gym = get(id);
        demoGuard.protectGym(gym);
        gym.setActive(active);
        if (!active) {
            refreshTokenRepository.revokeAllForGym(id);
        }
        return GymResponse.from(gym, userRepository.countByGym_Id(id));
    }

    /** Elimina el gimnasio y todos sus usuarios (sus refresh tokens caen por ON DELETE CASCADE). */
    @Transactional
    public void delete(Long id) {
        Gym gym = get(id);
        demoGuard.protectGym(gym);
        userRepository.deleteAllByGymId(id);
        gymRepository.delete(gym);
    }

    private Gym get(Long id) {
        return gymRepository.findById(id).orElseThrow(() -> ApiException.notFound("Gimnasio no encontrado"));
    }
}
