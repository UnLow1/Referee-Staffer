package com.jamex.refereestaffer.model.exception;

public class GradeNotFoundException extends RuntimeException {

    public static final String NOT_FOUND = "Grade with id %d has not been found";

    public GradeNotFoundException(Long id) {
        super(String.format(NOT_FOUND, id));
    }
}
