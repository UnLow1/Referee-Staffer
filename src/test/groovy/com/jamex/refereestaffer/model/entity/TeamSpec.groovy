package com.jamex.refereestaffer.model.entity

import spock.lang.Specification

class TeamSpec extends Specification {

    def "should derive the short code from the name when no override is stored"() {
        expect:
        new Team(name).getShortCode() == expectedCode

        where:
        name           || expectedCode
        "Legia"        || "LEG"
        "korona"       || "KOR"
        "FC Barcelona" || "FCB"
        "AC"           || "AC"
        null           || ""
        ""             || ""
        "   "          || ""
    }

    def "should prefer the stored override over the name-derived code"() {
        given:
        def team = new Team("Lechia")

        when:
        team.setShortCode("LGA")

        then:
        team.getShortCode() == "LGA"
        team.getShortCodeOverride() == "LGA"
    }

    def "should normalise a stored override to trimmed upper case"() {
        given:
        def team = new Team("Lechia")

        when:
        team.setShortCode(raw)

        then:
        team.getShortCode() == expectedCode
        team.getShortCodeOverride() == expectedCode

        where:
        raw      || expectedCode
        "lga"    || "LGA"
        " lga "  || "LGA"
        "LgA"    || "LGA"
    }

    def "should clear the override on a blank value so the name-derived code resumes"() {
        given:
        def team = Team.builder().name("Lechia").shortCode("LGA").build()

        when:
        team.setShortCode(raw)

        then: "no override is stored any more"
        team.getShortCodeOverride() == null
        and: "the derived code takes over instead of an empty string"
        team.getShortCode() == "LEC"

        where:
        raw << [null, "", "   "]
    }

    def "should treat a blank override passed to the constructor as absent"() {
        when:
        def team = Team.builder().name("Lechia").shortCode(raw).build()

        then:
        team.getShortCodeOverride() == null
        team.getShortCode() == "LEC"

        where:
        raw << [null, "", "  "]
    }
}
