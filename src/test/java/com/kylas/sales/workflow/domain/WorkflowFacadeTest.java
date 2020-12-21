package com.kylas.sales.workflow.domain;

import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.PLAIN;
import static com.kylas.sales.workflow.domain.workflow.TriggerFrequency.CREATED;
import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.EDIT_PROPERTY;
import static com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType.WEBHOOK;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

import com.kylas.sales.workflow.common.dto.ActionDetail;
import com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction;
import com.kylas.sales.workflow.common.dto.ActionDetail.WebhookAction.AuthorizationType;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.common.dto.WorkflowCondition.ConditionExpression;
import com.kylas.sales.workflow.common.dto.WorkflowCondition.Operator;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.exception.InsufficientPrivilegeException;
import com.kylas.sales.workflow.domain.exception.InvalidActionException;
import com.kylas.sales.workflow.domain.exception.WorkflowNotFoundException;
import com.kylas.sales.workflow.domain.service.UserService;
import com.kylas.sales.workflow.domain.user.User;
import com.kylas.sales.workflow.domain.workflow.ConditionType;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.TriggerFrequency;
import com.kylas.sales.workflow.domain.workflow.TriggerType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.WorkflowCondition;
import com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction;
import com.kylas.sales.workflow.domain.workflow.action.webhook.CryptoService;
import com.kylas.sales.workflow.domain.workflow.action.webhook.Parameter;
import com.kylas.sales.workflow.domain.workflow.action.webhook.attribute.AttributeFactory.WebhookEntity;
import com.kylas.sales.workflow.security.AuthService;
import com.kylas.sales.workflow.stubs.UserStub;
import com.kylas.sales.workflow.stubs.WorkflowStub;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
class WorkflowFacadeTest {

  @MockBean
  AuthService authService;
  @MockBean
  UserService userService;
  @Autowired
  WorkflowFacade workflowFacade;
  @Autowired
  CryptoService cryptoService;

  @Transactional
  @Test
  @Sql("/test-scripts/insert-users.sql")
  public void givenWorkflowRequest_shouldCreateIt() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(
            Mono.just(
                aUser));
    var workflowRequest = WorkflowStub
        .anEditPropertyWorkflowRequest("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, EDIT_PROPERTY, "lastName", "Stark", PLAIN, true);
    //when
    var workflowMono = workflowFacade.create(workflowRequest);
    //then
    StepVerifier.create(workflowMono)
        .expectNextMatches(workflow -> {
          assertThat(workflow.getId())
              .isNotNull()
              .isGreaterThan(0L);

          assertThat(workflow.getWorkflowTrigger().getTriggerFrequency()).isEqualTo(CREATED);
          assertThat(workflow.getWorkflowTrigger().getTriggerType()).isEqualTo(TriggerType.EVENT);

          assertThat(workflow.getWorkflowActions().size()).isEqualTo(1);
          EditPropertyAction workflowAction = (EditPropertyAction) workflow.getWorkflowActions().iterator().next();
          assertThat(workflowAction.getName()).isEqualTo("lastName");
          assertThat(workflowAction.getValue()).isEqualTo("Stark");

          assertThat(workflow.getWorkflowCondition().getType()).isEqualTo(ConditionType.FOR_ALL);

          assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNull();
          assertThat(workflow.getWorkflowExecutedEvent().getId()).isGreaterThan(0);
          assertThat(workflow.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(0);

          assertThat(workflow.isActive()).isTrue();
          return true;
        })
        .verifyComplete();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-users.sql")
  public void givenWorkflowRequest_withConditionExpression_shouldCreateIt() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(Mono.just(aUser));
    var action = new ActionResponse(EDIT_PROPERTY, new ActionDetail.EditPropertyAction("firstName", "Tony", PLAIN));

    ConditionExpression expression = new ConditionExpression(Operator.EQUAL, "firstName", "Tony");

    var workflowRequest = WorkflowStub.anConditionBasedEditPropertyWorkflowRequest(
        "SomeRandomName", "desc", EntityType.LEAD, expression, Set.of(action));
    //when
    var workflowMono = workflowFacade.create(workflowRequest);
    //then
    StepVerifier.create(workflowMono)
        .expectNextMatches(workflow -> {
          assertThat(workflow.getId())
              .isNotNull()
              .isGreaterThan(0L);

          WorkflowCondition condition = workflow.getWorkflowCondition();
          assertThat(condition.getType()).isEqualTo(ConditionType.CONDITION_BASED);
          assertThat(condition.getExpression()).isNotNull();
          assertThat(condition.getExpression().getOperator()).isEqualTo(Operator.EQUAL);
          assertThat(condition.getExpression().getName()).isEqualTo("firstName");
          assertThat(condition.getExpression().getValue()).isEqualTo("Tony");
          return true;
        })
        .verifyComplete();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-users.sql")
  public void givenWorkflowRequest_withMultipleActions_shouldCreateIt() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(Mono.just(aUser));

    List<Parameter> parameters = List.of(new Parameter("paramKey", WebhookEntity.LEAD, "firstName"));

    String requestUrl = "https://webhook.site/3e0d9676-ad3c-4cf2-a449-ca334e43b815";
    Set<ActionResponse> actions = Set.of(
        new ActionResponse(EDIT_PROPERTY,
            new ActionDetail.EditPropertyAction("city", "Pune", PLAIN)),
        new ActionResponse(WEBHOOK,
            new WebhookAction("name", "desc", HttpMethod.GET, requestUrl, AuthorizationType.NONE, parameters, null))
    );
    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, true, actions);
    //when
    var workflowMono = workflowFacade.create(workflowRequest);
    //then
    StepVerifier.create(workflowMono)
        .expectNextMatches(workflow -> {
          assertThat(workflow.getId())
              .isNotNull()
              .isGreaterThan(0L);
          assertThat(workflow.getWorkflowActions()).isNotEmpty().hasSize(2);
          assertThat(workflow.getWorkflowActions().stream().anyMatch(action -> action.getType().equals(EDIT_PROPERTY)))
              .isTrue();
          assertThat(workflow.getWorkflowActions().stream().anyMatch(action -> action.getType().equals(WEBHOOK)))
              .isTrue();
          return true;
        })
        .verifyComplete();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-users.sql")
  public void givenWorkflowRequest_withMalformedUrl_shouldThrow() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(Mono.just(aUser));

    String requestUrl = "some-malformed-url";
    Set<ActionResponse> actions = Set.of(
        new ActionResponse(WEBHOOK,
            new WebhookAction("name", "desc", HttpMethod.GET, requestUrl, AuthorizationType.NONE, Collections.emptyList(), null))
    );
    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, true, actions);
    //then
    assertThatExceptionOfType(InvalidActionException.class)
        .isThrownBy(() -> workflowFacade.create(workflowRequest).block());
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-users.sql")
  public void givenWorkflowRequest_withInvalidPropertyValue_shouldThrow() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(Mono.just(aUser));
    Set<ActionResponse> actions = Set.of(
        new ActionResponse(EDIT_PROPERTY, new ActionDetail.EditPropertyAction("city", null, PLAIN)));

    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, true, actions);
    //then
    assertThatExceptionOfType(InvalidActionException.class)
        .isThrownBy(() -> workflowFacade.create(workflowRequest).block());
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-users.sql")
  public void givenWorkflowRequest_withDuplicateWebhookParamKey_shouldThrow() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(Mono.just(aUser));

    String requestUrl = "https://webhook.site/3e0d9676-ad3c-4cf2-a449-ca334e43b815";
    List<Parameter> params = List.of(
        new Parameter("key", WebhookEntity.CUSTOM, "some-value"),
        new Parameter("key", WebhookEntity.CUSTOM, "some-different-value"));

    Set<ActionResponse> actions = Set.of(
        new ActionResponse(WEBHOOK,
            new WebhookAction("name", "desc", HttpMethod.GET, requestUrl, AuthorizationType.NONE, params, null)));

    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, true, actions);
    //then
    assertThatExceptionOfType(InvalidActionException.class)
        .isThrownBy(() -> workflowFacade.create(workflowRequest).block());
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-workflow.sql")
  public void givenWorkflowUpdateRequest_shouldUpdateIt() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(
            Mono.just(
                aUser));
    var workflowRequest = WorkflowStub
        .anEditPropertyWorkflowRequest("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, EDIT_PROPERTY, "lastName", "Stark", PLAIN, true);
    //when
    var workflowMono = workflowFacade.update(301L, workflowRequest);
    //then
    StepVerifier.create(workflowMono)
        .expectNextMatches(workflow -> {
          assertThat(workflow.getId())
              .isNotNull()
              .isGreaterThan(0L);

          assertThat(workflow.getWorkflowTrigger().getTriggerFrequency()).isEqualTo(CREATED);
          assertThat(workflow.getWorkflowTrigger().getTriggerType()).isEqualTo(TriggerType.EVENT);

          assertThat(workflow.getWorkflowActions().size()).isEqualTo(1);
          EditPropertyAction workflowAction = (EditPropertyAction) workflow.getWorkflowActions().iterator().next();
          assertThat(workflowAction.getName()).isEqualTo("lastName");
          assertThat(workflowAction.getValue()).isEqualTo("Stark");

          assertThat(workflow.getWorkflowCondition().getType()).isEqualTo(ConditionType.FOR_ALL);

          assertThat(workflow.getWorkflowExecutedEvent().getLastTriggeredAt()).isNull();
          assertThat(workflow.getWorkflowExecutedEvent().getId()).isGreaterThan(0);

          assertThat(workflow.isActive()).isTrue();
          return true;
        })
        .verifyComplete();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/create-webhook-workflow.sql")
  public void givenWorkflowUpdateRequest_updatingWebhook_shouldUpdateIt() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(Mono.just(aUser));

    String requestUrl = "https://webhook.site/3e0d9676-ad3c-4cf2-a449-ca334e43b815";
    var actionRequest = new ActionResponse(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"), WEBHOOK,
        new ActionDetail.WebhookAction("UpdatedName", "UpdatedDescription", HttpMethod.GET, requestUrl, AuthorizationType.NONE,
            Collections.emptyList(), "someAuthParameter"));
    Set<ActionResponse> actions = Set.of(actionRequest);

    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("UpdatedWorkflowName", "UpdatedWorkflowProperty", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, actions);
    //when
    var updatedWorkflow = workflowFacade.update(301L, workflowRequest);
    //then
    StepVerifier.create(updatedWorkflow)
        .expectNextMatches(workflow -> {
          assertThat(workflow.getId())
              .isNotNull()
              .isGreaterThan(0L);

          assertThat(workflow.getWorkflowTrigger().getTriggerFrequency()).isEqualTo(CREATED);
          assertThat(workflow.getWorkflowTrigger().getTriggerType()).isEqualTo(TriggerType.EVENT);

          assertThat(workflow.getWorkflowActions().size()).isEqualTo(1);
          assertThat(workflow.getWorkflowActions()).hasSize(1);
          var webhookAction = workflow.getWorkflowActions().stream()
              .filter(action -> action.getType().equals(WEBHOOK))
              .findFirst();
          assertThat(webhookAction).isPresent();
          var actionDetail = (com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookAction) webhookAction.get();

          assertThat(actionDetail.getId()).isEqualTo(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"));
          assertThat(actionDetail.getName()).isEqualTo("UpdatedName");
          assertThat(actionDetail.getDescription()).isEqualTo("UpdatedDescription");
          return true;
        })
        .verifyComplete();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/create-webhook-workflow.sql")
  public void givenWorkflowUpdateRequest_withEmptyParam_shouldUpdateIt() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(Mono.just(aUser));

    String requestUrl = "https://webhook.site/3e0d9676-ad3c-4cf2-a449-ca334e43b815";
    var actionRequest = new ActionResponse(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"), WEBHOOK,
        new ActionDetail.WebhookAction("UpdatedName", "UpdatedDescription", HttpMethod.GET, requestUrl, AuthorizationType.NONE,
            null, "someAuthParameter"));
    Set<ActionResponse> actions = Set.of(actionRequest);

    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("UpdatedWorkflowName", "UpdatedWorkflowProperty", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, actions);
    //when
    var updatedWorkflow = workflowFacade.update(301L, workflowRequest);
    //then
    StepVerifier.create(updatedWorkflow)
        .expectNextMatches(workflow -> {
          assertThat(workflow.getId())
              .isNotNull()
              .isGreaterThan(0L);

          assertThat(workflow.getWorkflowActions().size()).isEqualTo(1);
          assertThat(workflow.getWorkflowActions()).hasSize(1);
          var webhookAction = workflow.getWorkflowActions().stream()
              .filter(action -> action.getType().equals(WEBHOOK))
              .findFirst();
          assertThat(webhookAction).isPresent();
          var actionDetail = (com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookAction) webhookAction.get();

          assertThat(actionDetail.getParameters()).isEmpty();
          assertThat(actionDetail.getId()).isEqualTo(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"));
          assertThat(actionDetail.getName()).isEqualTo("UpdatedName");
          assertThat(actionDetail.getDescription()).isEqualTo("UpdatedDescription");
          return true;
        })
        .verifyComplete();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/create-multiple-actions-workflow.sql")
  public void givenWorkflowUpdateRequest_onRemovingActions_shouldUpdateIt() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(Mono.just(aUser));

    String requestUrl = "https://webhook.site/3e0d9676-ad3c-4cf2-a449-ca334e43b815";
    var actionRequest = new ActionResponse(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"), WEBHOOK,
        new ActionDetail.WebhookAction("webhookName", "webhook desc", HttpMethod.GET, requestUrl, AuthorizationType.NONE,
            null, "someAuthParameter"));
    Set<ActionResponse> actions = Set.of(actionRequest);

    var workflowRequest = WorkflowStub
        .aWorkflowRequestWithActions("Workflow 1", "Workflow 1", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, actions);
    //when
    var updatedWorkflow = workflowFacade.update(301L, workflowRequest);
    //then
    StepVerifier.create(updatedWorkflow)
        .expectNextMatches(workflow -> {
          assertThat(workflow.getId())
              .isNotNull()
              .isGreaterThan(0L);

          assertThat(workflow.getWorkflowActions().size()).isEqualTo(1);
          assertThat(workflow.getWorkflowActions()).hasSize(1);
          var webhookAction = workflow.getWorkflowActions().stream()
              .filter(action -> action.getType().equals(WEBHOOK))
              .findFirst();
          assertThat(webhookAction).isPresent();
          var actionDetail = (com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookAction) webhookAction.get();

          assertThat(actionDetail.getParameters()).isEmpty();
          assertThat(actionDetail.getId()).isEqualTo(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a12"));
          assertThat(actionDetail.getName()).isEqualTo("webhookName");
          assertThat(actionDetail.getDescription()).isEqualTo("webhook desc");
          return true;
        })
        .verifyComplete();
    assertThat(workflowFacade.get(301L).getWorkflowActions()).hasSize(1);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-workflow.sql")
  public void givenWorkflowUpdateRequest_shouldUpdateExistingAction() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(
            Mono.just(
                aUser));
    var workflowRequest = WorkflowStub
        .anExistingEditPropertyWorkflowRequest("Edit Lead Property", "Edit Lead Property",
            EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, UUID.fromString("a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11"), EDIT_PROPERTY, "lastName", "Stark", PLAIN,
            true);
    //when
    var workflowMono = workflowFacade.update(301L, workflowRequest);
    //then
    StepVerifier.create(workflowMono)
        .expectNextMatches(workflow -> {
          assertThat(workflow.getId())
              .isNotNull()
              .isGreaterThan(0L);

          assertThat(workflow.getWorkflowTrigger().getTriggerFrequency()).isEqualTo(CREATED);
          assertThat(workflow.getWorkflowTrigger().getTriggerType()).isEqualTo(TriggerType.EVENT);

          assertThat(workflow.getWorkflowActions().size()).isEqualTo(1);
          EditPropertyAction workflowAction = (EditPropertyAction) workflow.getWorkflowActions().iterator().next();
          assertThat(workflowAction.getName()).isEqualTo("lastName");
          assertThat(workflowAction.getValue()).isEqualTo("Stark");
          assertThat(workflowAction.getId()).isEqualTo(UUID.fromString("a0eebc55-9c0b-4ef8-bb6d-6bb9bd380a11"));
          return true;
        })
        .verifyComplete();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-users.sql")
  public void givenDeactivatedWorkflowRequest_shouldCreateIt() {
    //given
    User aUser = UserStub.aUser(11L, 99L, true, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(
            Mono.just(
                aUser));
    var workflowRequest = WorkflowStub
        .anEditPropertyWorkflowRequest("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, EDIT_PROPERTY, "lastName", "Stark", PLAIN, false);
    //when
    var workflowMono = workflowFacade.create(workflowRequest);
    //then
    StepVerifier.create(workflowMono)
        .expectNextMatches(workflow -> {
          assertThat(workflow.getId())
              .isNotNull()
              .isGreaterThan(0L);
          assertThat(workflow.isActive()).isFalse();
          return true;
        })
        .verifyComplete();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-users.sql")
  public void givenUserWithoutCreatePermission_tryToCreate_shouldThrow() {
    //given
    User aUser = UserStub.aUser(11L, 99L, false, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    given(userService.getUserDetails(11L, authService.getAuthenticationToken()))
        .willReturn(
            Mono.just(
                aUser));
    var workflowRequest = WorkflowStub
        .anEditPropertyWorkflowRequest("Edit Lead Property", "Edit Lead Property", EntityType.LEAD, TriggerType.EVENT, CREATED,
            ConditionType.FOR_ALL, EDIT_PROPERTY, "lastName", "Stark", PLAIN, true);
    //when
    var workflowMono = workflowFacade.create(workflowRequest);
    //then
    StepVerifier.create(workflowMono)
        .verifyErrorMatches(throwable -> throwable instanceof InsufficientPrivilegeException);

  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-workflow.sql")
  public void givenTenantIdAndEntityType_shouldReturn() {
    //given
    long tenantId = 99L;
    User aUser = UserStub.aUser(12, tenantId, false, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    //when
    var workflows = workflowFacade.findActiveBy(tenantId, EntityType.LEAD, CREATED);
    //then
    assertThat(workflows.size()).isEqualTo(2);
    List<Long> workflowIds = workflows.stream().map(workflow -> workflow.getId()).collect(toList());

    assertThat(workflowIds).containsExactlyInAnyOrder(301L, 302L);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/activate-workflow.sql")
  public void givenTenantIdAndEntityType_shouldReturnActiveOnly() {
    //given
    long tenantId = 99L;
    User aUser =
        UserStub.aUser(12, tenantId, false, true, true, true, true).withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    //when
    var workflows = workflowFacade.findActiveBy(tenantId, EntityType.LEAD, CREATED);

    //then
    assertThat(workflows).hasSize(1);
    assertThat(workflows.stream().map(Workflow::getId).collect(toList()))
        .containsExactlyInAnyOrder(302L);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/activate-workflow.sql")
  public void givenWorkflowId_shouldActivate() {
    //given
    long workflowId = 301;
    long tenantId = 99L;
    User aUser = UserStub.aUser(12, tenantId, false, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    //when
    Workflow workflow = workflowFacade.activate(workflowId);
    //then
    assertThat(workflow.isActive()).isTrue();
    assertThat(workflow.getUpdatedAt()).isAfter("2019-01-01");
    assertThat(workflow.getId()).isEqualTo(301L);
  }

  @Test
  public void activatingWorkflow_withoutUpdatePermissions_shouldThrow() {
    //given
    long workflowId = 301;
    long tenantId = 99L;
    User aUser = UserStub.aUser(12, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    //then
    assertThatExceptionOfType(InsufficientPrivilegeException.class)
        .isThrownBy(() -> workflowFacade.activate(workflowId))
        .withMessage("01702001");
  }

  @Transactional
  @Test
  @Sql("/test-scripts/activate-workflow.sql")
  public void givenWorkflowId_shouldDeactivate() {
    //given
    long workflowId = 302;
    long tenantId = 99L;
    User aUser = UserStub.aUser(12, tenantId, false, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    //when
    Workflow workflow = workflowFacade.deactivate(workflowId);
    //then
    assertThat(workflow.isActive()).isFalse();
    assertThat(workflow.getUpdatedAt()).isAfter("2019-01-01");
    assertThat(workflow.getId()).isEqualTo(302L);
  }

  @Test
  public void deactivatingWorkflow_withoutUpdatePermissions_shouldThrow() {
    //given
    long workflowId = 301;
    long tenantId = 99L;
    User aUser = UserStub.aUser(12, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    //then
    assertThatExceptionOfType(InsufficientPrivilegeException.class)
        .isThrownBy(() -> workflowFacade.deactivate(workflowId))
        .withMessage("01702001");
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-create-lead-workflow.sql")
  public void givenWorkflowId_shouldGetIt() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    long workflowId = 301;
    //when
    var workflow = workflowFacade.get(workflowId);
    //then
    assertThat(workflow.getId()).isEqualTo(workflowId);
    assertThat(workflow.getAllowedActions().canRead()).isEqualTo(true);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-workflow-with-condition.sql")
  public void givenWorkflowId_havingConditionOnWorkflow_shouldGetIt() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    long workflowId = 301;
    //when
    var workflow = workflowFacade.get(workflowId);
    //then
    assertThat(workflow.getId()).isEqualTo(workflowId);
    assertThat(workflow.getAllowedActions().canRead()).isEqualTo(true);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/create-multiple-actions-workflow.sql")
  public void givenWorkflowId_havingMultipleActions_shouldGetIt() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    long workflowId = 301;
    //when
    var workflow = workflowFacade.get(workflowId);
    //then
    assertThat(workflow.getId()).isEqualTo(workflowId);
    assertThat(workflow.getAllowedActions().canRead()).isEqualTo(true);
    var editPropertyAction = (EditPropertyAction) workflow.getWorkflowActions().stream()
        .filter(action -> action.getType().equals(EDIT_PROPERTY))
        .findFirst().get();
    assertThat(editPropertyAction.getName()).isEqualTo("firstName");
    assertThat(editPropertyAction.getValue()).isEqualTo("Tony 301");

    var webhookAction = (com.kylas.sales.workflow.domain.workflow.action.webhook.WebhookAction) workflow
        .getWorkflowActions().stream()
        .filter(workflowAction -> workflowAction.getType().equals(WEBHOOK)).findFirst().get();
    assertThat(webhookAction.getName()).isEqualTo("webhookName");
    assertThat(webhookAction.getMethod()).isEqualTo(HttpMethod.GET);
    assertThat(webhookAction.getAuthorizationType()).isEqualTo(AuthorizationType.NONE);
    assertThat(webhookAction.getParameters())
        .isNotEmpty()
        .hasSize(1);
    assertThat(webhookAction.getParameters().stream()
        .anyMatch(parameter -> parameter.getName().equals("param1") && parameter.getAttribute().equals("firstName")))
        .isTrue();
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-create-lead-workflow.sql")
  public void givenNonExistingWorkflowId_tryToGet_shouldThrow() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    long workflowId = 999;
    //when
    Assertions.assertThatThrownBy(() -> workflowFacade.get(workflowId))
        .isInstanceOf(WorkflowNotFoundException.class)
        .hasMessage("01701005");
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-create-lead-workflow.sql")
  public void givenUserDoesNotHaveReadAndReadAllPermission_tryToGetWorkflow_shouldThrow() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, false, false, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    long workflowId = 301;
    //when
    //then
    Assertions.assertThatThrownBy(() -> workflowFacade.get(workflowId))
        .isInstanceOf(InsufficientPrivilegeException.class)
        .hasMessage("01702001");
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void givenUserHavingReadAllPermission_tryToGetOthersWorkflow_shouldGet() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    long workflowIdForUser13 = 302;
    //when
    Workflow workflow = workflowFacade.get(workflowIdForUser13);
    //then
    assertThat(workflow.getId()).isEqualTo(workflowIdForUser13);
    assertThat(workflow.getAllowedActions().canRead()).isEqualTo(true);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-lead-workflow-for-multiple-users.sql")
  public void shouldGetAllWorkflowAsPageableResponse() {
    //given
    long tenantId = 99L;
    long userId = 12L;
    User aUser = UserStub.aUser(userId, tenantId, false, true, true, false, false)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);

    PageRequest pageable = PageRequest.of(0, 10);
    //when
    Page<Workflow> pageResponse = workflowFacade.list(pageable);
    //then
    assertThat(pageResponse.getTotalElements()).isEqualTo(3);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-workflow.sql")
  public void givenWorkflow_tryToUpdateWorkflowExecutedEvent_shouldUpdate() {
    //given
    var expectedLastTriggeredAt = new Date();
    long tenantId = 99L;
    User aUser = UserStub.aUser(12, tenantId, false, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    Workflow persistedWorkflow = workflowFacade.get(301);
    //when
    workflowFacade.updateExecutedEvent(persistedWorkflow);
    //then
    Workflow updatedWorkflowExecutedEvent = workflowFacade.get(301);
    assertThat(updatedWorkflowExecutedEvent.getWorkflowExecutedEvent().getLastTriggeredAt()).isNotNull();
    assertThat(updatedWorkflowExecutedEvent.getWorkflowExecutedEvent().getTriggerCount()).isEqualTo(152);
  }

  @Transactional
  @Test
  @Sql("/test-scripts/insert-workflow.sql")
  public void givenTriggerFrequencyUpdated_shouldGetWorkflow() {
    //given
    long tenantId = 75L;
    User aUser = UserStub.aUser(15, tenantId, false, true, true, true, true)
        .withName("user 1");
    given(authService.getLoggedInUser()).willReturn(aUser);
    //when
    List<Workflow> workflows = workflowFacade.findActiveBy(tenantId, EntityType.LEAD, TriggerFrequency.UPDATED);
    //then

    assertThat(workflows.size()).isEqualTo(1);
    assertThat(workflows.get(0).getId()).isEqualTo(304);
  }

}