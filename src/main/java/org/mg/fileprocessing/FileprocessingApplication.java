package org.mg.fileprocessing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FileprocessingApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileprocessingApplication.class, args);
	}

}
