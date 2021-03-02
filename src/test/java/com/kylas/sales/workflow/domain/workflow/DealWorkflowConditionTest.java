package com.kylas.sales.workflow.domain.workflow;

import static com.kylas.sales.workflow.api.request.Condition.TriggerType.NEW_VALUE;
import static com.kylas.sales.workflow.api.request.Condition.TriggerType.OLD_VALUE;
import static com.kylas.sales.workflow.common.dto.condition.Operator.BEGINS_WITH;
import static com.kylas.sales.workflow.common.dto.condition.Operator.BETWEEN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.CONTAINS;
import static com.kylas.sales.workflow.common.dto.condition.Operator.EQUAL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.GREATER;
import static com.kylas.sales.workflow.common.dto.condition.Operator.GREATER_OR_EQUAL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_EMPTY;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NOT_EMPTY;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NOT_NULL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.IS_NULL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.LESS;
import static com.kylas.sales.workflow.common.dto.condition.Operator.LESS_OR_EQUAL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_BETWEEN;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_CONTAINS;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_EQUAL;
import static com.kylas.sales.workflow.common.dto.condition.Operator.NOT_IN;
import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.common.dto.condition.WorkflowCondition.ConditionExpression;
import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import com.kylas.sales.workflow.domain.ConditionFacade;
import com.kylas.sales.workflow.domain.processor.deal.DealDetail;
import com.kylas.sales.workflow.domain.processor.deal.Money;
import com.kylas.sales.workflow.domain.processor.deal.Pipeline;
import com.kylas.sales.workflow.domain.processor.lead.IdName;
import com.kylas.sales.workflow.mq.event.DealEvent;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
public class DealWorkflowConditionTest {


  @Autowired
  private ConditionFacade conditionFacade;

  @Test
  public void givenEquals_onSameStrings_evaluatesTrue() {
    var expression = new ConditionExpression(EQUAL, "name", "test deal", NEW_VALUE);
    var oldValExpression = new ConditionExpression(EQUAL, "name", "test deal", OLD_VALUE);
    var entity = stubDealDetail();
    entity.setName("test deal");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
    assertThat(conditionFacade.satisfies(oldValExpression, entity))
        .isTrue();
  }

  @Test
  public void givenEquals_onDifferentStrings_evaluatesFalse() {
    var expression = new ConditionExpression(EQUAL, "name", "test deal", NEW_VALUE);
    var oldValExpression = new ConditionExpression(EQUAL, "name", "test deal", OLD_VALUE);
    var entity = stubDealDetail();
    entity.setName("test deal diff");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
    assertThat(conditionFacade.satisfies(oldValExpression, entity))
        .isFalse();
  }

  @Test
  public void givenNotEquals_onSameStrings_evaluatesFalse() {
    var expression = new ConditionExpression(NOT_EQUAL, "name", "test deal", NEW_VALUE);
    var oldValExpression = new ConditionExpression(NOT_EQUAL, "name", "test deal", OLD_VALUE);
    var entity = stubDealDetail();
    entity.setName("test deal");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
    assertThat(conditionFacade.satisfies(oldValExpression, entity))
        .isFalse();
  }

  @Test
  public void givenNotEquals_onDifferentStrings_evaluatesTrue() {
    var expression = new ConditionExpression(NOT_EQUAL, "name", "test deal", NEW_VALUE);
    var oldValExpression = new ConditionExpression(NOT_EQUAL, "name", "test deal", OLD_VALUE);
    var entity = stubDealDetail();
    entity.setName("test deal diff");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
    assertThat(conditionFacade.satisfies(oldValExpression, entity))
        .isTrue();
  }

  @Test
  public void givenIsNull_onString_evaluatesTrue() {
    var expression = new ConditionExpression(IS_NULL, "name", null, NEW_VALUE);
    var oldValExpression = new ConditionExpression(IS_NULL, "name", null, OLD_VALUE);
    var entity = stubDealDetail();
    entity.setName(null);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
    assertThat(conditionFacade.satisfies(oldValExpression, entity))
        .isTrue();
  }

  @Test
  public void givenIsNull_onString_evaluatesFalse() {
    var expression = new ConditionExpression(IS_NULL, "name", "not null", NEW_VALUE);
    var oldValExpression = new ConditionExpression(IS_NULL, "name", "not null", OLD_VALUE);
    var entity = stubDealDetail();
    entity.setName("not null");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
    assertThat(conditionFacade.satisfies(oldValExpression, entity))
        .isFalse();
  }

  @Test
  public void givenIsNotNull_onString_evaluatesTrue() {
    var expression = new ConditionExpression(IS_NOT_NULL, "name", "not null", NEW_VALUE);
    var oldValExpression = new ConditionExpression(IS_NOT_NULL, "name", "not null", OLD_VALUE);
    var entity = stubDealDetail();
    entity.setName("not null");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
    assertThat(conditionFacade.satisfies(oldValExpression, entity))
        .isTrue();
  }

  @Test
  public void givenIsNotNull_onString_evaluatesFalse() {
    var expression = new ConditionExpression(IS_NOT_NULL, "name", null, NEW_VALUE);
    var oldValExpression = new ConditionExpression(IS_NOT_NULL, "name", null, OLD_VALUE);
    var entity = stubDealDetail();
    entity.setName(null);

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
    assertThat(conditionFacade.satisfies(oldValExpression, entity))
        .isFalse();
  }

  @Test
  public void givenBeginsWith_onString_evaluatesTrue() {
    var expression = new ConditionExpression(BEGINS_WITH, "name", "new", NEW_VALUE);
    var oldValExpression = new ConditionExpression(BEGINS_WITH, "name", "new", OLD_VALUE);
    var entity = stubDealDetail();
    entity.setName("new deal");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isTrue();
    assertThat(conditionFacade.satisfies(oldValExpression, entity))
        .isTrue();
  }

  @Test
  public void givenBeginsWith_onString_evaluatesFalse() {
    var expression = new ConditionExpression(BEGINS_WITH, "name", "new", NEW_VALUE);
    var oldValExpression = new ConditionExpression(BEGINS_WITH, "name", "new", OLD_VALUE);
    var entity = stubDealDetail();
    entity.setName("deal");

    assertThat(conditionFacade.satisfies(expression, entity))
        .isFalse();
    assertThat(conditionFacade.satisfies(oldValExpression, entity))
        .isFalse();
  }

  @Nested
  @DisplayName("Condition tests for Deal IdName values")
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestDatabaseInitializer.class})
  public class IdNameConditionTest {

    @Test
    public void givenEquals_onSameOwnedByIds_evaluatesTrue() {
      Object ownedBy = new IdName(242L, "ownedBy");
      ConditionExpression expression = new ConditionExpression(EQUAL, "ownedBy", ownedBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "ownedBy", ownedBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setOwnedBy(new IdName(242L, "ownedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenEquals_onDifferentOwnedByIds_evaluatesFalse() {
      Object ownedBy = new IdName(242L, "ownedBy");
      ConditionExpression expression = new ConditionExpression(EQUAL, "ownedBy", ownedBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "ownedBy", ownedBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setOwnedBy(new IdName(243L, "ownedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onSameOwnedByIds_evaluatesFalse() {
      Object ownedBy = new IdName(242L, "ownedBy");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "ownedBy", ownedBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "ownedBy", ownedBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setOwnedBy(new IdName(242L, "ownedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onDifferentOwnedByIds_evaluatesTrue() {
      Object ownedBy = new IdName(242L, "ownedBy");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "ownedBy", ownedBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "ownedBy", ownedBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setOwnedBy(new IdName(243L, "ownedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenIsNull_onOwnedById_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "ownedBy", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "ownedBy", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setOwnedBy(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onOwnedById_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "ownedBy", new IdName(2L, "ownedBy"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "ownedBy", new IdName(2L, "ownedBy"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setOwnedBy(new IdName(2L, "ownedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenIsNotNull_onOwnedById_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "ownedBy", new IdName(2L, "ownedBy"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "ownedBy", new IdName(2L, "ownedBy"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setOwnedBy(new IdName(2L, "ownedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNotNull_onOwnedById_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "ownedBy", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "ownedBy", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setOwnedBy(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenEquals_onSameProductIds_evaluatesTrue() {
      Object product = new IdName(242L, "product");
      ConditionExpression expression = new ConditionExpression(EQUAL, "product", product, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "product", product, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setProduct(new IdName(242L, "product"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenEquals_onDifferentProductIds_evaluatesFalse() {
      Object product = new IdName(242L, "product");
      ConditionExpression expression = new ConditionExpression(EQUAL, "product", product, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "product", product, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setProduct(new IdName(243L, "product"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onSameProductIds_evaluatesFalse() {
      Object product = new IdName(242L, "product");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "product", product, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "product", product, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setProduct(new IdName(242L, "product"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onDifferentProductIds_evaluatesTrue() {
      Object product = new IdName(242L, "product");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "product", product, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "product", product, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setProduct(new IdName(243L, "product"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onProductId_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "product", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "product", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setProduct(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onProductId_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "product", new IdName(2L, "product"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "product", new IdName(2L, "product"), NEW_VALUE);
      var entity = stubDealDetail();
      entity.setProduct(new IdName(2L, "product"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenIsNotNull_onProductId_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "product", new IdName(2L, "product"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "product", new IdName(2L, "product"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setProduct(new IdName(2L, "product"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNotNull_onProductId_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "product", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "product", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setProduct(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenEquals_onSameCreatedByIds_evaluatesTrue() {
      Object createdBy = new IdName(242L, "createdBy");
      ConditionExpression expression = new ConditionExpression(EQUAL, "createdBy", createdBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "createdBy", createdBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCreatedBy(new IdName(242L, "createdBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenEquals_onDifferentCreatedByIds_evaluatesFalse() {
      Object createdBy = new IdName(242L, "createdBy");
      ConditionExpression expression = new ConditionExpression(EQUAL, "createdBy", createdBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "createdBy", createdBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCreatedBy(new IdName(243L, "createdBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onSameCreatedByIds_evaluatesFalse() {
      Object createdBy = new IdName(242L, "createdBy");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "createdBy", createdBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "createdBy", createdBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCreatedBy(new IdName(242L, "createdBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onDifferentCreatedByIds_evaluatesTrue() {
      Object createdBy = new IdName(242L, "createdBy");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "createdBy", createdBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "createdBy", createdBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCreatedBy(new IdName(243L, "createdBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onCreatedById_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "createdBy", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "createdBy", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCreatedBy(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onCreatedById_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "createdBy", new IdName(2L, "createdBy"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "createdBy", new IdName(2L, "createdBy"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCreatedBy(new IdName(2L, "createdBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenIsNotNull_onCreatedById_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "createdBy", new IdName(2L, "createdBy"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "createdBy", new IdName(2L, "createdBy"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCreatedBy(new IdName(2L, "createdBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNotNull_onCreatedById_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "createdBy", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "createdBy", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCreatedBy(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenEquals_onSameCompanyIds_evaluatesTrue() {
      Object company = new IdName(242L, "company");
      ConditionExpression expression = new ConditionExpression(EQUAL, "company", company, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "company", company, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCompany(new IdName(242L, "company"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenEquals_onDifferentCompanyIds_evaluatesFalse() {
      Object company = new IdName(242L, "company");
      ConditionExpression expression = new ConditionExpression(EQUAL, "company", company, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "company", company, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCompany(new IdName(243L, "company"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onSameCompanyIds_evaluatesFalse() {
      Object company = new IdName(242L, "company");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "company", company, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "company", company, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCompany(new IdName(242L, "company"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onDifferentCompanyIds_evaluatesTrue() {
      Object company = new IdName(242L, "company");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "company", company, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "company", company, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCompany(new IdName(243L, "company"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onCompanyId_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "company", null, NEW_VALUE);
      ConditionExpression oldValueExpression = new ConditionExpression(IS_NULL, "company", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCompany(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValueExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onCompanyId_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "company", new IdName(2L, "company"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "company", new IdName(2L, "company"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCompany(new IdName(2L, "company"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenIsNotNull_onCompanyId_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "company", new IdName(2L, "company"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "company", new IdName(2L, "company"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCompany(new IdName(2L, "company"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNotNull_onCompanyId_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "company", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "company", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setCompany(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenEquals_onSameUpdatedByIds_evaluatesTrue() {
      Object createdBy = new IdName(242L, "updatedBy");
      ConditionExpression expression = new ConditionExpression(EQUAL, "updatedBy", createdBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "updatedBy", createdBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setUpdatedBy(new IdName(242L, "updatedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenEquals_onDifferentUpdatedByIds_evaluatesFalse() {
      Object createdBy = new IdName(242L, "updatedBy");
      ConditionExpression expression = new ConditionExpression(EQUAL, "updatedBy", createdBy, NEW_VALUE);
      var entity = stubDealDetail();
      entity.setUpdatedBy(new IdName(243L, "updatedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onSameUpdatedByIds_evaluatesFalse() {
      Object createdBy = new IdName(242L, "updatedBy");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "updatedBy", createdBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "updatedBy", createdBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setUpdatedBy(new IdName(242L, "updatedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onDifferentUpdatedByIds_evaluatesTrue() {
      Object createdBy = new IdName(242L, "updatedBy");
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "updatedBy", createdBy, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "updatedBy", createdBy, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setUpdatedBy(new IdName(243L, "updatedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onUpdatedById_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "updatedBy", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "updatedBy", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setUpdatedBy(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onUpdatedById_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "updatedBy", new IdName(2L, "updatedBy"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "updatedBy", new IdName(2L, "updatedBy"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setUpdatedBy(new IdName(2L, "updatedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenIsNotNull_onUpdatedById_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "updatedBy", new IdName(2L, "updatedBy"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "updatedBy", new IdName(2L, "updatedBy"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setUpdatedBy(new IdName(2L, "updatedBy"));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNotNull_onUpdatedById_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "updatedBy", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "updatedBy", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setUpdatedBy(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }


    @Test
    public void givenEquals_onSamePipelineIds_evaluatesTrue() {
      Object pipeline = new Pipeline(242L, "new pipeline", new IdName(23L, "new stage"));
      ConditionExpression expression = new ConditionExpression(EQUAL, "pipeline", pipeline, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "pipeline", pipeline, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setPipeline(new Pipeline(242L, "new pipeline", new IdName(24L, "new stage1")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenEquals_onDifferentPipelineIds_evaluatesFalse() {
      Object pipeline = new Pipeline(242L, "new pipeline", new IdName(23L, "new stage"));
      ConditionExpression expression = new ConditionExpression(EQUAL, "pipeline", pipeline, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "pipeline", pipeline, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setPipeline(new Pipeline(243L, "new pipeline", new IdName(24L, "new stage1")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onSamePipelineIds_evaluatesFalse() {
      Object pipeline = new Pipeline(242L, "new pipeline", new IdName(23L, "new stage"));
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "pipeline", pipeline, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "pipeline", pipeline, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setPipeline(new Pipeline(242L, "new pipeline", new IdName(24L, "new stage1")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEquals_onDifferentPipelineIds_evaluatesTrue() {
      Object pipeline = new Pipeline(242L, "new pipeline", new IdName(23L, "new stage"));
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "pipeline", pipeline, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "pipeline", pipeline, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setPipeline(new Pipeline(243L, "new pipeline", new IdName(24L, "new stage1")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onPipelineId_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "pipeline", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "pipeline", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setPipeline(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onPipelineId_evaluatesFalse() {
      Object pipeline = new Pipeline(242L, "new pipeline", new IdName(23L, "new stage"));
      ConditionExpression expression = new ConditionExpression(IS_NULL, "pipeline", pipeline, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "pipeline", pipeline, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setPipeline(new Pipeline(243L, "new pipeline", new IdName(24L, "new stage1")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenIsNotNull_onPipelineId_evaluatesTrue() {
      Object pipeline = new Pipeline(242L, "new pipeline", new IdName(23L, "new stage"));
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "pipeline", pipeline, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "pipeline", pipeline, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setPipeline(new Pipeline(243L, "new pipeline", new IdName(24L, "new stage1")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNotNull_onPipelineId_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "pipeline", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "pipeline", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setPipeline(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

  }

  @Nested
  @DisplayName("Condition tests for Deal IdName List values")
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestDatabaseInitializer.class})
  public class IdNameListConditionTest {

    @Test
    public void givenContains_onAssociatedContacts_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(CONTAINS, "associatedContacts", new IdName(3L, "User2"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(CONTAINS, "associatedContacts", new IdName(3L, "User2"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setAssociatedContacts(List.of(new IdName(2L, "user1"), new IdName(3L, "User2")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenContains_onAssociatedContacts_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(CONTAINS, "associatedContacts", new IdName(4L, "User4"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(CONTAINS, "associatedContacts", new IdName(4L, "User4"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setAssociatedContacts(List.of(new IdName(2L, "user1"), new IdName(3L, "User2")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotContains_onAssociatedContacts_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(NOT_CONTAINS, "associatedContacts", new IdName(3L, "User2"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_CONTAINS, "associatedContacts", new IdName(3L, "User2"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setAssociatedContacts(List.of(new IdName(2L, "user1"), new IdName(3L, "User2")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotContains_onAssociatedContacts_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(NOT_CONTAINS, "associatedContacts", new IdName(4L, "User4"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_CONTAINS, "associatedContacts", new IdName(4L, "User4"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setAssociatedContacts(List.of(new IdName(2L, "user1"), new IdName(3L, "User2")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsEmpty_onAssociatedContacts_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_EMPTY, "associatedContacts", new IdName(4L, "User4"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_EMPTY, "associatedContacts", new IdName(4L, "User4"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setAssociatedContacts(Collections.emptyList());

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsEmpty_onAssociatedContacts_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_EMPTY, "associatedContacts", new IdName(4L, "User4"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_EMPTY, "associatedContacts", new IdName(4L, "User4"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setAssociatedContacts(List.of(new IdName(2L, "user1"), new IdName(3L, "User2")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenIsNotEmpty_onAssociatedContacts_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_EMPTY, "associatedContacts", new IdName(4L, "User4"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_EMPTY, "associatedContacts", new IdName(4L, "User4"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setAssociatedContacts(Collections.emptyList());

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenIsNotEmpty_onAssociatedContacts_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_EMPTY, "associatedContacts", new IdName(4L, "User4"), NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_EMPTY, "associatedContacts", new IdName(4L, "User4"), OLD_VALUE);
      var entity = stubDealDetail();
      entity.setAssociatedContacts(List.of(new IdName(2L, "user1"), new IdName(3L, "User2")));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

  }

  @Nested
  @DisplayName("Condition tests for Deal Currency Values")
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestDatabaseInitializer.class})
  public class DealCurrencyValueConditionTest {

    @Test
    public void givenEqual_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(EQUAL, "estimatedValue", 4000.00, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "estimatedValue", 4000.00, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 4000.00));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenEqual_onEstimatedValue_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(EQUAL, "estimatedValue", 4000.00, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "estimatedValue", 4000.00, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 5000.00));

      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEqual_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "estimatedValue", 4000.00, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "estimatedValue", 4000.00, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 5000.00));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenNotEqual_onEstimatedValue_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "estimatedValue", 4000.00, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "estimatedValue", 4000.00, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 4000.00));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenBetween_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(BETWEEN, "estimatedValue", "[20, 50]", NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(BETWEEN, "estimatedValue", "[20, 50]", OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 30D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenNotBetween_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(NOT_BETWEEN, "estimatedValue", "[20, 50]", NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_BETWEEN, "estimatedValue", "[20, 50]", OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 60D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenGreater_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(GREATER, "estimatedValue", 50D, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(GREATER, "estimatedValue", 50D, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 60D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenGreaterOrEqual_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(GREATER_OR_EQUAL, "estimatedValue", 60D, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(GREATER_OR_EQUAL, "estimatedValue", 60D, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 60D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenLess_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(LESS, "estimatedValue", 50D, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(LESS, "estimatedValue", 50D, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 40D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenLessOrEqual_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(LESS_OR_EQUAL, "estimatedValue", 40D, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(LESS_OR_EQUAL, "estimatedValue", 40D, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 40D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenIn_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IN, "estimatedValue", "20, 40, 30", NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IN, "estimatedValue", "20, 40, 30", OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 40D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenNotIn_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(NOT_IN, "estimatedValue", "20, 40, 30", NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_IN, "estimatedValue", "20, 40, 30", OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 500D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "estimatedValue", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "estimatedValue", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNotNull_onEstimatedValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "estimatedValue", 40D, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "estimatedValue", 40D, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setEstimatedValue(new Money(12L, 40D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenEqual_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(EQUAL, "actualValue", 4000.00, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "actualValue", 4000.00, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 4000.00));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenEqual_onActualValue_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(EQUAL, "actualValue", 4000.00, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(EQUAL, "actualValue", 4000.00, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 5000.00));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenNotEqual_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "actualValue", 4000.00, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "actualValue", 4000.00, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 5000.00));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenNotEqual_onActualValue_evaluatesFalse() {
      ConditionExpression expression = new ConditionExpression(NOT_EQUAL, "actualValue", 4000.00, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_EQUAL, "actualValue", 4000.00, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 4000.00));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isFalse();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isFalse();
    }

    @Test
    public void givenBetween_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(BETWEEN, "actualValue", "[20, 50]", NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(BETWEEN, "actualValue", "[20, 50]", OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 30D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenNotBetween_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(NOT_BETWEEN, "actualValue", "[20, 50]", NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_BETWEEN, "actualValue", "[20, 50]", OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 60D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenGreater_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(GREATER, "actualValue", 50D, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(GREATER, "actualValue", 50D, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 60D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenGreaterOrEqual_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(GREATER_OR_EQUAL, "actualValue", 60D, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(GREATER_OR_EQUAL, "actualValue", 60D, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 60D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenLess_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(LESS, "actualValue", 50D, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(LESS, "actualValue", 50D, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 40D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenLessOrEqual_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(LESS_OR_EQUAL, "actualValue", 40D, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(LESS_OR_EQUAL, "actualValue", 40D, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 40D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenIn_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IN, "actualValue", "20, 40, 30", NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IN, "actualValue", "20, 40, 30", OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 40D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenNotIn_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(NOT_IN, "actualValue", "20, 40, 30", NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(NOT_IN, "actualValue", "20, 40, 30", OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 500D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();
    }

    @Test
    public void givenIsNull_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NULL, "actualValue", null, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NULL, "actualValue", null, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(null);

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }

    @Test
    public void givenIsNotNull_onActualValue_evaluatesTrue() {
      ConditionExpression expression = new ConditionExpression(IS_NOT_NULL, "actualValue", 40D, NEW_VALUE);
      ConditionExpression oldValExpression = new ConditionExpression(IS_NOT_NULL, "actualValue", 40D, OLD_VALUE);
      var entity = stubDealDetail();
      entity.setActualValue(new Money(12L, 40D));

      assertThat(conditionFacade.satisfies(expression, entity))
          .isTrue();
      assertThat(conditionFacade.satisfies(oldValExpression, entity))
          .isTrue();

    }
  }

  @Nested
  @DisplayName("Condition tests for Deal IsChanged Values")
  @SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
  @AutoConfigureTestDatabase(replace = Replace.NONE)
  @ContextConfiguration(initializers = {TestDatabaseInitializer.class})
  public class DealIsChangedValueTest {

    @Test
    public void givenIsChanged_onDealName_evaluatesTrue() {
      var expression = new ConditionExpression(null, null, null, "name", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      var newEntity = stubDealDetail();
      newEntity.setName("new deal changed");
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isTrue();
    }

    @Test
    public void givenIsChanged_onDealName_evaluatesFalse() {
      var expression = new ConditionExpression(null, null, null, "name", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      var newEntity = stubDealDetail();
      newEntity.setName("new deal");
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isFalse();
    }

    @Test
    public void givenIsChanged_onDealOwnedBy_evaluatesTrue() {
      var expression = new ConditionExpression(null, null, null, "ownedBy", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setOwnedBy(new IdName(12L, "oldOwnedBy"));
      var newEntity = stubDealDetail();
      newEntity.setOwnedBy(new IdName(13L, "newOwnedBy"));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isTrue();
    }

    @Test
    public void givenIsChanged_onDealOwnedBy_evaluatesFalse() {
      var expression = new ConditionExpression(null, null, null, "ownedBy", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setOwnedBy(new IdName(12L, "oldOwnedBy"));
      var newEntity = stubDealDetail();
      newEntity.setOwnedBy(new IdName(12L, "newOwnedBy"));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isFalse();
    }

    @Test
    public void givenIsChanged_onDealCreatedBy_evaluatesTrue() {
      var expression = new ConditionExpression(null, null, null, "createdBy", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setCreatedBy(new IdName(12L, "oldCreatedBy"));
      var newEntity = stubDealDetail();
      newEntity.setCreatedBy(new IdName(13L, "newCreatedBy"));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isTrue();
    }

    @Test
    public void givenIsChanged_onDealCreatedBy_evaluatesFalse() {
      var expression = new ConditionExpression(null, null, null, "createdBy", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setCreatedBy(new IdName(12L, "oldCreatedBy"));
      var newEntity = stubDealDetail();
      newEntity.setCreatedBy(new IdName(12L, "newCreatedBy"));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isFalse();
    }

    @Test
    public void givenIsChanged_onDealUpdatedBy_evaluatesTrue() {
      var expression = new ConditionExpression(null, null, null, "updatedBy", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setUpdatedBy(new IdName(12L, "oldUpdatedBy"));
      var newEntity = stubDealDetail();
      newEntity.setUpdatedBy(new IdName(13L, "newUpdatedBy"));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isTrue();
    }

    @Test
    public void givenIsChanged_onDealUpdatedBy_evaluatesFalse() {
      var expression = new ConditionExpression(null, null, null, "updatedBy", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setUpdatedBy(new IdName(12L, "oldUpdatedBy"));
      var newEntity = stubDealDetail();
      newEntity.setUpdatedBy(new IdName(12L, "newUpdatedBy"));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isFalse();
    }

    @Test
    public void givenIsChanged_onDealProduct_evaluatesTrue() {
      var expression = new ConditionExpression(null, null, null, "product", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setProduct(new IdName(12L, "oldProduct"));
      var newEntity = stubDealDetail();
      newEntity.setProduct(new IdName(13L, "newProduct"));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isTrue();
    }

    @Test
    public void givenIsChanged_onDealProduct_evaluatesFalse() {
      var expression = new ConditionExpression(null, null, null, "product", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setProduct(new IdName(12L, "oldProduct"));
      var newEntity = stubDealDetail();
      newEntity.setProduct(new IdName(12L, "newProduct"));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isFalse();
    }

    @Test
    public void givenIsChanged_onDealCompany_evaluatesTrue() {
      var expression = new ConditionExpression(null, null, null, "company", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setCompany(new IdName(12L, "oldCompany"));
      var newEntity = stubDealDetail();
      newEntity.setCompany(new IdName(13L, "newCompany"));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isTrue();
    }

    @Test
    public void givenIsChanged_onDealCompany_evaluatesFalse() {
      var expression = new ConditionExpression(null, null, null, "company", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setCompany(new IdName(12L, "oldCompany"));
      var newEntity = stubDealDetail();
      newEntity.setCompany(new IdName(12L, "newCompany"));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isFalse();
    }

    @Test
    public void givenIsChanged_onDealPipeline_evaluatesTrue() {
      var expression = new ConditionExpression(null, null, null, "pipeline", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setPipeline(new Pipeline(12L, "oldPipeline", new IdName(5L, "stage")));
      var newEntity = stubDealDetail();
      newEntity.setPipeline(new Pipeline(13L, "newPipeline", new IdName(5L, "stage")));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isTrue();
    }

    @Test
    public void givenIsChanged_onDealPipeline_evaluatesFalse() {
      var expression = new ConditionExpression(null, null, null, "pipeline", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setPipeline(new Pipeline(12L, "oldPipeline", new IdName(5L, "stage")));
      var newEntity = stubDealDetail();
      newEntity.setPipeline(new Pipeline(12L, "newPipeline", new IdName(5L, "stage")));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isFalse();
    }

    @Test
    public void givenIsChanged_onDealEstimatedValue_evaluatesTrue() {
      var expression = new ConditionExpression(null, null, null, "estimatedValue", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setEstimatedValue(new Money(12L, 3000D));
      var newEntity = stubDealDetail();
      newEntity.setEstimatedValue(new Money(13L, 3001D));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isTrue();
    }

    @Test
    public void givenIsChanged_onDealEstimatedValue_evaluatesFalse() {
      var expression = new ConditionExpression(null, null, null, "estimatedValue", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setEstimatedValue(new Money(12L, 3000D));
      var newEntity = stubDealDetail();
      newEntity.setEstimatedValue(new Money(12L, 3000D));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isFalse();
    }

    @Test
    public void givenIsChanged_onDealActualValue_evaluatesTrue() {
      var expression = new ConditionExpression(null, null, null, "actualValue", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setActualValue(new Money(12L, 3000D));
      var newEntity = stubDealDetail();
      newEntity.setActualValue(new Money(13L, 3001D));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isTrue();
    }

    @Test
    public void givenIsChanged_onDealActualValue_evaluatesFalse() {
      var expression = new ConditionExpression(null, null, null, "actualValue", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setActualValue(new Money(12L, 3000D));
      var newEntity = stubDealDetail();
      newEntity.setActualValue(new Money(12L, 3000D));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isFalse();
    }

    @Test
    public void givenIsChanged_onDealAssociatedContacts_evaluatesTrue() {
      var expression = new ConditionExpression(null, null, null, "associatedContacts", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setAssociatedContacts(List.of(new IdName(12L, "new contact")));
      var newEntity = stubDealDetail();
      newEntity.setAssociatedContacts(List.of(new IdName(13L, "new contact")));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isTrue();
    }

    @Test
    public void givenIsChanged_onDealAssociatedContacts_evaluatesFalse() {
      var expression = new ConditionExpression(null, null, null, "associatedContacts", null, "IS_CHANGED");
      var oldEntity = stubDealDetail();
      oldEntity.setAssociatedContacts(List.of(new IdName(12L, "new contact")));
      var newEntity = stubDealDetail();
      newEntity.setAssociatedContacts(List.of(new IdName(12L, "new contact")));
      DealEvent event = new DealEvent(newEntity, oldEntity, null);
      assertThat(conditionFacade.satisfiesValueIsChanged(event, expression)).isFalse();
    }

  }

  private DealDetail stubDealDetail() {
    var dealDetail = new DealDetail();
    dealDetail.setId(2000L);
    dealDetail.setName("new deal");
    return dealDetail;
  }

}
