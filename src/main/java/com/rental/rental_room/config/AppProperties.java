package com.rental.rental_room.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Images images, Bank bank, Pdf pdf) {
  public AppProperties {
    jwt = jwt == null ? new Jwt("") : jwt;
    images = images == null ? new Images("./data/meter-images") : images;
    bank = bank == null ? new Bank("", "", "") : bank;
    pdf = pdf == null ? new Pdf("") : pdf;
  }

  public record Jwt(String secret) {}
  public record Images(String root) {}
  public record Bank(String bin, String account, String name) {}
  public record Pdf(String font) {}
}