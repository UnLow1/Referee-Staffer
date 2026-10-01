package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.TeamDto;
import com.jamex.refereestaffer.model.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamConverter implements BaseConverter<Team, TeamDto> {

    @Override
    public TeamDto convertFromEntity(Team entity) {
        return new TeamDto(entity.getId(), entity.getName(), entity.getCity(),
                entity.getShortCode(), entity.getShortCodeOverride(), entity.getPoints());
    }

    @Override
    public Team convertFromDto(TeamDto dto) {
        // `short` is deliberately not read from the DTO: it is a computed read-model field
        // (GET fills it with the name-derived fallback), so echoing it back on writes would
        // persist the fallback as a stored override. `shortOverride` carries the real intent
        // and is null unless the client actually set a code.
        return new Team(dto.id(), dto.name(), dto.city(), dto.shortOverride(), (short) 0, null);
    }
}
