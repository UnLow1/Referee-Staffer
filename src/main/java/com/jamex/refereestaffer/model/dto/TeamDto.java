package com.jamex.refereestaffer.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jamex.refereestaffer.model.validation.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TeamDto(

        @NotNull(groups = OnUpdate.class)
        Long id,

        @NotBlank
        String name,

        @NotBlank
        String city,

        /**
         * 3-letter team code. Renamed in JSON to `short` (the frontend's wire format; the
         * Java component can't use the keyword). Backend always returns a non-null value —
         * Team.getShortCode() falls back to the first three letters of `name` when no
         * override is stored. Read-only: ignored on POST/PUT so the computed fallback the
         * client echoes back never gets persisted as an override — writes go through
         * {@code shortOverride} instead.
         */
        @JsonProperty("short")
        String shortCode,

        /**
         * The *stored* override, or null when the team has none — the writable half of the
         * pair. Kept separate from `short` on purpose: `short` is a computed read model, so
         * a client echoing a whole GET payload back on an unrelated edit would otherwise
         * persist the name-derived fallback and freeze the code across future renames.
         * Sending null (or blank) clears the override and restores the fallback.
         */
        @JsonProperty("shortOverride")
        @Size(max = 8, message = "short code must be at most 8 characters")
        // Unicode classes, not \p{Alnum}: that one is ASCII-only in Java, so a code carrying a
        // Polish diacritic would 400 even though the importer happily derives one.
        @Pattern(regexp = "^[\\p{L}\\p{N}]*$", message = "short code must be alphanumeric")
        String shortOverride,

        Short points
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String city;
        private String shortCode;
        private String shortOverride;
        private Short points;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder shortCode(String shortCode) {
            this.shortCode = shortCode;
            return this;
        }

        public Builder shortOverride(String shortOverride) {
            this.shortOverride = shortOverride;
            return this;
        }

        public Builder points(Short points) {
            this.points = points;
            return this;
        }

        public TeamDto build() {
            return new TeamDto(id, name, city, shortCode, shortOverride, points);
        }
    }
}
