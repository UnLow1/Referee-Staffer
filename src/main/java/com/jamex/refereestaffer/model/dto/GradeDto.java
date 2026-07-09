package com.jamex.refereestaffer.model.dto;

import jakarta.validation.constraints.NotNull;

public class GradeDto {

    @NotNull
    private final Long id;

    @NotNull
    private final Double value;

    // Second component of a split grade (e.g. 7.9/8.3); null for a plain grade.
    private final Double secondValue;

    public GradeDto(Long id, Double value, Double secondValue) {
        this.id = id;
        this.value = value;
        this.secondValue = secondValue;
    }

    public Long getId() {
        return id;
    }

    public Double getValue() {
        return value;
    }

    public Double getSecondValue() {
        return secondValue;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Double value;
        private Double secondValue;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder value(Double value) {
            this.value = value;
            return this;
        }

        public Builder secondValue(Double secondValue) {
            this.secondValue = secondValue;
            return this;
        }

        public GradeDto build() {
            return new GradeDto(id, value, secondValue);
        }
    }
}
