package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.exception.TeamNotFoundException;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@RequiredArgsConstructor
@Component
public class MatchConverter implements BaseConverter<Match, MatchDto> {

    private final TeamRepository teamRepository;
    private final RefereeRepository refereeRepository;
    private final GradeRepository gradeRepository;

    @Override
    public MatchDto convertFromEntity(Match entity) {
        var referee = Optional.ofNullable(entity.getReferee())
                .map(Referee::getId);
        var grade = Optional.ofNullable(entity.getGrade())
                .map(Grade::getId);

        return MatchDto.builder()
                .id(entity.getId())
                .queue(entity.getQueue())
                .homeTeamId(entity.getHome().getId())
                .awayTeamId(entity.getAway().getId())
                .date(entity.getDate())
                .refereeId(referee.orElse(null))
                .gradeId(grade.orElse(null))
                .homeScore(entity.getHomeScore())
                .awayScore(entity.getAwayScore())
                .build();
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

        return Match.builder()
                .id(dto.getId())
                .queue(dto.getQueue())
                .home(homeTeam)
                .away(awayTeam)
                .date(dto.getDate())
                .referee(referee.orElse(null))
                .grade(grade.orElse(null))
                .homeScore(dto.getHomeScore())
                .awayScore(dto.getAwayScore())
                .build();
    }
}
