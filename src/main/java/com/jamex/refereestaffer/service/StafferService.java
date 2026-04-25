package com.jamex.refereestaffer.service;

import com.jamex.refereestaffer.model.converter.MatchConverter;
import com.jamex.refereestaffer.model.dto.MatchDto;
import com.jamex.refereestaffer.model.entity.Config;
import com.jamex.refereestaffer.model.entity.ConfigName;
import com.jamex.refereestaffer.model.entity.Match;
import com.jamex.refereestaffer.model.entity.Referee;
import com.jamex.refereestaffer.model.entity.Team;
import com.jamex.refereestaffer.model.entity.Vacation;
import com.jamex.refereestaffer.model.exception.StafferException;
import com.jamex.refereestaffer.repository.ConfigurationRepository;
import com.jamex.refereestaffer.repository.VacationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class StafferService {

    private static final Logger log = LoggerFactory.getLogger(StafferService.class);

    private static final double DEFAULT_AVERAGE_GRADE = 8.3;

    private final ConfigurationRepository configurationRepository;
    private final VacationRepository vacationRepository;
    private final MatchConverter matchConverter;
    private final MatchService matchService;
    private final RefereeService refereeService;

    public StafferService(ConfigurationRepository configurationRepository, VacationRepository vacationRepository,
                          MatchConverter matchConverter, MatchService matchService, RefereeService refereeService) {
        this.configurationRepository = configurationRepository;
        this.vacationRepository = vacationRepository;
        this.matchConverter = matchConverter;
        this.matchService = matchService;
        this.refereeService = refereeService;
    }

    public Collection<MatchDto> staffReferees(short queue) {
        var referees = refereeService.getAvailableRefereesForQueue(queue);
        refereeService.calculateStats(referees);
        var sortedMatchesInQueue = matchService.getMatchesToAssignInQueue(queue);
        // Load all config values up front instead of hitting the DB per (referee × match) — see
        // countRefereePotentialLvl. With ~15 referees × ~8 matches that's 600 → 1 query.
        var config = loadConfig();

        assignRefereesToMatches(referees, sortedMatchesInQueue, config);

        return matchConverter.convertFromEntities(sortedMatchesInQueue);
    }

    private Map<ConfigName, Double> loadConfig() {
        return configurationRepository.findAll().stream()
                .collect(Collectors.toMap(Config::getName, Config::getValue));
    }

    private void assignRefereesToMatches(List<Referee> referees, List<Match> matches, Map<ConfigName, Double> config) {
        for (var match : matches) {
            var refereesPotentialLvlMap = new HashMap<Referee, Double>();
            var vacations = vacationRepository.findAllByStartDateIsLessThanEqualAndEndDateIsGreaterThanEqual(match.getDate());

            var refereesWithVacations = vacations.stream()
                    .map(Vacation::getReferee)
                    .toList();

            var availableReferees = referees.stream()
                    .filter(ref -> !ref.isBusy())
                    .filter(ref -> !refereesWithVacations.contains(ref))
                    .toList();

            for (var referee : availableReferees) {
                var potentialLvl = countRefereePotentialLvl(referee, match.getHome(), match.getAway(), config);
                refereesPotentialLvlMap.put(referee, potentialLvl);
            }
            var sortedRefereesPotentialLvlMap = refereesPotentialLvlMap.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));

            log.debug(String.format("Match: %s - %s; hardnessLvl = %f", match.getHome(), match.getAway(), match.getHardnessLvl()));
            log.debug("Referees with their potential:" + sortedRefereesPotentialLvlMap);
            var chosenReferee = sortedRefereesPotentialLvlMap.keySet().stream()
                    .findFirst()
                    .orElseThrow(StafferException::new);
            // TODO separate list for assigned referees id - get rid off field busy
            chosenReferee.setBusy(true);

            match.setReferee(chosenReferee);
        }
    }

    private double countRefereePotentialLvl(Referee referee, Team homeTeam, Team awayTeam, Map<ConfigName, Double> config) {
        var numberOfHomeTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(homeTeam, (short) 0);
        var numberOfAwayTeamRefereedMatches = referee.getTeamsRefereed().getOrDefault(awayTeam, (short) 0);

        var averageGrade = Optional.ofNullable(referee.getAverageGrade())
                .orElse(DEFAULT_AVERAGE_GRADE);

        return config.get(ConfigName.AVERAGE_GRADE_MULTIPLIER) * averageGrade +
                config.get(ConfigName.EXPERIENCE_MULTIPLIER) * referee.getExperience() -
                config.get(ConfigName.NUMBER_OF_MATCHES_MULTIPLIER) * referee.getNumberOfMatchesInRound() -
                config.get(ConfigName.HOME_TEAM_REFEREED_MULTIPLIER) * numberOfHomeTeamRefereedMatches -
                config.get(ConfigName.AWAY_TEAM_REFEREED_MULTIPLIER) * numberOfAwayTeamRefereedMatches;
    }
}
