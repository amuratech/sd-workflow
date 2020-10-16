package com.kylas.sales.workflow.domain.processor.lead;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class Product {

  private long id;
  private String name;
  private long tenantId;

  public Product(long id, String name, long tenantId) {
    this.id = id;
    this.name = name;
    this.tenantId = tenantId;
  }

  @JsonCreator
  public Product(@JsonProperty("id") long id, @JsonProperty("name") String name) {
    this.id = id;
    this.name = name;
  }

  public Product withName(String name) {
    return new Product(id, name, tenantId);
  }

  public Product withTenantId(long tenantId) {
    return new Product(id, name, tenantId);
  }
}
