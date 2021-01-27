package com.kylas.sales.workflow.layout.api;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import com.kylas.sales.workflow.layout.api.response.Layout;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/v1")
public class LayoutController {

  private final LayoutService layoutService;

  @Autowired
  public LayoutController(LayoutService layoutService) {
    this.layoutService = layoutService;
  }

  @ApiOperation(value = "Get list layout for deals", code = 200, response = Layout.class)
  @GetMapping(value = "/workflows/layout/list", produces = APPLICATION_JSON_VALUE)
  public ResponseEntity getListLayout() {
    var layoutForView = layoutService.getListLayout();
    return ResponseEntity.ok(layoutForView);
  }
}
