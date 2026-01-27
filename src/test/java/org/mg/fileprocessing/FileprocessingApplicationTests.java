package org.mg.fileprocessing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@EmbeddedKafka
@ActiveProfiles("test")
class FileprocessingApplicationTests {
	@Test
	void contextLoads() {
	}

}
