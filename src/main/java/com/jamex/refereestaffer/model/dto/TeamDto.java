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

        /**
         * Home venue name, nullable — teams created by the CSV importer have none until
         * someone fills it in. Unlike {@code shortCode} this is client-editable: it round
         * trips through POST/PUT so the team drawer can maintain it.
         */
        String venueName,

        /** Street address of the venue, nullable and client-editable for the same reason. */
        String venueAddress,

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
        private String venueName;
        private String venueAddress;
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

        public Builder venueName(String venueName) {
            this.venueName = venueName;
            return this;
        }

        public Builder venueAddress(String venueAddress) {
            this.venueAddress = venueAddress;
            return this;
        }

        public Builder points(Short points) {
            this.points = points;
            return this;
        }

        public TeamDto build() {
            return new TeamDto(id, name, city, shortCode, venueName, venueAddress, points);
        }
    }
}
