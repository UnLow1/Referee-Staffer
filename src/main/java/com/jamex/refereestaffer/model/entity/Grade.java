package com.jamex.refereestaffer.model.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToOne;

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
    @Setter
    private Match match;

    public Grade(Match match, double value) {
        this.match = match;
        this.value = value;
    }
}
