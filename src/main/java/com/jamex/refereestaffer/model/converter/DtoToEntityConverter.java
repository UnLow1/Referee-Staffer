package com.jamex.refereestaffer.model.converter;

/**
 * Dto → entity half of a converter, for dtos that carry no references to other entities
 * and can therefore be mapped from the dto alone.
 *
 * <p>Deliberately separate from {@link EntityToDtoConverter}: a dto whose id fields point at
 * other entities (match, grade, referee, team) cannot be mapped without those entities, and
 * resolving them is the service layer's job — so {@code MatchConverter}, {@code GradeConverter}
 * and {@code VacationConverter} expose a {@code convertFromDto} taking the resolved entities
 * and implement only the entity → dto half. Before the split, opting out of one direction
 * meant opting out of the whole interface and re-implementing the bulk entity → dto mapping.
 */
public interface DtoToEntityConverter<E, D> {

    E convertFromDto(D dto);
}
