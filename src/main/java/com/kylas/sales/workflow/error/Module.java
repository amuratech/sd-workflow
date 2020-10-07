package com.kylas.sales.workflow.error;

public enum Module {

  Domain("01"), Security("02"), Rest("03");

  Module(String code){
    this.code = code;
  }

  private String code;

  public String getCode(){
    return code;
  }
}
