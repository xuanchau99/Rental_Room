package com.rental.rental_room;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RentalRoomApplication {

  public static void main(String[] args) {
    SpringApplication.run(RentalRoomApplication.class, args);
  }
}
