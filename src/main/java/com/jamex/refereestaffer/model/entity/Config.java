package com.jamex.refereestaffer.model.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
@ToString
public class Config {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConfigName name;

    @Column(nullable = false)
    private Double value;

    public Config(ConfigName name, Double value) {
        this.name = name;
        this.value = value;
    }
}
