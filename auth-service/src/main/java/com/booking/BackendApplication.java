package com.booking;

import com.booking.infrastructure.config.AppProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
@RequiredArgsConstructor
public class BackendApplication {

	private final AppProperties appProperties;

	@PostConstruct
	public void init() {
		TimeZone.setDefault(
				TimeZone.getTimeZone(appProperties.getTimezone())
		);
	}

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
	}
}