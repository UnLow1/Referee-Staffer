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

    def "should resolve a prefix collision with the longer prefix before rearranging letters"() {
        when: "two teams whose first three letters are identical"
        def first = generator.generate("Lech")
        def second = generator.generate("Lechia")

        then: "the first asker keeps the plain prefix, the second extends it by one character"
        first == "LEC"
        and: "LECH reads as the name, which LEH would not"
        second == "LECH"
    }

    def "should slide the third character once both prefixes are taken"() {
        when:
        def codes = ["Lech", "Lechia", "Lechxx", "Lecha"].collect { generator.generate(it) }

        then: "LEC, then LECH, and only then does the third character walk the rest of the name"
        codes == ["LEC", "LECH", "LEH", "LEA"]
        codes.toSet().size() == 4
    }

    def "should fall back to a numeric suffix when the name offers no distinct characters"() {
        when: "the same three-letter name repeatedly - no longer prefix, nothing to slide to"
        def codes = (1..3).collect { generator.generate("Lec") }

        then:
        codes == ["LEC", "LEC2", "LEC3"]
    }

    def "should keep every generated code renderable in the team pill"() {
        when: "the ladder is driven all the way through its numeric tail"
        def codes = (1..20).collect { generator.generate("Lec") }

        then: "nothing exceeds the width the 22x22 pill can show"
        codes.findAll { it != null }.every { it.length() <= TeamShortCodeGenerator.MAX_CODE_LENGTH }
        and: "the ladder is finite - LEC plus LEC2..LEC9 is all a three-letter name can yield"
        codes.findAll { it != null } == ["LEC", "LEC2", "LEC3", "LEC4", "LEC5", "LEC6", "LEC7", "LEC8", "LEC9"]
        codes.last() == null
    }

    def "should keep Polish diacritics in a generated code"() {
        expect:
        generator.generate(name) == expectedCode

        where:
        name       || expectedCode
        "Slask"    || "SLA"
        "\u015al\u0105sk"    || "\u015aL\u0104"
        "\u0141KS \u0141\u00f3d\u017a" || "\u0141KS"
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
        and: "Leg has no fourth character to extend or slide to, so it takes the numeric suffix"
        leg == "LEG2"
    }

    def "should not split a surrogate pair when the name opens with one"() {
        given: "a name whose first characters live outside the basic multilingual plane"
        def name = "\ud835\udc00\ud835\udc01\ud835\udc02\ud835\udc03"

        when:
        def code = generator.generate(name)

        then: "three whole code points, not three chars cutting a pair in half"
        code.codePointCount(0, code.length()) == 3
        code == "\ud835\udc00\ud835\udc01\ud835\udc02"
    }
}
