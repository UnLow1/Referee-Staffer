package com.jamex.refereestaffer.model.converter;

/**
 * Dto → entity half of a converter, for dtos that need no resolved entities and can therefore
 * be mapped from the dto alone. A converter whose dto → entity direction does need them takes
 * them as parameters instead and implements only {@link EntityToDtoConverter} — see
 * {@code MatchConverter} for that case.
 *
 * @param <E> the entity type, first for symmetry with {@link EntityToDtoConverter} so a class
 *            implementing both repeats the same pair; note this puts it opposite to the
 *            reading order of the interface name
 * @param <D> the dto type
 */
public interface DtoToEntityConverter<E, D> {

    E convertFromDto(D dto);
}
