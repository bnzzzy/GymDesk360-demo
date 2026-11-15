package com.benz.gymdesk360.service;

import com.benz.gymdesk360.dto.response.GymDashboardResponse;
import com.benz.gymdesk360.dto.response.MemberResponse;
import com.benz.gymdesk360.model.Member;
import com.benz.gymdesk360.model.Membership;
import com.benz.gymdesk360.model.enums.MembershipStatus;
import com.benz.gymdesk360.repository.GymRepository;
import com.benz.gymdesk360.repository.MemberRepository;
import com.benz.gymdesk360.repository.PlanRepository;
import com.benz.gymdesk360.security.AuthUser;
import com.benz.gymdesk360.util.GymScope;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GymDashboardService {

    private static final int EXPIRING_DAYS = 7;

    private final MemberRepository memberRepository;
    private final PlanRepository planRepository;
    private final GymRepository gymRepository;
    private final MemberService memberService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public GymDashboardResponse get(AuthUser actor) {
        Long gymId = GymScope.gymId(actor);
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);
        YearMonth month = YearMonth.from(today);

        Map<Long, List<Membership>> byMember = memberService.membershipsByMember(gymId);
        List<Member> members = memberRepository.findAllByGymIdOrderByLastNameAscFirstNameAsc(gymId);

        long active = 0;
        long expiredRecently = 0;
        long newThisMonth = 0;
        List<MemberResponse> expiring = new ArrayList<>();

        for (Member m : members) {
            if (YearMonth.from(m.getCreatedAt().atZone(zone)).equals(month)) newThisMonth++;
            if (!m.isActive()) continue;

            MemberResponse r = MemberService.summarize(m, byMember.getOrDefault(m.getId(), List.of()), today);
            if (r.status() == MembershipStatus.ACTIVE) {
                active++;
                // Vence pronto y no tiene una renovación ya pagada
                if (r.daysLeft() != null && r.daysLeft() <= EXPIRING_DAYS && r.paidUntil().equals(r.currentEndDate())) {
                    expiring.add(r);
                }
            } else if (r.status() == MembershipStatus.EXPIRED && !r.currentEndDate().isBefore(today.minusDays(30))) {
                expiredRecently++;
            }
        }
        expiring.sort(Comparator.comparing(MemberResponse::currentEndDate));

        long sales = 0;
        long revenue = 0;
        for (List<Membership> list : byMember.values()) {
            for (Membership ms : list) {
                if (!ms.isCancelled() && YearMonth.from(ms.getCreatedAt().atZone(zone)).equals(month)) {
                    sales++;
                    revenue += ms.getPricePaid();
                }
            }
        }

        long activePlans = planRepository.findAllByGymIdOrderByActiveDescPriceAsc(gymId).stream().filter(p -> p.isActive()).count();
        String gymName = gymRepository.findById(gymId).map(g -> g.getName()).orElse("");

        return new GymDashboardResponse(gymName, active, members.stream().filter(Member::isActive).count(),
                expiredRecently, newThisMonth, sales, revenue, activePlans, expiring);
    }

}
