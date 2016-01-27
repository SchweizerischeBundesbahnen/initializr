package ch.sbb.esta.initializr.starter;

import io.spring.initializr.web.LegacyStsController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(value = {"ch.sbb.esta", "io.spring.initializr"})
public class EstaStarterApp {

	public static void main(String[] args) {
		SpringApplication.run(EstaStarterApp.class, args);
	}

	@Bean
	@SuppressWarnings("deprecation")
	public LegacyStsController legacyStsController() {
		return new LegacyStsController();
	}

}
