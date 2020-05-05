package com.jamex.refereestaffer.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "match")
public class Grade {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private Double value;

    @JsonIgnore
    @OneToOne(targetEntity = Match.class)
    private Match match;

    public Grade(Match match, double value) {
        this.match = match;
        this.value = value;
    }
}
