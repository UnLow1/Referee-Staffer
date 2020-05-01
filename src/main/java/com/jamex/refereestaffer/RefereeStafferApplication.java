package com.jamex.refereestaffer;

import com.jamex.refereestaffer.model.Grade;
import com.jamex.refereestaffer.model.Match;
import com.jamex.refereestaffer.model.Referee;
import com.jamex.refereestaffer.model.Team;
import com.jamex.refereestaffer.repository.GradeRepository;
import com.jamex.refereestaffer.repository.MatchRepository;
import com.jamex.refereestaffer.repository.RefereeRepository;
import com.jamex.refereestaffer.repository.TeamRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class RefereeStafferApplication {

    public static void main(String[] args) {
        SpringApplication.run(RefereeStafferApplication.class, args);
    }

    @Bean
    CommandLineRunner init(RefereeRepository refereeRepository, GradeRepository gradeRepository,
                           MatchRepository matchRepository, TeamRepository teamRepository) {
        return args -> {
            var ref1 = new Referee("Adam", "Jamka", "ajamka@gmail.com", 150);
            var ref2 = new Referee("Jan", "Kowalski", "jkowalski@gmail.com", 24);
            var ref3 = new Referee("Tomasz", "Nowak", "tnowak@gmail.com", 532);

            var team1 = new Team("Korona", "Kielce");
            var team2 = new Team("Wisła", "Kraków");
            var team3 = new Team("Cracovia", "Kraków");
            var team4 = new Team("Legia", "Warszawa");
            var team5 = new Team("Lech", "Poznań");
            var team6 = new Team("Łks", "Łódź");

            var match1 = new Match(team1, team2, ref1, 1, 0);
            var match2 = new Match(team3, team5, ref2, 0, 0);
            var match3 = new Match(team5, team6, ref3, 0, 2);
            var match4 = new Match(team1, team3, ref1, 1, 2);
            var match5 = new Match(team1, team4, ref2, 3, 3);
            var match6 = new Match(team6, team2, ref3, 2, 2);
            var match7 = new Match(team2, team4, ref1, 0, 0);
            var match8 = new Match(team3, team6, ref2, 1, 3);
            var match9 = new Match(team4, team5, ref3, 2, 2);

            var grade1 = new Grade(match1, 8.3);
            var grade2 = new Grade(match2, 8.2);
            var grade3 = new Grade(match3, 7.9);
            var grade4 = new Grade(match4, 8.5);
            var grade5 = new Grade(match5, 8.1);
            var grade6 = new Grade(match6, 8.4);
            var grade7 = new Grade(match7, 8.2);
            var grade8 = new Grade(match8, 8.3);
            var grade9 = new Grade(match9, 7.9);

            refereeRepository.saveAll(List.of(ref1, ref2, ref3));
            teamRepository.saveAll(List.of(team1, team2, team3, team4, team5, team6));
            matchRepository.saveAll(List.of(match1, match2, match3, match4, match5, match6, match7, match8, match9));
            gradeRepository.saveAll(List.of(grade1, grade2, grade3, grade4, grade5, grade6, grade7, grade8, grade9));

            refereeRepository.findAll().forEach(System.out::println);
            gradeRepository.findAll().forEach(System.out::println);
            teamRepository.findAll().forEach(System.out::println);
            matchRepository.findAll().forEach(System.out::println);
        };
    }
}
