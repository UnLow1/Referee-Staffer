package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfigurationRepository extends JpaRepository<Config, Long> {

    Config findByName(String name);
}
