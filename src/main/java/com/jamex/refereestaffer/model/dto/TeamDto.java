package com.jamex.refereestaffer.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jamex.refereestaffer.model.validation.OnUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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
         * client echoes back never gets persisted as an override.
         */
        @JsonProperty("short")
        String shortCode,

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

        public Builder points(Short points) {
            this.points = points;
            return this;
        }

        public TeamDto build() {
            return new TeamDto(id, name, city, shortCode, points);
        }
    }
}
