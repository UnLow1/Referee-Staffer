package com.jamex.refereestaffer.model.exception;

public class VacationNotFoundException extends RuntimeException {

    public static final String NOT_FOUND = "Vacation with id %d has not been found";

    public VacationNotFoundException(Long id) {
        super(String.format(NOT_FOUND, id));
    }
}
