package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.Referee;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefereeRepository extends CrudRepository<Referee, Long> {
}
