package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.StreamSupport;

/**
 * Pure mapping between {@link Match} and {@link MatchDto} — no repository access.
 * Resolving ids to entities (teams, referee, grade) is the service layer's job
 * ({@link com.jamex.refereestaffer.service.MatchService}), so the dto → entity
 * direction takes already-resolved entities instead of implementing
 * {@link BaseConverter#convertFromDto(Object)}.
 */
@Component
public class MatchConverter {

    public MatchDto convertFromEntity(Match entity) {
        var referee = Optional.ofNullable(entity.getReferee())
                .map(Referee::getId);
        var grade = Optional.ofNullable(entity.getGrade())
                .map(Grade::getId);

        return new MatchDto(
                entity.getId(),
                entity.getQueue(),
                entity.getHome().getId(),
                entity.getAway().getId(),
                entity.getDate(),
                referee.orElse(null),
                entity.getHomeScore(),
                entity.getAwayScore(),
                grade.orElse(null),
                entity.getHardnessLvl());
    }

    public Collection<MatchDto> convertFromEntities(final Iterable<Match> entities) {
        return StreamSupport.stream(entities.spliterator(), false)
                .map(this::convertFromEntity)
                .toList();
    }

    public Match convertFromDto(MatchDto dto, Team home, Team away, Referee referee, Grade grade) {
        return new Match(
                dto.getId(),
                dto.getQueue(),
                home,
                away,
                dto.getDate(),
                referee,
                grade,
                dto.getHomeScore(),
                dto.getAwayScore(),
                0.0);
    }
}
