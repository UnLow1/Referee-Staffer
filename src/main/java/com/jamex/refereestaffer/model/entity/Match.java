package com.jamex.refereestaffer.model.entity;

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

    private short queue;

    @ManyToOne(targetEntity = Team.class)
    private Team home;

    @ManyToOne(targetEntity = Team.class)
    private Team away;

    @ManyToOne(targetEntity = Referee.class)
    private Referee referee;

    @OneToOne(mappedBy = "match", targetEntity = Grade.class)
    private Grade grade;

    private int homeScore;

    private int awayScore;

    public Match(short queue, Team home, Team away, Referee referee, int homeScore, int awayScore) {
        this.queue = queue;
        this.home = home;
        this.away = away;
        this.referee = referee;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
}
