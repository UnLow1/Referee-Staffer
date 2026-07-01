package com.jamex.refereestaffer.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

public class TeamDto {

    @NotNull
    private final Long id;

    @NotNull
    private final String name;

    @NotNull
    private final String city;

    /**
     * 3-letter team code. Renamed in JSON to `short` (the frontend's wire format; the
     * Java field can't use the keyword). Backend always returns a non-null value —
     * Team.getShortCode() falls back to the first three letters of `name` when no
     * override is stored.
     */
    @JsonProperty("short")
    private final String shortCode;

    private final Short points;

    public TeamDto(Long id, String name, String city, String shortCode, Short points) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.shortCode = shortCode;
        this.points = points;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getShortCode() {
        return shortCode;
    }

    public Short getPoints() {
        return points;
    }

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
