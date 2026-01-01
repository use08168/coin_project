package com.team_biance.the_coin_killer;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TheCoinKillerApplication {

	public static void main(String[] args) {
		Dotenv dotenv = Dotenv.configure()
				.ignoreIfMissing()
				.load();

		dotenv.entries().forEach(e -> {
			// OS 환경변수가 이미 있으면 덮어쓰지 않음
			if (System.getProperty(e.getKey()) == null && System.getenv(e.getKey()) == null) {
				System.setProperty(e.getKey(), e.getValue());
			}
		});

		SpringApplication.run(TheCoinKillerApplication.class, args);
	}
}
