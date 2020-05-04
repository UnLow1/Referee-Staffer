package com.jamex.refereestaffer.model.entity;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;
import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@ToString(exclude = "matches")
public class Referee {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;

    @JsonIgnore // TODO not needed when using converters
    @OneToMany(mappedBy = "referee", targetEntity = Match.class)
    private List<Match> matches;

    @Column(nullable = false)
    private int experience;

    public Referee(String firstName, String lastName, String email, int experience) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.experience = experience;
    }
}
