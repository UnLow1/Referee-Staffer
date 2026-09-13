package com.jamex.refereestaffer.model.dto;

import com.jamex.refereestaffer.model.validation.OnUpdate;
import jakarta.validation.constraints.NotNull;

public record GradeDto(

        @NotNull(groups = OnUpdate.class)
        Long id,

        @NotNull
        Double value,

        // Second component of a split grade (e.g. 7.9/8.3); null for a plain grade.
        Double secondValue
) {

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
