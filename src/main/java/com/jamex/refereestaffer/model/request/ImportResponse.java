package com.jamex.refereestaffer.model.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public class ImportResponse {

    private final int matches;
    private final int referees;
    private final int grades;
    private final int teams;
}
