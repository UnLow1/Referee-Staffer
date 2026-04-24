package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.exception.TeamNotFoundException;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MatchConverter implements BaseConverter<Match, MatchDto> {

    private final TeamRepository teamRepository;
    private final RefereeRepository refereeRepository;
    private final GradeRepository gradeRepository;

    public MatchConverter(TeamRepository teamRepository, RefereeRepository refereeRepository, GradeRepository gradeRepository) {
        this.teamRepository = teamRepository;
        this.refereeRepository = refereeRepository;
        this.gradeRepository = gradeRepository;
    }

    @Override
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
                grade.orElse(null));
    }

    @Override
    public Match convertFromDto(MatchDto dto) {
        var homeTeam = teamRepository.findById(dto.getHomeTeamId())
                .orElseThrow(() -> new TeamNotFoundException(dto.getHomeTeamId()));
        var awayTeam = teamRepository.findById(dto.getAwayTeamId())
                .orElseThrow(() -> new TeamNotFoundException(dto.getAwayTeamId()));
        var referee = Optional.ofNullable(dto.getRefereeId())
                .flatMap(refereeRepository::findById);
        var grade = Optional.ofNullable(dto.getGradeId())
                .flatMap(gradeRepository::findById);

        return new Match(
                dto.getId(),
                dto.getQueue(),
                homeTeam,
                awayTeam,
                dto.getDate(),
                referee.orElse(null),
                grade.orElse(null),
                dto.getHomeScore(),
                dto.getAwayScore(),
                0.0);
    }
}
