package org.springframework.boot.enhancer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TestApplication {

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			if (this instanceof Initializer) {
				System.err.println("Initializing..");
				((Initializer) this).initialize();
			}
		};
	}

	public static void main(String[] args) {
		SpringApplication.run(TestApplication.class, args);
	}
}
