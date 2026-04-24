package com.jamex.refereestaffer.model.dto;

import jakarta.validation.constraints.NotNull;

public class GradeDto {

    @NotNull
    private final Long id;

    @NotNull
    private final Double value;

    public GradeDto(Long id, Double value) {
        this.id = id;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public Double getValue() {
        return value;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Double value;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder value(Double value) {
            this.value = value;
            return this;
        }

        public GradeDto build() {
            return new GradeDto(id, value);
        }
    }
}
