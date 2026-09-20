package com.jamex.refereestaffer.model.request;

/**
 * A single pre-pinned (match, referee) pair sent along with a staffing request.
 * Locked pairs are applied verbatim: the referee is treated as busy and the match as
 * already cast, so the staffing algorithm only assigns the remaining matches.
 */
public record StaffingLockRequest(Long matchId, Long refereeId) {
}
