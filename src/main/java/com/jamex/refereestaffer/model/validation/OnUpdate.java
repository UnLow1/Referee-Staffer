package com.jamex.refereestaffer.model.validation;

import jakarta.validation.groups.Default;

/**
 * Validation group for PUT (update) requests. Extends {@link Default} so validating with
 * this group also runs every un-grouped constraint — an update payload must satisfy all
 * create-time rules plus the ones marked with this group (in practice: a non-null id).
 *
 * <p>Needed because the frontend drawers build create payloads without an id (the server
 * generates it), so {@code @NotNull} on id must not apply to POST. Controllers pick the
 * group via {@code @Validated(OnUpdate.class)} on the {@code @RequestBody} parameter;
 * POST endpoints use plain {@code @Valid}.
 */
public interface OnUpdate extends Default {
}
