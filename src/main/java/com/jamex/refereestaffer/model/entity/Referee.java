package com.jamex.refereestaffer.model.entity;


import lombok.*;

import javax.persistence.*;
import java.util.List;
import java.util.Map;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@ToString(exclude = {"matches", "id", "email", "teamsRefereed", "busy"})
public class Referee {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column
    private String email;

    @OneToMany(mappedBy = "referee", targetEntity = Match.class)
    private List<Match> matches;

    @Column
    private int experience;

    @Transient
    @Setter
    private double averageGrade;

    @Transient
    @Setter
    private short numberOfMatchesInRound;

    @Transient
    @Setter
    private Map<Team, Short> teamsRefereed;

    @Transient
    @Setter
    private boolean busy;

    public Referee(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    public Referee(String firstName, String lastName, String email, int experience) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.experience = experience;
    }
}
