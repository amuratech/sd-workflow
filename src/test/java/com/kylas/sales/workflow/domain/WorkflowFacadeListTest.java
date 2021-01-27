package com.kylas.sales.workflow.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.user.UserFacade;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.security.AuthService;
import com.kylas.sales.workflow.stubs.UserStub;
import javax.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
class WorkflowFacadeListTest {

  @MockBean
  AuthService authService;
  @MockBean
  UserService userService;
  @Autowired
  UserFacade userFacade;
  @Autowired
  WorkflowFacade workflowFacade;

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void givenUserWithReadAll_shouldGetAllWorkflowsByPage() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, false, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    PageRequest pageable = PageRequest.of(0, 10);
    //when
    Page<Workflow> pageResponse = workflowFacade.list(pageable);
    //then
    assertThat(pageResponse.getTotalElements()).isEqualTo(3);
    assertThat(pageResponse.getTotalPages()).isEqualTo(1);
    assertThat(pageResponse.getContent().size()).isEqualTo(3);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void givenUserWithReadPermission_shouldGetHisWorkflowByPage() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, false, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    PageRequest pageable = PageRequest.of(0, 10);
    //when
    Page<Workflow> pageResponse = workflowFacade.list(pageable);
    //then
    assertThat(pageResponse.getTotalElements()).isEqualTo(1);
    assertThat(pageResponse.getTotalPages()).isEqualTo(1);
    assertThat(pageResponse.getContent().size()).isEqualTo(1);
    assertThat(pageResponse.getContent().get(0).getId()).isEqualTo(301);
    assertThat(pageResponse.getContent().get(0).getAllowedActions().canRead()).isTrue();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void givenUserWithoutReadAndReadAll_tryToGetWorkflowList_shouldThrow() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, false, false, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    PageRequest pageable = PageRequest.of(0, 10);
    //when
    //then
    Assertions.assertThatThrownBy(() -> workflowFacade.list(pageable))
        .isInstanceOf(InsufficientPrivilegeException.class)
        .hasMessage("01702001");
  }
}