package com.jamex.refereestaffer.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;

@Entity
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column
    private String city;

    @Transient
    private short points;

    @Transient
    private short place;

    public Team() {
    }

    public Team(Long id, String name, String city, short points, short place) {
        this.id = id;
        this.name = name;
        this.city = city;
        this.points = points;
        this.place = place;
    }

    public Team(String name) {
        this.name = name;
    }

    public Team(String name, String city) {
        this.name = name;
        this.city = city;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public short getPoints() {
        return points;
    }

    public short getPlace() {
        return place;
    }

    public void setPlace(short place) {
        this.place = place;
    }

    public void addPoints(int points) {
        this.points += points;
    }

    @Override
    public String toString() {
        return "Team(name=" + name + ", city=" + city + ", points=" + points + ", place=" + place + ")";
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private String name;
        private String city;
        private short points;
        private short place;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder city(String city) {
            this.city = city;
            return this;
        }

        public Builder points(short points) {
            this.points = points;
            return this;
        }

        public Builder place(short place) {
            this.place = place;
            return this;
        }

        public Team build() {
            return new Team(id, name, city, points, place);
        }
    }
}
