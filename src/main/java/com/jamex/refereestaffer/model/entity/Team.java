package com.jamex.refereestaffer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * A league team. Purely persistent since RS-99 — season numbers (points, table place,
 * W/D/L, goals) are not fields here but rows of the computed league table
 * ({@code TeamService.getStandings()}), so there is nothing to keep in sync and no
 * transient state whose value depends on which persistence context loaded the entity.
 */
@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String city;

    /**
     * 3-letter team code shown in TeamPill (e.g. "LEG" for Legia Warszawa). Nullable so
     * existing data keeps working — when null, {@link #getShortCode()} derives a fallback
     * from `name`. The field exists so league imports can override the fallback for teams
     * where prefix-matching would collide (Lech vs Lechia).
     */
    @Column(name = "short_code", length = 8)
    private String shortCode;

    public Team() {
    }

    public Team(Long id, String name, String city, String shortCode) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.shortCode = shortCode;
    }

    public Team(String name) {
        this.name = name;
    }

    public Team(String name, String city) {
        this.name = name;
        this.city = city;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    /**
     * Returns the stored {@code short_code} when set, otherwise a fallback derived from
     * the first three characters of {@link #name}. Callers (DTOs, JSON serialisation)
     * always see a non-null short code, so the frontend can render it without its own
     * fallback. The stored override exists for cases where prefix-matching collides.
     */
    public String getShortCode() {
        if (shortCode != null && !shortCode.isBlank()) {
            return shortCode.toUpperCase();
        }
        if (name == null || name.isBlank()) {
            return "";
        }
        return name.substring(0, Math.min(3, name.length())).toUpperCase();
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    @Override
    public String toString() {
        return "Team(name=" + name + ", city=" + city + ")";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String city;
        private String shortCode;

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

        public Team build() {
            return new Team(id, name, city, shortCode);
        }
    }
}
