package com.jamex.refereestaffer.model.entity

import spock.lang.Specification

class MatchSpec extends Specification {

    def "should tell whether the staffer may re-decide the referee: #description"() {
        given:
        def match = Match.builder()
                .referee(referee)
                .homeScore(homeScore as Short)
                .awayScore(awayScore as Short)
                .build()

        expect:
        match.isReassignable() == reassignable

        where:
        description                        | referee            | homeScore | awayScore || reassignable
        "unassigned, not played"           | null               | null      | null      || true
        // An unassigned match stays open even once it is played: there is nothing to rewrite.
        "unassigned, already played"       | null               | 2         | 1         || true
        "assigned, not played"             | referee("John", "Doe") | null  | null      || true
        "assigned and played"              | referee("John", "Doe") | 2     | 1         || false
        // Any score already recorded pins the assignment, even a half-entered one.
        "assigned, only home score"        | referee("John", "Doe") | 2     | null      || false
        "assigned, only away score"        | referee("John", "Doe") | null  | 1         || false
        // "S C" = Sędzia z Centrali: assigned top-down, never reassignable.
        "central sentinel, not played"     | referee("S", "C")  | null      | null      || false
        "central sentinel, played"         | referee("S", "C")  | 2         | 1         || false
    }

    private static Referee referee(String firstName, String lastName) {
        Referee.builder().firstName(firstName).lastName(lastName).build()
    }
}
