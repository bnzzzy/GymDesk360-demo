package com.benz.gymdesk360.dto.response;

import java.util.List;

/** Ficha del socio con su historial de membresías (más reciente primero). */
public record MemberDetailResponse(MemberResponse member, List<MembershipResponse> memberships) {
}
