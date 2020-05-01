package com.jamex.refereestaffer.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;
import java.util.List;

@Entity
@NoArgsConstructor
@Getter
@ToString(exclude = "grades")
public class Referee {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;

    @JsonIgnore
    @OneToMany(mappedBy = "referee", targetEntity = Grade.class)
    private List<Grade> grades;

    @Column(nullable = false)
    private int experience;

    public Referee(String firstName, String lastName, String email, int experience) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.experience = experience;
    }
}
