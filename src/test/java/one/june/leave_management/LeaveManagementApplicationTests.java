package one.june.leave_management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LeaveManagementApplicationTests {

	@Test
	void contextLoads() {
		// This test verifies that the Spring context loads successfully
		// with the embedded PostgreSQL database
	}

	@Test
	void applicationStartsSuccessfully() {
		// This test verifies that the Spring context loads successfully
		// with the embedded PostgreSQL database
	}

}
