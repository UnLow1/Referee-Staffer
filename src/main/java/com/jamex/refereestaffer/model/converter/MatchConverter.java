package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Grade;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
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
                .queue(entity.getQueue())
                .homeTeamId(entity.getHome().getId())
                .awayTeamId(entity.getAway().getId())
                .refereeId(referee.orElse(null))
                .gradeId(grade.orElse(null))
                .homeScore(entity.getHomeScore())
                .awayScore(entity.getAwayScore())
                .build();
    }

    @Override
    public Match convertFromDto(MatchDto dto) {
        var homeTeam = teamRepository.findById(dto.getHomeTeamId());
        var awayTeam = teamRepository.findById(dto.getAwayTeamId());
        var referee = Optional.ofNullable(dto.getRefereeId())
                .flatMap(refereeRepository::findById);
        var grade = Optional.ofNullable(dto.getGradeId())
                .flatMap(gradeRepository::findById);

        return Match.builder()
                .queue(dto.getQueue())
                .home(homeTeam.orElse(null))
                .away(awayTeam.orElse(null))
                .referee(referee.orElse(null))
                .grade(grade.orElse(null))
                .homeScore(dto.getHomeScore())
                .awayScore(dto.getAwayScore())
                .build();
    }
}
