package com.rental.home_management.config;

import com.zaxxer.hikari.HikariDataSource;
import java.net.URI;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

  @Bean
  @Primary
  @ConfigurationProperties("spring.datasource")
  public DataSourceProperties dataSourceProperties() {
    return new DataSourceProperties();
  }

  @Bean
  @Primary
  public DataSource dataSource(DataSourceProperties properties) {
    String url = properties.getUrl();
    String username = properties.getUsername();
    String password = properties.getPassword();

    String databaseUrl = System.getenv("DATABASE_URL");
    if ((url == null || url.isBlank() || url.contains("localhost"))
        && databaseUrl != null
        && !databaseUrl.isBlank()) {
      try {
        URI uri = new URI(databaseUrl);
        String host = uri.getHost();
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        String path = uri.getPath();
        if (path != null && path.startsWith("/")) {
          path = path.substring(1);
        }
        url = "jdbc:postgresql://" + host + ":" + port + "/" + path + "?sslmode=require";
        if (uri.getUserInfo() != null) {
          String[] userInfo = uri.getUserInfo().split(":", 2);
          username = userInfo[0];
          if (userInfo.length > 1) {
            password = userInfo[1];
          }
        }
      } catch (Exception ignored) {
      }
    }

    if (url != null) {
      url = url.replaceAll("\\s+", "");
      url =
          url.replace("&sslfactory=org.postgresql.ssl.NonValidatingFactory", "")
              .replace("?sslfactory=org.postgresql.ssl.NonValidatingFactory&", "?")
              .replace("?sslfactory=org.postgresql.ssl.NonValidatingFactory", "");
      if (!url.contains("localhost") && !url.contains(":postgres:") && !url.contains("sslmode=")) {
        url += (url.contains("?") ? "&" : "?") + "sslmode=require";
      }
    }

    var dataSource = properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    if (url != null) {
      dataSource.setJdbcUrl(url);
    }
    if (username != null) {
      dataSource.setUsername(username.trim());
    }
    if (password != null) {
      dataSource.setPassword(password.trim());
    }
    return dataSource;
  }
}
