package com.benz.gymdesk360.service;

import com.benz.gymdesk360.dto.request.MemberRequest;
import com.benz.gymdesk360.dto.response.MemberDetailResponse;
import com.benz.gymdesk360.dto.response.MemberResponse;
import com.benz.gymdesk360.dto.response.MembershipResponse;
import com.benz.gymdesk360.exception.ApiException;
import com.benz.gymdesk360.model.Member;
import com.benz.gymdesk360.model.Membership;
import com.benz.gymdesk360.model.enums.MembershipStatus;
import com.benz.gymdesk360.repository.MemberRepository;
import com.benz.gymdesk360.repository.MembershipRepository;
import com.benz.gymdesk360.security.AuthUser;
import com.benz.gymdesk360.util.GymScope;
import com.benz.gymdesk360.util.RutUtils;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.benz.gymdesk360.service.PlanService.blankToNull;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final MembershipRepository membershipRepository;
    private final Clock clock;

    /**
     * Socios del gimnasio con el estado de su membresía.
     *
     * @param q      busca en nombre, RUT, email y teléfono (sin importar tildes ni mayúsculas)
     * @param status filtra por estado de membresía (ACTIVE, UPCOMING, EXPIRED, NONE)
     */
    @Transactional(readOnly = true)
    public List<MemberResponse> list(AuthUser actor, String q, MembershipStatus status) {
        Long gymId = GymScope.gymId(actor);
        LocalDate today = LocalDate.now(clock);
        Map<Long, List<Membership>> byMember = membershipsByMember(gymId);
        String needle = q == null || q.isBlank() ? null : fold(q);

        return memberRepository.findAllByGymIdOrderByLastNameAscFirstNameAsc(gymId).stream()
                .filter(m -> needle == null || matches(m, needle))
                .map(m -> summarize(m, byMember.getOrDefault(m.getId(), List.of()), today))
                .filter(r -> status == null || r.status() == status)
                .toList();
    }

    @Transactional(readOnly = true)
    public MemberDetailResponse detail(AuthUser actor, Long id) {
        Member member = get(actor, id);
        LocalDate today = LocalDate.now(clock);
        List<Membership> memberships = membershipRepository.findAllByMember_IdOrderByStartDateDesc(id);
        return new MemberDetailResponse(
                summarize(member, memberships, today),
                memberships.stream().map(ms -> MembershipResponse.from(ms, today)).toList());
    }

    @Transactional
    public MemberResponse create(AuthUser actor, MemberRequest req) {
        Long gymId = GymScope.gymId(actor);
        String rut = normalizeRut(req.rut());
        if (rut != null && memberRepository.existsByGymIdAndRut(gymId, rut)) {
            throw ApiException.conflict("Ya existe un socio con ese RUT");
        }
        Member m = new Member();
        m.setGymId(gymId);
        apply(m, req, rut);
        return summarize(memberRepository.save(m), List.of(), LocalDate.now(clock));
    }

    @Transactional
    public MemberResponse update(AuthUser actor, Long id, MemberRequest req) {
        Member m = get(actor, id);
        String rut = normalizeRut(req.rut());
        if (rut != null && memberRepository.existsByGymIdAndRutAndIdNot(m.getGymId(), rut, id)) {
            throw ApiException.conflict("Ya existe un socio con ese RUT");
        }
        apply(m, req, rut);
        return toResponse(m);
    }

    @Transactional
    public MemberResponse setActive(AuthUser actor, Long id, boolean active) {
        Member m = get(actor, id);
        m.setActive(active);
        return toResponse(m);
    }

    /** Solo se elimina un socio sin historial; si tiene membresías, se desactiva. */
    @Transactional
    public void delete(AuthUser actor, Long id) {
        Member m = get(actor, id);
        if (membershipRepository.existsByMember_Id(id)) {
            throw ApiException.conflict("El socio tiene membresías registradas. Desactívalo en lugar de eliminarlo.");
        }
        memberRepository.delete(m);
    }

    /** Socio del gimnasio del usuario (404 si es de otro gimnasio). */
    public Member get(AuthUser actor, Long id) {
        return memberRepository.findByIdAndGymId(id, GymScope.gymId(actor))
                .orElseThrow(() -> ApiException.notFound("Socio no encontrado"));
    }

    public MemberResponse toResponse(Member m) {
        LocalDate today = LocalDate.now(clock);
        return summarize(m, membershipRepository.findAllByMember_IdOrderByStartDateDesc(m.getId()), today);
    }

    public Map<Long, List<Membership>> membershipsByMember(Long gymId) {
        return membershipRepository.findAllByGymId(gymId).stream()
                .collect(Collectors.groupingBy(ms -> ms.getMember().getId()));
    }

    /** Socio + resumen de su membresía, a partir de todas sus membresías. */
    public static MemberResponse summarize(Member m, List<Membership> memberships, LocalDate today) {
        return MemberResponse.from(m, current(memberships, today), paidUntil(memberships, today), today);
    }

    /** Último día cubierto por membresías no anuladas que aún no terminan (vigentes o futuras). */
    public static LocalDate paidUntil(List<Membership> memberships, LocalDate today) {
        return memberships.stream()
                .filter(ms -> !ms.isCancelled() && !ms.getEndDate().isBefore(today))
                .map(Membership::getEndDate)
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    /**
     * Membresía más relevante de un socio (sin contar anuladas):
     * la vigente que termina más tarde; si no hay, la próxima en comenzar; si no, la última que terminó.
     */
    public static Membership current(List<Membership> memberships, LocalDate today) {
        List<Membership> valid = memberships.stream().filter(ms -> !ms.isCancelled()).toList();
        return valid.stream()
                .filter(ms -> ms.statusOn(today) == MembershipStatus.ACTIVE)
                .max(Comparator.comparing(Membership::getEndDate))
                .or(() -> valid.stream()
                        .filter(ms -> ms.statusOn(today) == MembershipStatus.UPCOMING)
                        .min(Comparator.comparing(Membership::getStartDate)))
                .or(() -> valid.stream().max(Comparator.comparing(Membership::getEndDate)))
                .orElse(null);
    }

    private static void apply(Member m, MemberRequest req, String rut) {
        m.setRut(rut);
        m.setFirstName(req.firstName().trim());
        m.setLastName(req.lastName().trim());
        m.setEmail(req.email() == null || req.email().isBlank() ? null : req.email().trim().toLowerCase(Locale.ROOT));
        m.setPhone(blankToNull(req.phone()));
        m.setBirthDate(req.birthDate());
        m.setEmergencyContactName(blankToNull(req.emergencyContactName()));
        m.setEmergencyContactPhone(blankToNull(req.emergencyContactPhone()));
        m.setNotes(blankToNull(req.notes()));
    }

    private static String normalizeRut(String raw) {
        try {
            return RutUtils.normalize(raw);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static boolean matches(Member m, String needle) {
        String haystack = fold(String.join(" ", m.getFirstName(), m.getLastName(),
                nz(m.getRut()), nz(m.getRut()).replace("-", ""), nz(m.getEmail()), nz(m.getPhone())));
        return haystack.contains(needle);
    }

    /** minúsculas y sin tildes: "Muñoz" -> "munoz" */
    private static String fold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
