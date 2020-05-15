package com.jamex.refereestaffer.model.exception;

public class TeamNotFoundException extends RuntimeException {

    private static final String NOT_FOUND = "Team with name = \"%s\" has not been found";

    public TeamNotFoundException(String name) {
        super(String.format(NOT_FOUND, name));
    }
}
