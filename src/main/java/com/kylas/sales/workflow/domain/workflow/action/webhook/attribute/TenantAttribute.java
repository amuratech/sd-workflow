package com.kylas.sales.workflow.domain.workflow.action.webhook.attribute;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum TenantAttribute implements EntityAttribute {

  ACCOUNT_NAME("accountName", "Account Name"),
  INDUSTRY("industry", "Industry"),
  ADDRESS("address", "Address"),
  CITY("city", "City"),
  STATE("state", "State"),
  COUNTRY("country", "Country"),
  ZIPCODE("zipcode", "Zipcode"),
  LANGUAGE("language", "Language"),
  CURRENCY("currency", "Currency"),
  TIMEZONE("timezone", "Timezone"),
  COMPANY_NAME("companyName", "Company Name"),
  WEBSITE("website", "Website");

  private final String name;
  private final String displayName;

  public static List<Attribute> getAttributes() {
    return Arrays.stream(values())
        .map(attribute -> new Attribute(attribute.name, attribute.displayName))
        .collect(Collectors.toList());
  }

  @Override
  public String getPathToField() {
    return name;
  }
}
