package com.kylas.sales.workflow.domain.workflow.action;

import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.ARRAY;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.OBJECT;
import static com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction.ValueType.PLAIN;
import static org.assertj.core.api.Assertions.assertThat;

import com.kylas.sales.workflow.common.dto.ActionDetail.EditPropertyAction;
import com.kylas.sales.workflow.common.dto.ActionResponse;
import com.kylas.sales.workflow.domain.processor.lead.ConversionAssociation;
import com.kylas.sales.workflow.domain.processor.lead.Product;
import com.kylas.sales.workflow.domain.workflow.action.WorkflowAction.ActionType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class EditPropertyActionTest {

  @Test
  public void givenEditPropertyAction_withValueTypeString_shouldCreate() {
    //given
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("firstName", "Tony", PLAIN));
    Set<ActionResponse> actionResponses = new HashSet<>();
    actionResponses.add(editPropertyAction);
    //when
    Set<AbstractWorkflowAction> actions = actionResponses.stream()
        .map(actionResponse -> editPropertyAction.getType().create(actionResponse))
        .collect(Collectors.toSet());
    //then
    assertThat(actions.size()).isEqualTo(1);
    com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction action = (com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction) actions
        .iterator().next();
    assertThat(action.getName()).isEqualTo("firstName");
    assertThat(action.getValue()).isEqualTo("Tony");
  }

  @Test
  public void givenEditPropertyAction_withValueTypeNumber_shouldCreate() {
    //given
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("salutation", 1, PLAIN));
    Set<ActionResponse> actionResponses = new HashSet<>();
    actionResponses.add(editPropertyAction);
    //when
    Set<AbstractWorkflowAction> actions = actionResponses.stream()
        .map(actionResponse -> editPropertyAction.getType().create(actionResponse))
        .collect(Collectors.toSet());
    //then
    assertThat(actions.size()).isEqualTo(1);
    com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction action = (com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction) actions
        .iterator().next();
    assertThat(action.getName()).isEqualTo("salutation");
    assertThat(action.getValue()).isEqualTo(1);
  }

  @Test
  public void givenEditPropertyAction_withValueTypeObjectArray_shouldCreate() {
    //given
    ArrayList<Product> products = new ArrayList<>(Arrays.asList(new Product(1, "cellphone"), new Product(2, "telephone")));
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY, new EditPropertyAction("products", products, ARRAY));
    Set<ActionResponse> actionResponses = new HashSet<>();
    actionResponses.add(editPropertyAction);
    //when
    Set<AbstractWorkflowAction> actions = actionResponses.stream()
        .map(actionResponse -> editPropertyAction.getType().create(actionResponse))
        .collect(Collectors.toSet());
    //then
    assertThat(actions.size()).isEqualTo(1);
    com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction action = (com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction) actions
        .iterator().next();
    assertThat(action.getName()).isEqualTo("products");
    assertThat(action.getValue()).isInstanceOf(ArrayList.class);
    List<?> actualProducts = (ArrayList<?>) action.getValue();
    assertThat(actualProducts).hasSize(products.size());
    Product product = (Product) actualProducts.get(0);
    assertThat(product.getId()).isEqualTo(1);
    assertThat(product.getName()).isEqualTo("cellphone");
    product = (Product) actualProducts.get(1);
    assertThat(product.getId()).isEqualTo(2);
    assertThat(product.getName()).isEqualTo("telephone");
  }

  @Test
  public void givenEditPropertyAction_withValueTypeObject_shouldCreate() {
    //given
    ConversionAssociation conversionAssociation = new ConversionAssociation(1L, 2L, 3L, 4L, 5L);
    var editPropertyAction = new ActionResponse(ActionType.EDIT_PROPERTY,
        new EditPropertyAction("conversionAssociation", conversionAssociation, OBJECT));
    Set<ActionResponse> actionResponses = new HashSet<>();
    actionResponses.add(editPropertyAction);
    //when
    Set<AbstractWorkflowAction> actions = actionResponses.stream()
        .map(actionResponse -> editPropertyAction.getType().create(actionResponse))
        .collect(Collectors.toSet());
    //then
    assertThat(actions.size()).isEqualTo(1);
    com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction action = (com.kylas.sales.workflow.domain.workflow.action.EditPropertyAction) actions
        .iterator().next();
    assertThat(action.getName()).isEqualTo("conversionAssociation");
    assertThat(action.getValue()).isInstanceOf(ConversionAssociation.class);
    ConversionAssociation actualConversionAssociation = (ConversionAssociation) action.getValue();
    assertThat(actualConversionAssociation.getId()).isEqualTo(1);
    assertThat(actualConversionAssociation.getDealId()).isEqualTo(2);
    assertThat(actualConversionAssociation.getContactId()).isEqualTo(3);
    assertThat(actualConversionAssociation.getTenantId()).isEqualTo(4);
    assertThat(actualConversionAssociation.getCompanyId()).isEqualTo(5);
  }
}