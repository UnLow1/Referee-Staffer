package com.jamex.refereestaffer.model.entity;

import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Short queue;

    @ManyToOne(targetEntity = Team.class)
    private Team home;

    @ManyToOne(targetEntity = Team.class)
    private Team away;

    @ManyToOne(targetEntity = Referee.class)
    private Referee referee;

    @OneToOne(mappedBy = "match", targetEntity = Grade.class)
    private Grade grade;

    private Short homeScore;

    private Short awayScore;

    public Match(short queue, Team home, Team away, Referee referee, Short homeScore, Short awayScore) {
        this.queue = queue;
        this.home = home;
        this.away = away;
        this.referee = referee;
        this.homeScore = homeScore;
        this.awayScore = awayScore;
    }
}
