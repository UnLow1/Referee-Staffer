package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.entity.Config;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/configuration")
public class ConfigurationController {

    private static final Logger log = LoggerFactory.getLogger(ConfigurationController.class);

    private final ConfigurationRepository configurationRepository;

    public ConfigurationController(ConfigurationRepository configurationRepository) {
        this.configurationRepository = configurationRepository;
    }

    @GetMapping
    public Collection<Config> getConfiguration() {
        log.info("Getting configuration");
        return configurationRepository.findAll();
    }

    @PutMapping
    public Collection<Config> updateConfiguration(@RequestBody List<Config> config) {
        log.info("Updating configuration");
        return configurationRepository.saveAll(config);
    }
}
