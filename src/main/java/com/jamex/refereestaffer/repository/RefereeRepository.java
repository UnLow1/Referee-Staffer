package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Referee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefereeRepository extends JpaRepository<Referee, Long> {

    List<Referee> findAll();

    Optional<Referee> findByFirstNameAndLastName(String firstName, String lastName);

    @Query(value = "SELECT * FROM referee " +
            "WHERE id NOT IN " +
            "(SELECT referee_id FROM match " +
            "WHERE queue = :queue " +
            "AND referee_id IS NOT NULL)", nativeQuery = true)
    List<Referee> findAllWithNoMatchInQueue(@Param("queue") Short queue);
}
