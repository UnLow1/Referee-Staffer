package com.jamex.refereestaffer.controller;

import com.jamex.refereestaffer.model.entity.Config;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/configuration")
public class ConfigurationController {

    private final ConfigurationRepository configurationRepository;

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
