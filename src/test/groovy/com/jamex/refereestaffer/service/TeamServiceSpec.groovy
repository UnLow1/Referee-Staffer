package com.jamex.refereestaffer.service


import com.jamex.refereestaffer.repository.MatchRepository
import com.jamex.refereestaffer.repository.TeamRepository
import spock.lang.Specification
import spock.lang.Subject

class TeamServiceSpec extends Specification {

    @Subject
    TeamService teamService

    TeamRepository teamRepository = Mock()
    MatchRepository matchRepository = Mock()
    MatchService matchService = Mock()

    def setup() {
        teamService = new TeamService(teamRepository, matchRepository, matchService)
    }
}
