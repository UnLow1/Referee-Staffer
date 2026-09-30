package com.jamex.refereestaffer.model.exception;

/**
 * Thrown when an entity cannot be deleted because other rows still reference it.
 *
 * <p>Maps to 409 Conflict: the request is well-formed and the target exists, but the
 * database would reject the delete on a foreign key. Without this check the constraint
 * violation surfaces as an opaque 500 with no hint of what is blocking the delete.
 */
public class EntityInUseException extends RuntimeException {

    public static final String REFEREE_HAS_MATCHES =
            "Referee with id = %d cannot be deleted: %d match(es) are assigned to them";
    public static final String TEAM_HAS_MATCHES =
            "Team with id = %d cannot be deleted: it takes part in %d match(es)";

    private EntityInUseException(String message) {
        super(message);
    }

    public static EntityInUseException refereeHasMatches(Long refereeId, long matches) {
        return new EntityInUseException(String.format(REFEREE_HAS_MATCHES, refereeId, matches));
    }

    public static EntityInUseException teamHasMatches(Long teamId, long matches) {
        return new EntityInUseException(String.format(TEAM_HAS_MATCHES, teamId, matches));
    }
}
