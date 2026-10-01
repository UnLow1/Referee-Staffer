package com.jamex.refereestaffer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity
public class Team {

    /** Length of a name-derived short code; stored overrides may be shorter or longer. */
    public static final int SHORT_CODE_LENGTH = 3;

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

    @Transient
    private short points;

    /**
     * Standings position computed by {@code MatchService.calculatePointsForTeams} — only
     * teams that appear in a finished match get ranked. {@code null} means unranked (no
     * finished matches yet); never 0.
     */
    @Transient
    private Short place;

    public Team() {
    }

    public Team(Long id, String name, String city, short points, Short place) {
        this(id, name, city, null, points, place);
    }

    public Team(Long id, String name, String city, String shortCode, short points, Short place) {
        this.id = id;
        this.name = name;
        this.city = city;
        // Through the setter so a blank override normalises to null here too — otherwise a
        // "" coming from a DTO would count as "set" and getShortCode() would return "".
        setShortCode(shortCode);
        this.points = points;
        this.place = place;
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
     * Returns the stored {@code short_code} when set, otherwise the fallback derived from
     * {@link #name} by {@link #deriveShortCode(String)}. Callers (DTOs, JSON serialisation)
     * always see a non-null short code, so the frontend can render it without its own
     * fallback. The stored override exists for cases where prefix-matching collides.
     */
    public String getShortCode() {
        if (shortCode != null && !shortCode.isBlank()) {
            // Upper-cased here as well as in the setter: Hibernate writes the field directly
            // when loading a row, so a legacy lower-case value never passes through setShortCode.
            return shortCode.toUpperCase();
        }
        return deriveShortCode(name);
    }

    /** Returns the stored override, or {@code null} when none is set. */
    public String getShortCodeOverride() {
        return shortCode;
    }

    /**
     * Stores an explicit short code. Blank input clears the override, so
     * {@link #getShortCode()} resumes falling back to the name-derived code — that is how
     * the team form un-sets a code the user no longer wants.
     */
    public void setShortCode(String shortCode) {
        // Trimming matters for in-process callers (the importer, tests); requests never reach
        // here with surrounding whitespace, since TeamDto's @Pattern rejects it with a 400.
        this.shortCode = shortCode == null || shortCode.isBlank() ? null : shortCode.trim().toUpperCase();
    }

    /**
     * Derives a short code from a team name: the first {@value #SHORT_CODE_LENGTH}
     * alphanumeric characters, uppercased. Non-alphanumerics are skipped so
     * "FC Barcelona" yields "FCB" rather than "FC ". Shared with the importer's
     * collision-aware generator, which starts from this same value.
     */
    public static String deriveShortCode(String name) {
        var alphanumeric = alphanumericUpperCase(name);
        // Counted in code points, not chars, so a name opening with a supplementary-plane
        // character cannot be cut in the middle of a surrogate pair.
        if (alphanumeric.codePointCount(0, alphanumeric.length()) <= SHORT_CODE_LENGTH) {
            return alphanumeric;
        }
        return alphanumeric.substring(0, alphanumeric.offsetByCodePoints(0, SHORT_CODE_LENGTH));
    }

    /** Uppercased name with every non-alphanumeric character dropped; never null. */
    public static String alphanumericUpperCase(String name) {
        if (name == null) {
            return "";
        }
        var builder = new StringBuilder(name.length());
        name.codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(codePoint -> builder.appendCodePoint(Character.toUpperCase(codePoint)));
        return builder.toString();
    }

    public short getPoints() {
        return points;
    }

    public Short getPlace() {
        return place;
    }

    public void setPlace(Short place) {
        this.place = place;
    }

    public void addPoints(short points) {
        // Explicit cast to silence CodeQL "implicit narrowing in compound assignment".
        // `short + short` evaluates as int in Java; without the cast `this.points += points`
        // would silently truncate. Football match points cap at ~114/season, well under
        // Short.MAX_VALUE (32767), so the cast is purely defensive.
        this.points = (short) (this.points + points);
    }

    @Override
    public String toString() {
        return "Team(name=" + name + ", city=" + city + ", points=" + points + ", place=" + place + ")";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String city;
        private String shortCode;
        private short points;
        private Short place;

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

        public Builder points(short points) {
            this.points = points;
            return this;
        }

        public Builder place(Short place) {
            this.place = place;
            return this;
        }

        public Team build() {
            return new Team(id, name, city, shortCode, points, place);
        }
    }
}
