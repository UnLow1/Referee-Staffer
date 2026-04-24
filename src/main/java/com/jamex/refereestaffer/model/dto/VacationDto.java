package com.jamex.refereestaffer.model.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class VacationDto {

    @NotNull
    private final Long id;

    @NotNull
    private final Long refereeId;

    @NotNull
    private final LocalDate startDate;

    @NotNull
    private final LocalDate endDate;

    public VacationDto(Long id, Long refereeId, LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.refereeId = refereeId;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public Long getRefereeId() {
        return refereeId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

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
