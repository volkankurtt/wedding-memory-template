package com.dugunanisi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DugunAnisiApplication {

	public static void main(String[] args) {
		// Render + Java often stalls ~connect-timeout on an unused IPv6 route to supabase.co.
		System.setProperty("java.net.preferIPv4Stack", "true");
		SpringApplication.run(DugunAnisiApplication.class, args);
	}

}
