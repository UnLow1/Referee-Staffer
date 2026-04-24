package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.TeamDto;
import com.jamex.refereestaffer.model.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamConverter implements BaseConverter<Team, TeamDto> {

    @Override
    public TeamDto convertFromEntity(Team entity) {
        return new TeamDto(entity.getId(), entity.getName(), entity.getCity(), entity.getPoints());
    }

    @Override
    public Team convertFromDto(TeamDto dto) {
        return new Team(dto.getId(), dto.getName(), dto.getCity(), (short) 0, (short) 0);
    }
}
