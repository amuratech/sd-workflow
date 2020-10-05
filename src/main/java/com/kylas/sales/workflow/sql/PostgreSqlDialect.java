package com.kylas.sales.workflow.sql;

import java.sql.Types;
import org.hibernate.dialect.PostgreSQL94Dialect;

public class PostgreSqlDialect extends PostgreSQL94Dialect {

  public PostgreSqlDialect() {
    this.registerColumnType(Types.JAVA_OBJECT, "json");
  }
}
