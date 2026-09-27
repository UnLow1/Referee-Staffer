package com.jamex.refereestaffer.model.dto;

import com.jamex.refereestaffer.model.staffing.StaffingViolation;

import java.util.List;

/**
 * Rules one {@code (match, referee)} pair would break, as served to the staffer's candidate
 * list. The endpoint returns the whole queue's matrix at once and skips clean pairs, so the
 * drawer opens with no request of its own and the payload stays proportional to the number of
 * actual conflicts rather than to matches × referees.
 */
public record CandidateViolationsDto(Long matchId, Long refereeId, List<StaffingViolation> violations) {
}
