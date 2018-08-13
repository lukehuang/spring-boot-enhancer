package org.springframework.boot.enhancer;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
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

	@Autowired
	private ObjectMapper mapper;

	@GetMapping("/")
	public String home() {
		return "Hello";
	}

	@Bean
	public CommandLineRunner runner() {
		return args -> {
			if (this instanceof MetadataProvider) {
				String json = mapper
						.writeValueAsString(((MetadataProvider) this).initialize());
				System.err.println("Initializing.." + json);
				AnnotationMetadata metadata = mapper.readValue(json,
						NonStandardAnnotationMetadata.class);
				System.err.println("Initialized.." + metadata);
			}
		};
	}

	public static void main(String[] args) {
		new SpringApplicationBuilder(TestApplication.class).run(args);
	}
}
