package com.jamex.refereestaffer.model.request;

public class ImportResponse {

    private final int matches;
    private final int referees;
    private final int grades;
    private final int teams;

    public ImportResponse(int matches, int referees, int grades, int teams) {
        this.matches = matches;
        this.referees = referees;
        this.grades = grades;
        this.teams = teams;
    }

    public int getMatches() {
        return matches;
    }

    public int getReferees() {
        return referees;
    }

    public int getGrades() {
        return grades;
    }

    public int getTeams() {
        return teams;
    }
}
