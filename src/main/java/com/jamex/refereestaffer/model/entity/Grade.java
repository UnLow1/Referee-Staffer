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

    @JsonIgnore
    @OneToOne(targetEntity = Match.class)
    private Match match;

    public Grade() {
    }

    public Grade(Long id, Double value, Match match) {
        this.id = id;
        this.value = value;
        this.match = match;
    }

    public Grade(Match match, double value) {
        this.match = match;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public Double getValue() {
        return value;
    }

    public Match getMatch() {
        return match;
    }

    public void setMatch(Match match) {
        this.match = match;
    }

    @Override
    public String toString() {
        return "Grade(id=" + id + ", value=" + value + ")";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private Double value;
        private Match match;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder value(Double value) {
            this.value = value;
            return this;
        }

        public Builder match(Match match) {
            this.match = match;
            return this;
        }

        public Grade build() {
            return new Grade(id, value, match);
        }
    }
}
