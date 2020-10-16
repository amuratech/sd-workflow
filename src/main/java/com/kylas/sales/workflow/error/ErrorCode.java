package com.kylas.sales.workflow.error;

public class ErrorCode {

  private static final String serviceCode = "017";

  public static final String LAYOUT_NOT_FOUND = serviceCode + Module.Domain.getCode() + "001";
  public static final String INVALID_ACTION = serviceCode + Module.Domain.getCode() + "002";
  public static final String INVALID_WORKFLOW_PROPERTY = serviceCode + Module.Domain.getCode() + "003";
  public static final String UPDATE_PROPERTY = serviceCode + Module.Domain.getCode() + "004";

  public static final String INSUFFICIENT_PRIVILEGES =
      serviceCode + Module.Security.getCode() + "001";

  public static final String MALFORMED_REQUEST = serviceCode + Module.Rest.getCode() + "001";
  public static final String GENERIC_ERROR = serviceCode + Module.Rest.getCode() + "002";
}
