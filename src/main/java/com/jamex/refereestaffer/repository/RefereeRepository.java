package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Referee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefereeRepository extends JpaRepository<Referee, Long> {

    List<Referee> findAll();

    Optional<Referee> findByFirstNameAndLastName(String firstName, String lastName);
}
