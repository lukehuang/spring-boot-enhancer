package org.springframework.boot.enhancer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class TestApplication {

	@GetMapping("/")
	public String home() {
		return "Hello";
	}

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			if (this instanceof MetadataProvider) {
				AnnotationMetadata metadata = ((MetadataProvider) this).initialize();
				System.err.println("Initialized.." + metadata);
			}
		};
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(TestApplication.class).run(args);
	}
}
