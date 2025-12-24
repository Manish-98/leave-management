package one.june.leave_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = "one.june.leave_management")
@EnableAsync
public class LeaveManagementApplication {

	public static void main(String[] args) {
		SpringApplication.run(LeaveManagementApplication.class, args);
	}

}
