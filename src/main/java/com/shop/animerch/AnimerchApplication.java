package com.shop.animerch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AnimerchApplication {
	public static void main(String[] args) {
		SpringApplication.run(AnimerchApplication.class, args);
	}
}