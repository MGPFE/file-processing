package org.mg.fileprocessing;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@EmbeddedKafka
@ActiveProfiles("test")
class FileprocessingApplicationTests {
	@MockitoBean private ProxyManager<byte[]> proxyManager;

	@Test
	void contextLoads() {
	}

}
