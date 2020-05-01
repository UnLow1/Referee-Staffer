package com.jamex.refereestaffer.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
@ToString
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @ManyToOne(targetEntity = Team.class)
    private Team home;

    @ManyToOne(targetEntity = Team.class)
    private Team away;

    @ManyToOne(targetEntity = Referee.class)
    private Referee referee;

    public Match(Team home, Team away, Referee referee) {
        this.home = home;
        this.away = away;
        this.referee = referee;
    }
}
