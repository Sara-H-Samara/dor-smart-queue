package com.sara.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Application start")
class BackendApplicationTests {

	@Test
	@DisplayName("The application starts with the test configuration")
	void contextLoads() {
	}

}
