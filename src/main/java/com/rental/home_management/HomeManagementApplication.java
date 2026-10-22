package com.rental.home_management;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class HomeManagementApplication {

  public static void main(String[] args) {
    SpringApplication.run(HomeManagementApplication.class, args);
  }
}
