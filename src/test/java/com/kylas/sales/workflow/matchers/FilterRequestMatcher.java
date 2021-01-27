package com.kylas.sales.workflow.matchers;

import com.kylas.sales.workflow.api.request.FilterRequest;
import java.util.Optional;
import org.mockito.ArgumentMatcher;

public class FilterRequestMatcher implements ArgumentMatcher<Optional<FilterRequest>> {

  private FilterRequest expectedFilter;

  public FilterRequestMatcher(FilterRequest expectedFilter) {
    this.expectedFilter = expectedFilter;
  }


  @Override
  public boolean matches(Optional<FilterRequest> requestedFilter) {
    return requestedFilter.get().getFilters().size() == expectedFilter.getFilters().size();
  }
}
