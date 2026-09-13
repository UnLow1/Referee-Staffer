package com.jamex.refereestaffer.model.dto;

import com.jamex.refereestaffer.model.validation.OnUpdate;
import jakarta.validation.constraints.NotNull;

public record GradeDto(

        @NotNull(groups = OnUpdate.class)
        Long id,

        @NotNull
        Double value
) {

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
