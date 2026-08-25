package com.example.efficientia;

import com.example.efficientia.config.DotenvPropertyLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Path;

@SpringBootApplication
public class EfficientiaApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(EfficientiaApplication.class);
		application.setDefaultProperties(DotenvPropertyLoader.load(Path.of(".env")));
		application.run(args);
	}

}
