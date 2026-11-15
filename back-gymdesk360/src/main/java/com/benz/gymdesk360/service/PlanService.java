package com.benz.gymdesk360.service;

import com.benz.gymdesk360.dto.request.PlanRequest;
import com.benz.gymdesk360.dto.response.PlanResponse;
import com.benz.gymdesk360.exception.ApiException;
import com.benz.gymdesk360.model.Plan;
import com.benz.gymdesk360.repository.MembershipRepository;
import com.benz.gymdesk360.repository.PlanRepository;
import com.benz.gymdesk360.security.AuthUser;
import com.benz.gymdesk360.util.GymScope;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final MembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public List<PlanResponse> list(AuthUser actor) {
        return planRepository.findAllByGymIdOrderByActiveDescPriceAsc(GymScope.gymId(actor)).stream()
                .map(PlanResponse::from).toList();
    }

    @Transactional
    public PlanResponse create(AuthUser actor, PlanRequest req) {
        Long gymId = GymScope.gymId(actor);
        String name = req.name().trim();
        if (planRepository.existsByGymIdAndNameIgnoreCase(gymId, name)) {
            throw ApiException.conflict("Ya existe un plan con ese nombre");
        }
        Plan plan = new Plan();
        plan.setGymId(gymId);
        apply(plan, req, name);
        return PlanResponse.from(planRepository.save(plan));
    }

    /** Editar un plan no cambia las membresías ya vendidas (guardan su propio nombre y precio). */
    @Transactional
    public PlanResponse update(AuthUser actor, Long id, PlanRequest req) {
        Plan plan = get(actor, id);
        String name = req.name().trim();
        if (planRepository.existsByGymIdAndNameIgnoreCaseAndIdNot(plan.getGymId(), name, id)) {
            throw ApiException.conflict("Ya existe un plan con ese nombre");
        }
        apply(plan, req, name);
        return PlanResponse.from(plan);
    }

    /** Un plan inactivo no se puede vender, pero sus membresías existentes siguen vigentes. */
    @Transactional
    public PlanResponse setActive(AuthUser actor, Long id, boolean active) {
        Plan plan = get(actor, id);
        plan.setActive(active);
        return PlanResponse.from(plan);
    }

    @Transactional
    public void delete(AuthUser actor, Long id) {
        Plan plan = get(actor, id);
        if (membershipRepository.existsByPlanId(id)) {
            throw ApiException.conflict("Este plan ya tiene membresías vendidas. Desactívalo en lugar de eliminarlo.");
        }
        planRepository.delete(plan);
    }

    /** Plan del gimnasio del usuario (404 si es de otro gimnasio). */
    public Plan get(AuthUser actor, Long id) {
        return planRepository.findByIdAndGymId(id, GymScope.gymId(actor))
                .orElseThrow(() -> ApiException.notFound("Plan no encontrado"));
    }

    private static void apply(Plan plan, PlanRequest req, String name) {
        plan.setName(name);
        plan.setDescription(blankToNull(req.description()));
        plan.setPrice(req.price());
        plan.setDurationValue(req.durationValue());
        plan.setDurationUnit(req.durationUnit());
    }

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
