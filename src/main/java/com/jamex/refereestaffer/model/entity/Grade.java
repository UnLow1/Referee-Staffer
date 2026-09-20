package com.jamex.refereestaffer.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

@Entity
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Double value;

    // Second component of a "split" grade (e.g. 7.9/8.3). Null for a plain single grade.
    @Column
    private Double secondValue;

    @JsonIgnore
    @OneToOne(targetEntity = Match.class)
    private Match match;

    public Grade() {
    }

    public Grade(Long id, Double value, Double secondValue, Match match) {
        this.id = id;
        this.value = value;
        this.secondValue = secondValue;
        this.match = match;
    }

    public Grade(Match match, double value) {
        this.match = match;
        this.value = value;
    }

    public Grade(Match match, double value, Double secondValue) {
        this.match = match;
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

    /**
     * The grade that counts towards referee statistics: the arithmetic mean of both
     * components for a split grade (7.9/8.3 -> 8.1), or the single value otherwise.
     */
    public Double getEffectiveValue() {
        if (value == null) {
            return null;
        }
        return secondValue != null ? (value + secondValue) / 2 : value;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    @Override
    public String toString() {
        return "Grade(id=" + id + ", value=" + value + ", secondValue=" + secondValue + ")";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Double value;
        private Double secondValue;
        private Match match;

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

        public Builder match(Match match) {
            this.match = match;
            return this;
        }

        public Grade build() {
            return new Grade(id, value, secondValue, match);
        }
    }
}
