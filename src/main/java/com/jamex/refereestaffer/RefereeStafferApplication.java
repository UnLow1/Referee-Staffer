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
            refereeRepository.saveAll(List.of(ref1, ref2, ref3));

            gradeRepository.save(new Grade(8.3, ref1));
            gradeRepository.save(new Grade(8.3, ref1));
            gradeRepository.save(new Grade(8.1, ref1));
            gradeRepository.save(new Grade(8.2, ref1));
            gradeRepository.save(new Grade(7.9, ref2));
            gradeRepository.save(new Grade(8.0, ref2));
            gradeRepository.save(new Grade(8.4, ref2));
            gradeRepository.save(new Grade(8.4, ref2));
            gradeRepository.save(new Grade(8.4, ref3));
            gradeRepository.save(new Grade(8.1, ref3));
            gradeRepository.save(new Grade(8.2, ref3));
            gradeRepository.save(new Grade(8.2, ref3));

            var team1 = new Team("Korona", "Kielce", 30);
            var team2 = new Team("Wisła", "Kraków", 28);
            var team3 = new Team("Cracovia", "Kraków", 40);
            var team4 = new Team("Legia", "Warszawa", 52);
            var team5 = new Team("Lech", "Poznań", 49);
            var team6 = new Team("Łks", "Łódź", 24);
            teamRepository.saveAll(List.of(team1, team2, team3, team4, team5, team6));

            matchRepository.save(new Match(team1, team2, ref1));
            matchRepository.save(new Match(team3, team4, ref2));
            matchRepository.save(new Match(team5, team6, ref3));
            matchRepository.save(new Match(team1, team3, ref2));
            matchRepository.save(new Match(team2, team5, ref3));
            matchRepository.save(new Match(team3, team6, ref1));

            refereeRepository.findAll().forEach(System.out::println);
            gradeRepository.findAll().forEach(System.out::println);
            teamRepository.findAll().forEach(System.out::println);
            matchRepository.findAll().forEach(System.out::println);
        };
    }
}
