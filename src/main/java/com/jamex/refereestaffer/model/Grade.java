package com.jamex.refereestaffer.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
@ToString
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    private double value;

    @ManyToOne(targetEntity = Referee.class)
    private Referee referee;

    public Grade(double value, Referee referee) {
        this.value = value;
        this.referee = referee;
    }
}
