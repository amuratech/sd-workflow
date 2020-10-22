package com.kylas.sales.workflow.domain;

import com.kylas.sales.workflow.domain.user.User_;
import com.kylas.sales.workflow.domain.workflow.EntityType;
import com.kylas.sales.workflow.domain.workflow.Workflow;
import com.kylas.sales.workflow.domain.workflow.Workflow_;
import org.springframework.data.jpa.domain.Specification;

public class WorkflowSpecification {

  static Specification<Workflow> belongToTenant(long tenantId) {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.tenantId), tenantId);
  }

  static Specification<Workflow> belongToUser(long userId) {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.createdBy).get(User_.id), userId);
  }

  static Specification<Workflow> withId(long workflowId) {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.id), workflowId);
  }

  static Specification<Workflow> withEntityType(EntityType entityType) {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.entityType), entityType);
  }

  static Specification<Workflow> active() {
    return (root, criteria, builder) -> builder.equal(root.get(Workflow_.active), true);
  }
}
