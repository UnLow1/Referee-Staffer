package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {

    List<Team> findAllByIdNotIn(List<Long> ids);

    Optional<Team> findByName(String name);
}
