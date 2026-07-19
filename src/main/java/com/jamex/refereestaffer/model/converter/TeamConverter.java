package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.TeamDto;
import com.jamex.refereestaffer.model.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamConverter implements BaseConverter<Team, TeamDto> {

    @Override
    public TeamDto convertFromEntity(Team entity) {
        return new TeamDto(entity.getId(), entity.getName(), entity.getCity(),
                entity.getShortCode(), entity.getPoints());
    }

    @Override
    public Team convertFromDto(TeamDto dto) {
        // shortCode is deliberately not taken from the DTO: `short` in JSON is a computed
        // read-model field (GET fills it with the name-derived fallback), so echoing it
        // back on writes would persist the fallback as a stored override.
        return new Team(dto.getId(), dto.getName(), dto.getCity(), null, (short) 0, null);
    }
}
