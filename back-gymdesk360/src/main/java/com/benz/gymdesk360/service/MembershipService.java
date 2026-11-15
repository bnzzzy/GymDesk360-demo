package com.benz.gymdesk360.service;

import com.benz.gymdesk360.dto.request.MembershipRequest;
import com.benz.gymdesk360.dto.response.MembershipResponse;
import com.benz.gymdesk360.exception.ApiException;
import com.benz.gymdesk360.model.Member;
import com.benz.gymdesk360.model.Membership;
import com.benz.gymdesk360.model.Plan;
import com.benz.gymdesk360.repository.MembershipRepository;
import com.benz.gymdesk360.security.AuthUser;
import com.benz.gymdesk360.util.GymScope;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import static com.benz.gymdesk360.service.PlanService.blankToNull;

@Service
@RequiredArgsConstructor
public class MembershipService {

    /** Margen para registrar ventas con fecha pasada (p. ej. se olvidaron de anotarla). */
    private static final int MAX_DAYS_BACK = 31;

    private final MembershipRepository membershipRepository;
    private final MemberService memberService;
    private final PlanService planService;
    private final Clock clock;

    /**
     * Vende un plan a un socio. Sin fecha de inicio: si el socio tiene una membresía vigente o futura,
     * la nueva empieza al día siguiente de que termine (renovación); si no, hoy.
     */
    @Transactional
    public MembershipResponse create(AuthUser actor, Long memberId, MembershipRequest req) {
        Member member = memberService.get(actor, memberId);
        if (!member.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El socio está desactivado. Reactívalo antes de venderle un plan.");
        }
        Plan plan = planService.get(actor, req.planId());
        if (!plan.isActive()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El plan está desactivado");
        }

        LocalDate today = LocalDate.now(clock);
        LocalDate start = req.startDate() != null ? req.startDate() : nextStart(memberId, today);
        if (start.isBefore(today.minusDays(MAX_DAYS_BACK))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La fecha de inicio no puede ser anterior a " + MAX_DAYS_BACK + " días");
        }

        Membership ms = new Membership();
        ms.setGymId(GymScope.gymId(actor));
        ms.setMember(member);
        ms.setPlanId(plan.getId());
        ms.setPlanName(plan.getName());
        ms.setStartDate(start);
        ms.setEndDate(plan.endDateFor(start));
        ms.setPricePaid(req.pricePaid() != null ? req.pricePaid() : plan.getPrice());
        ms.setNotes(blankToNull(req.notes()));
        ms.setCreatedBy(actor.id());
        return MembershipResponse.from(membershipRepository.save(ms), today);
    }

    /** Anula una venta (queda en el historial como anulada y deja de dar acceso). */
    @Transactional
    public MembershipResponse cancel(AuthUser actor, Long id) {
        Membership ms = membershipRepository.findByIdAndGymId(id, GymScope.gymId(actor))
                .orElseThrow(() -> ApiException.notFound("Membresía no encontrada"));
        if (ms.isCancelled()) {
            throw ApiException.conflict("La membresía ya estaba anulada");
        }
        ms.setCancelledAt(Instant.now(clock));
        return MembershipResponse.from(ms, LocalDate.now(clock));
    }

    private LocalDate nextStart(Long memberId, LocalDate today) {
        LocalDate paidUntil = MemberService.paidUntil(membershipRepository.findAllByMember_IdOrderByStartDateDesc(memberId), today);
        return paidUntil == null ? today : paidUntil.plusDays(1);
    }
}
