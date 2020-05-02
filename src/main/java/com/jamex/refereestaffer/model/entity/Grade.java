package com.jamex.refereestaffer.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
@ToString(exclude = "match")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private double value;

    @JsonIgnore
    @OneToOne(targetEntity = Match.class)
    private Match match;

    public Grade(Match match, double value) {
        this.match = match;
        this.value = value;
    }
}
