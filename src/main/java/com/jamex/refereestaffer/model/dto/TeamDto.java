package com.jamex.refereestaffer.model.dto;

import jakarta.validation.constraints.NotNull;

public class TeamDto {

    @NotNull
    private final Long id;

    @NotNull
    private final String name;

    @NotNull
    private final String city;

    private final Short points;

    public TeamDto(Long id, String name, String city, Short points) {
        this.id = id;
        this.name = name;
        this.city = city;
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

        public Builder points(Short points) {
            this.points = points;
            return this;
        }

        public TeamDto build() {
            return new TeamDto(id, name, city, points);
        }
    }
}
