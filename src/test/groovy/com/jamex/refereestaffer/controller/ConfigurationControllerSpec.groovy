package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.entity.Config
import com.jamex.refereestaffer.model.entity.ConfigName
import com.jamex.refereestaffer.repository.ConfigurationRepository
import groovy.json.JsonSlurper
import org.spockframework.runtime.model.parallel.ExecutionMode
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Execution
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

// Features must run on one thread: the @SpringBean mocks live in the shared Spring
// context, so concurrent features would attach/stub the same mock instances at once.
@Execution(ExecutionMode.SAME_THREAD)
@WebMvcTest(ConfigurationController)
class ConfigurationControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    ConfigurationRepository configurationRepository = Mock()

    def "should return configuration with group and description in JSON"() {
        given:
        def configs = [new Config(ConfigName.AVERAGE_GRADE_MULTIPLIER, 50.0d),
                       new Config(ConfigName.EXPERIENCE_MULTIPLIER, 0.01d)]

        when:
        def response = mockMvc.perform(get("/api/configuration")).andReturn().response

        then:
        1 * configurationRepository.findAll() >> configs
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json*.name == ["AVERAGE_GRADE_MULTIPLIER", "EXPERIENCE_MULTIPLIER"]
        json*.value == [50.0, 0.01]
        json.every { it.group != null && it.description != null }
    }

    def "should update configuration from JSON body and echo the saved values"() {
        when:
        def response = mockMvc.perform(put("/api/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content('[{"name": "EXPERIENCE_MULTIPLIER", "value": 0.5}]'))
                .andReturn().response

        then:
        1 * configurationRepository.saveAll({ List<Config> configs ->
            configs.size() == 1 && configs[0].name == ConfigName.EXPERIENCE_MULTIPLIER && configs[0].value == 0.5d
        }) >> [new Config(ConfigName.EXPERIENCE_MULTIPLIER, 0.5d)]
        response.status == 200
        def json = new JsonSlurper().parseText(response.contentAsString)
        json[0].name == "EXPERIENCE_MULTIPLIER"
        json[0].value == 0.5
    }

    def "should reject configuration update when a value is missing"() {
        when:
        def response = mockMvc.perform(put("/api/configuration")
                .contentType(MediaType.APPLICATION_JSON)
                .content('[{"name": "EXPERIENCE_MULTIPLIER"}]'))
                .andReturn().response

        then:
        0 * configurationRepository._
        response.status == 400
        def json = new JsonSlurper().parseText(response.contentAsString)
        json.detail == "[0].value: must not be null"
    }
}
