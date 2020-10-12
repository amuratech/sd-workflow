package com.kylas.sales.workflow.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
@Sql("/test-scripts/insert-users.sql")
class UserFacadeTest {

  @Autowired
  private UserRepository userRepository;
  private UserFacade userFacade;

  @BeforeEach
  public void initialise() {
    userFacade = new UserFacade(userRepository);
  }

  @Test
  public void givenExistingUser_shouldUpdateIt_andFetchIt() {
    // given
    var userId = 11L;
    var tenantId = 99L;

    // when
    var user =
        userFacade.getExistingOrCreateNewUser(
            new User(userId, tenantId, null).withName("Iron Man"), tenantId);

    // then
    assertThat(user.getId()).isEqualTo(userId);
    assertThat(user.getTenantId()).isEqualTo(tenantId);
    assertThat(user.getName()).isEqualTo("Iron Man");
  }

  @Test
  public void givenNewUser_shouldCreateIt_andReturn() {
    // given
    var userId = 16L;
    var tenantId = 11L;

    // when
    var user =
        userFacade.getExistingOrCreateNewUser(
            new User(userId, tenantId, null).withName("Nick Furry"), tenantId);

    // then
    assertThat(user.getId()).isEqualTo(userId);
    assertThat(user.getTenantId()).isEqualTo(tenantId);
    assertThat(user.getName()).isEqualTo("Nick Furry");
  }

  @Test
  public void givenExistingUser_shouldReturnItsDetails() {
    // given
    var userId = 15L;
    var tenantId = 75L;

    // when
    var optionalUser = userFacade.tryGetUserByIdAndTenantId(userId, tenantId);

    // then
    var user = optionalUser.get();
    assertThat(user.getId()).isEqualTo(userId);
    assertThat(user.getTenantId()).isEqualTo(tenantId);
    assertThat(user.getName()).isEqualTo("user 23");
  }

  @Test
  public void givenNonExistingUser_shouldReturnItsEmpty() {
    // given
    var userId = 35L;
    var tenantId = 75L;

    // when
    var optionalUser = userFacade.tryGetUserByIdAndTenantId(userId, tenantId);

    // then
    assertThat(optionalUser).isNotPresent();
  }

  @Test
  public void givenExistingUser_shouldUpdateIt() {
    // given
    var userId = 12L;
    var tenantId = 99L;
    var firstName = "Steve";
    var lastName = "Rogers";

    // when
    var optionalUser = userFacade.tryUpdateUser(userId, tenantId, firstName, lastName);

    // then
    var user = optionalUser.get();
    assertThat(user.getId()).isEqualTo(userId);
    assertThat(user.getTenantId()).isEqualTo(tenantId);
    assertThat(user.getName()).isEqualTo("Steve Rogers");
  }

  @Test
  public void givenNonExistingUser_whenTryingToUpdate_shouldReturnEmpty() {
    // given
    var userId = 35L;
    var tenantId = 75L;
    var firstName = "Steve";
    var lastName = "Rogers";

    // when
    var optionalUser = userFacade.tryUpdateUser(userId, tenantId, firstName, lastName);

    // then
    assertThat(optionalUser).isEmpty();
  }
}