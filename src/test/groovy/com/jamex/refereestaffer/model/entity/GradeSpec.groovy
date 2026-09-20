package com.jamex.refereestaffer.model.entity

import spock.lang.Specification

class GradeSpec extends Specification {

    def "should return single value as effective value for a plain grade"() {
        given:
        def grade = Grade.builder()
                .value(8.3d)
                .build()

        expect:
        grade.effectiveValue == 8.3d
    }

    def "should return arithmetic mean of both components as effective value for a split grade"() {
        given:
        def grade = Grade.builder()
                .value(7.9d)
                .secondValue(8.3d)
                .build()

        expect:
        grade.effectiveValue == (7.9d + 8.3d) / 2
    }

    def "should return null effective value when grade has no value"() {
        given:
        def grade = Grade.builder().build()

        expect:
        grade.effectiveValue == null
    }
}
