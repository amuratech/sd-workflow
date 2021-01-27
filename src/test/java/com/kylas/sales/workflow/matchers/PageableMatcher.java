package com.kylas.sales.workflow.matchers;

import org.mockito.ArgumentMatcher;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class PageableMatcher implements ArgumentMatcher<Pageable> {

  private final int pageNumber;
  private final int pageSize;
  private final Sort sort;

  public PageableMatcher(int pageNumber, int pageSize, Sort sort) {
    this.pageNumber = pageNumber;
    this.pageSize = pageSize;
    this.sort = sort;
  }

  @Override
  public boolean matches(Pageable right) {
    return pageNumber == right.getPageNumber()
        && pageSize == right.getPageSize()
        && sort.equals(right.getSort());
  }
}
