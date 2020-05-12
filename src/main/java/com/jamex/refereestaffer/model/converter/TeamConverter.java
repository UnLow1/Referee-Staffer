package com.jamex.refereestaffer.model.converter;

import com.jamex.refereestaffer.model.dto.TeamDto;
import com.jamex.refereestaffer.model.entity.Team;
import org.springframework.stereotype.Component;

@Component
public class TeamConverter implements BaseConverter<Team, TeamDto> {

    @Override
    public TeamDto convertFromEntity(Team entity) {
        return TeamDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .city(entity.getCity())
                .points(entity.getPoints())
                .build();
    }

    @Override
    public Team convertFromDto(TeamDto dto) {
        return Team.builder()
                .id(dto.getId())
                .name(dto.getName())
                .city(dto.getCity())
                .build();
    }
}
