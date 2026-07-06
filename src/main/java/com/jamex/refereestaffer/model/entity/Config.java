package com.jamex.refereestaffer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

@Entity
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Bean-validation mirrors of the NOT NULL columns — the entity doubles as the
    // request body of PUT /api/configuration, so nulls must 400 at the controller
    // instead of surfacing as a 500 constraint violation on flush.
    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigName name;

    @NotNull
    @Column(nullable = false)
    private Double value;

    public Config() {
    }

    public Config(ConfigName name, Double value) {
        this.name = name;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public ConfigName getName() {
        return name;
    }

    public Double getValue() {
        return value;
    }

    /**
     * Surfaced in JSON — drives the Configuration screen's panel grouping. Computed from
     * {@link ConfigName#group()} so adding a new key doesn't require touching this entity.
     */
    public String getGroup() {
        return name != null ? name.group() : null;
    }

    /** Surfaced in JSON — short human-readable explanation shown under each input. */
    public String getDescription() {
        return name != null ? name.description() : null;
    }

    @Override
    public String toString() {
        return "Config(id=" + id + ", name=" + name + ", value=" + value + ")";
    }
}
