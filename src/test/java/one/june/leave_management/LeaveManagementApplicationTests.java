package one.june.leave_management;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LeaveManagementApplicationTests {

	@Test
	void applicationClassExists() {
		// This test verifies that the main application class can be instantiated
		assertDoesNotThrow(() -> {
			new LeaveManagementApplication();
		});
	}

}
