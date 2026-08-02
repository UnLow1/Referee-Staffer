package com.jamex.refereestaffer.model.request;

public record ImportResponse(
        int matches,
        int referees,
        int grades,
        int teams
) {
}
