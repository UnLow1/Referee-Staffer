package com.jamex.refereestaffer.service

import spock.lang.Specification
import spock.lang.Subject

class TeamShortCodeGeneratorSpec extends Specification {

    @Subject
    TeamShortCodeGenerator generator = new TeamShortCodeGenerator()

    def "should derive the plain three-letter prefix when nothing collides"() {
        expect:
        generator.generate(name) == expectedCode

        where:
        name               || expectedCode
        "Legia"            || "LEG"
        "Korona"           || "KOR"
        "legia"            || "LEG"
        "AC"               || "AC"
        "X"                || "X"
        "1"                || "1"
        "16"               || "16"
    }

    def "should skip non-alphanumeric characters so separators never land in the code"() {
        expect:
        generator.generate(name) == expectedCode

        where:
        name                || expectedCode
        "FC Barcelona"      || "FCB"
        "  Legia"           || "LEG"
        "A.C. Milan"        || "ACM"
        "Lech-Poznan"       || "LEC"
    }

    def "should resolve a prefix collision by swapping the last character for a later one"() {
        when: "two teams whose first three letters are identical"
        def first = generator.generate("Lech")
        def second = generator.generate("Lechia")

        then: "the first asker keeps the plain prefix and the second slides to the next letter"
        first == "LEC"
        second == "LEH"
    }

    def "should keep sliding through the remaining characters for a run of collisions"() {
        when:
        def codes = ["Lech", "Lechia", "Lechxx", "Lecha"].collect { generator.generate(it) }

        then: "LEC first, then the third character walks the rest of each name"
        codes == ["LEC", "LEH", "LEX", "LEA"]
        codes.toSet().size() == 4
    }

    def "should fall back to a numeric suffix when the name offers no distinct characters"() {
        when: "the same name twice - the ladder has no alternative letters to offer"
        def first = generator.generate("Lec")
        def second = generator.generate("Lec")
        def third = generator.generate("Lec")

        then:
        first == "LEC"
        second == "LEC2"
        third == "LEC3"
    }

    def "should stay inside the eight-character column even at the end of the ladder"() {
        when: "the ladder is driven all the way to its numeric tail"
        def codes = (1..100).collect { generator.generate("Lec") }

        then: "every issued code fits the short_code column"
        codes.findAll { it != null }.every { it.length() <= 8 }
        and: "the ladder is finite - the last request past LEC99 gets nothing"
        codes.last() == null
    }

    def "should return null when the name carries no alphanumeric character"() {
        expect:
        generator.generate(name) == null

        where:
        name << [null, "", "   ", "-.-", "???"]
    }

    def "should not reissue a code already handed out for an unrelated name"() {
        when: "a name whose prefix was already taken by a completely different team"
        def legia = generator.generate("Legia")
        def leg = generator.generate("Leg")

        then:
        legia == "LEG"
        and: "Leg has no fourth character to slide to, so it takes the numeric suffix"
        leg == "LEG2"
    }
}
