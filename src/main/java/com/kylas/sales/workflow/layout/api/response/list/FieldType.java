package com.kylas.sales.workflow.layout.api.response.list;

public enum FieldType {
  TEXT_FIELD("Allows users to create Text field"),
  SINGLE_LINE_TEXT("Allows users to create Single line text"),
  PARAGRAPH_TEXT("Allows users to create Paragraph text"),
  RICH_TEXT("Allows users to create Rich text"),
  NUMBER("Allows users to enter number leading zeros are removed"),
  CHECKBOX("Allows users to create checkbox with option values"),
  RADIO_BUTTON("Allows users to create radio button with option values"),
  PICK_LIST("Allows users to create picklist with option values"),
  MULTI_PICKLIST("Allows users to create multi-picklist with option values"),
  DATE_PICKER("Allows users to enter a date or pick a date from a popup calendar"),
  TIME_PICKER("Allows users to enter a time"),
  DATETIME_PICKER("Allows user to enter a date and time,or pick a date from popup calendar.When users click a date in the popup,that date and current time are entered into Date/Time field"),
  URL("url"),
  EMAIL("Allow user to enter a email address, which is validated to ensure proper format. If this field is specified for a contact or lead,user can choose the address when clicking send an Email"),
  PRODUCT("Allow user to associate Product"),
  PHONE("phone"),
  LOOK_UP("lookup"),
  AUTO_INCREMENT("A system-generated sequence number that uses a display format you define.The number is automatically incremented for each new record"),
  TOGGLE("Allows users to select True(checked) or False(unchecked) value"),
  MONEY("Allows users to select a currency type and value"),
  FORECASTING_TYPE("Allows users to select a pipeline stage forecasting Types");
  private final String description;

  private FieldType(String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }
}
