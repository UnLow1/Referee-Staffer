package com.jamex.refereestaffer.repository;

import com.jamex.refereestaffer.model.entity.Referee;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefereeRepository extends CrudRepository<Referee, Long> {
}
