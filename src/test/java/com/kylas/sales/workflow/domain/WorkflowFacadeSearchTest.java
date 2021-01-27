package com.kylas.sales.workflow.domain;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.InvalidFilterException;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.user.UserFacade;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.security.AuthService;
import com.kylas.sales.workflow.stubs.UserStub;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import javax.transaction.Transactional;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
class WorkflowFacadeSearchTest {

  @MockBean AuthService authService;
  @MockBean UserService userService;
  @Autowired UserFacade userFacade;
  @Autowired WorkflowFacade workflowFacade;

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void givenUserWithReadAll_shouldGetAllWorkflowsByPage() {
    // given
    long tenantId = 99L;
    long userId = 12L;
    User aUser =
        UserStub.aUser(userId, tenantId, false, false, true, false, false).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    PageRequest pageable = PageRequest.of(0, 10);
    // when
    Page<Workflow> pageResponse = workflowFacade.search(pageable, Optional.empty());
    // then
    assertThat(pageResponse.getTotalElements()).isEqualTo(3);
    assertThat(pageResponse.getTotalPages()).isEqualTo(1);
    assertThat(pageResponse.getContent().size()).isEqualTo(3);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void givenUserWithReadPermission_shouldGetHisWorkflowByPage() {
    // given
    long tenantId = 99L;
    long userId = 12L;
    User aUser =
        UserStub.aUser(userId, tenantId, false, true, false, false, false).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    PageRequest pageable = PageRequest.of(0, 10);
    // when
    Page<Workflow> pageResponse = workflowFacade.search(pageable, Optional.empty());
    // then
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
    // given
    long tenantId = 99L;
    long userId = 12L;
    User aUser =
        UserStub.aUser(userId, tenantId, false, false, false, false, false).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    PageRequest pageable = PageRequest.of(0, 10);
    // when
    // then
    Assertions.assertThatThrownBy(() -> workflowFacade.search(pageable, Optional.empty()))
        .isInstanceOf(InsufficientPrivilegeException.class)
        .hasMessage("01702001");
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void givenUserWithReadAllPermission_tryToSearchWithSortOnLastTriggeredAtDesc_shouldGet() {
    // given
    long tenantId = 99L;
    long userId = 12L;
    User aUser =
        UserStub.aUser(userId, tenantId, false, true, true, false, false).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    Sort sortByLastTriggeredAt = Sort.by(Order.desc("workflowExecutedEvent.lastTriggeredAt"));
    PageRequest pageable = PageRequest.of(0, 10, sortByLastTriggeredAt);
    // when
    Page<Workflow> pageResponse = workflowFacade.search(pageable, Optional.empty());
    // then
    assertThat(pageResponse.getTotalElements()).isEqualTo(3);
    assertThat(
            pageResponse.getContent().stream().map(workflow -> workflow.getId()).collect(toList()))
        .containsExactly(303L, 302L, 301L);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void givenUserWithReadAllPermission_tryToSearchWithSortOnLastTriggeredAtAsc_shouldGet() {
    // given
    long tenantId = 99L;
    long userId = 12L;
    User aUser =
        UserStub.aUser(userId, tenantId, false, true, true, false, false).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    Sort sortByLastTriggeredAt = Sort.by(Order.asc("workflowExecutedEvent.lastTriggeredAt"));
    PageRequest pageable = PageRequest.of(0, 10, sortByLastTriggeredAt);
    // when
    Page<Workflow> pageResponse = workflowFacade.search(pageable, Optional.empty());
    // then
    assertThat(pageResponse.getTotalElements()).isEqualTo(3);
    assertThat(
            pageResponse.getContent().stream().map(workflow -> workflow.getId()).collect(toList()))
        .containsExactly(301L, 302L, 303L);
  }

  @Transactional
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users-for-search.sql")
  public void givenFilter_tryToSearchOnStatus_shouldGet() {
    // given
    long tenantId = 99L;
    long userId = 12L;
    User aUser =
        UserStub.aUser(userId, tenantId, false, true, true, false, false).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    Sort sortByLastTriggeredAt = Sort.by(Order.asc("updatedAt"));
    PageRequest pageable = PageRequest.of(0, 10, sortByLastTriggeredAt);
    Set<WorkflowFilter> workflowFilters = new HashSet<>();
    workflowFilters.add(new WorkflowFilter("equal", "active", "boolean", true));
    // when
    Page<Workflow> searchResponse = workflowFacade.search(pageable, Optional.of(workflowFilters));
    // then
    assertThat(searchResponse.getTotalElements()).isEqualTo(2);
    assertThat(
            searchResponse.getContent().stream()
                .map(workflow -> workflow.getId())
                .collect(toList()))
        .containsExactly(301L, 303L);
  }

  @Transactional
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users-for-search.sql")
  @Test
  public void givenFilter_tryToSearchOnEntityType_shouldGet() {
    // given
    long tenantId = 99L;
    long userId = 12L;
    User aUser =
        UserStub.aUser(userId, tenantId, false, true, true, false, false).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    Sort sortByLastTriggeredAt = Sort.by(Order.asc("updatedAt"));
    PageRequest pageable = PageRequest.of(0, 10, sortByLastTriggeredAt);
    Set<WorkflowFilter> workflowFilters = new HashSet<>();
    workflowFilters.add(new WorkflowFilter("equal", "entityType", "string", "lead"));
    // when
    Page<Workflow> searchResponse = workflowFacade.search(pageable, Optional.of(workflowFilters));
    // then
    assertThat(searchResponse.getTotalElements()).isEqualTo(3);
    assertThat(
            searchResponse.getContent().stream()
                .map(workflow -> workflow.getId())
                .collect(toList()))
        .containsExactly(301L, 302L, 303L);
  }

  @Transactional
  @ParameterizedTest(name = "Operator \"{0}\" FilterValue {1}")
  @MethodSource("createdAtOperatorAndExpectedValues")
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users-for-search.sql")
  public void givenEachOperator_tryToSearchOnCreatedAt_shouldGetResult(
      String operator, Object fieldValue, List<Long> expectedIds) {
    // given
    long tenantId = 99L;
    long userId = 12L;
    User aUser =
        UserStub.aUser(userId, tenantId, false, true, true, false, false).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    Sort sortByLastTriggeredAt = Sort.by(Order.asc("updatedAt"));
    PageRequest pageable = PageRequest.of(0, 10, sortByLastTriggeredAt);
    Set<WorkflowFilter> workflowFilters = new HashSet<>();
    workflowFilters.add(new WorkflowFilter(operator, "createdAt", "date", fieldValue));
    // when
    Page<Workflow> searchResponse = workflowFacade.search(pageable, Optional.of(workflowFilters));
    // then
    assertThat(
            searchResponse.getContent().stream()
                .map(workflow -> workflow.getId())
                .collect(toList()))
        .containsExactly(expectedIds.toArray(new Long[expectedIds.size()]));
  }

  @Transactional
  @ParameterizedTest(name = "Operator \"{0}\" FilterValue {1}")
  @MethodSource("createdByOperatorAndExpectedValues")
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users-for-search.sql")
  public void givenEachOperator_tryToSearchOnCreatedBy_shouldGetResult(
      String operator, Object fieldValue, List<Long> expectedIds) {
    // given
    long tenantId = 99L;
    long userId = 12L;
    User aUser =
        UserStub.aUser(userId, tenantId, false, true, true, false, false).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    Sort sortByLastTriggeredAt = Sort.by(Order.asc("updatedAt"));
    PageRequest pageable = PageRequest.of(0, 10, sortByLastTriggeredAt);
    Set<WorkflowFilter> workflowFilters = new HashSet<>();
    workflowFilters.add(new WorkflowFilter(operator, "createdBy", "long", fieldValue));
    // when
    Page<Workflow> searchResponse = workflowFacade.search(pageable, Optional.of(workflowFilters));
    // then
    assertThat(
            searchResponse.getContent().stream()
                .map(workflow -> workflow.getId())
                .collect(toList()))
        .containsExactly(expectedIds.toArray(new Long[expectedIds.size()]));
  }

  private static Stream<Arguments> createdAtOperatorAndExpectedValues() {
    return Stream.of(
        Arguments.of("greater", "2020-10-23T10:53:58.250z", Arrays.asList(303L)),
        Arguments.of("greater_or_equal", "2020-10-23T10:53:58.250z", Arrays.asList(302L, 303L)),
        Arguments.of("less", "2020-10-23T10:53:58.250z", Arrays.asList(301L)),
        Arguments.of("less_or_equal", "2020-10-23T10:53:58.250z", Arrays.asList(301L, 302L)),
        Arguments.of("is_not_null", "2020-10-23T10:53:58.250z", Arrays.asList(301L, 302L, 303L)),
        Arguments.of("is_null", "2020-10-23T10:53:58.250z", Collections.emptyList()),
        Arguments.of(
            "between",
            List.of("2020-10-21T10:53:58.250z", "2020-10-24T10:53:58.250z"),
            Arrays.asList(301L, 302L)),
        Arguments.of(
            "not_between",
            List.of("2020-10-23T10:53:58.250z", "2020-10-24T10:53:58.250z"),
            Arrays.asList(301L, 303L)));
  }

  private static Stream<Arguments> createdByOperatorAndExpectedValues() {
    return Stream.of(
        Arguments.of("equal", 12, Arrays.asList(301L)),
        Arguments.of("not_equal", 12, Arrays.asList(302L, 303L)),
        Arguments.of("is_not_null", "", Arrays.asList(301L, 302L, 303L)),
        Arguments.of("is_null", "", Collections.emptyList()));
  }
}
