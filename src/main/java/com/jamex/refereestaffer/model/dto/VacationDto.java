package com.jamex.refereestaffer.model.dto;

import com.jamex.refereestaffer.model.validation.OnUpdate;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record VacationDto(

        @NotNull(groups = OnUpdate.class)
        Long id,

        @NotNull
        Long refereeId,

        @NotNull
        LocalDate startDate,

        @NotNull
        LocalDate endDate
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Long refereeId;
        private LocalDate startDate;
        private LocalDate endDate;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder refereeId(Long refereeId) {
            this.refereeId = refereeId;
            return this;
        }

        public Builder startDate(LocalDate startDate) {
            this.startDate = startDate;
            return this;
        }

        public Builder endDate(LocalDate endDate) {
            this.endDate = endDate;
            return this;
        }

        public VacationDto build() {
            return new VacationDto(id, refereeId, startDate, endDate);
        }
    }
}
