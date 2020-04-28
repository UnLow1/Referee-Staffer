package com.jamex.refereestaffer;

import com.jamex.refereestaffer.model.Referee;
import com.jamex.refereestaffer.repository.RefereeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.stream.Stream;

@SpringBootApplication
public class RefereeStafferApplication {

    public static void main(String[] args) {
        SpringApplication.run(RefereeStafferApplication.class, args);
    }

    @Bean
    CommandLineRunner init(RefereeRepository refereeRepository) {
        return args -> {
            Stream.of("John", "Julie", "Jennifer", "Helen", "Rachel").forEach(name -> {
                Referee user = new Referee(name, name.toLowerCase() + "@domain.com");
                refereeRepository.save(user);
            });
            refereeRepository.findAll().forEach(System.out::println);
        };
    }

}
