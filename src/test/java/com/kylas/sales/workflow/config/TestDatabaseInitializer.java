package com.kylas.sales.workflow.config;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;

@TestConfiguration
@EnableJpaRepositories
public class TestDatabaseInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext>, DisposableBean {

  @Container
  public static PostgreSQLContainer postgreSQLContainer =
      new PostgreSQLContainer("postgres:10.11")
          .withDatabaseName("workflow")
          .withUsername("test-user")
          .withPassword("test-password");

  @Override
  public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
    postgreSQLContainer.start();

    TestPropertyValues.of(
            "spring.datasource.url=" + postgreSQLContainer.getJdbcUrl(),
            "spring.datasource.username=" + postgreSQLContainer.getUsername(),
            "spring.datasource.password=" + postgreSQLContainer.getPassword())
        .applyTo(configurableApplicationContext.getEnvironment());
  }

  @Override
  public void destroy() {
    if (postgreSQLContainer != null && postgreSQLContainer.isRunning()) {
      postgreSQLContainer.stop();
    }
  }
}
