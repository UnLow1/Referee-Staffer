package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Config;
import com.jamex.refereestaffer.model.entity.ConfigName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.stream.Collectors;

@Repository
public interface ConfigurationRepository extends JpaRepository<Config, Long> {

    Config findByName(ConfigName name);

    /**
     * All config values in one query. Callers that score in a loop (staffing, hardness
     * per match) must use this instead of {@link #findByName} so the DB isn't hit per
     * iteration.
     */
    default Map<ConfigName, Double> findAllAsMap() {
        return findAll().stream()
                .collect(Collectors.toMap(Config::getName, Config::getValue));
    }
}
