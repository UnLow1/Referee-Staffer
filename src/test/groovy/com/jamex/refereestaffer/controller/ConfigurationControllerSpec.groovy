package com.jamex.refereestaffer.controller

import com.jamex.refereestaffer.model.entity.Config
import com.jamex.refereestaffer.repository.ConfigurationRepository
import spock.lang.Specification
import spock.lang.Subject

class ConfigurationControllerSpec extends Specification {

    @Subject
    ConfigurationController configurationController

    ConfigurationRepository configurationRepository = Mock()

    def setup() {
        configurationController = new ConfigurationController(configurationRepository)
    }

    def "should return configurations"() {
        given:
        def configs = [[] as Config, [] as Config]

        when:
        def result = configurationController.getConfiguration()

        then:
        1 * configurationRepository.findAll() >> configs
        result == configs
    }

    def "should update configuration"() {
        given:
        def configs = [[] as Config, [] as Config]

        when:
        def result = configurationController.updateConfiguration(configs)

        then:
        1 * configurationRepository.saveAll(configs) >> configs
        result == configs
    }
}
