package com.kylas.sales.workflow.domain.workflow.action.webhook;

import com.kylas.sales.workflow.domain.workflow.EntityType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping(value = "/v1/workflows/webhook")
public class WebhookController {

  private final WebhookService webhookService;

  @Autowired
  public WebhookController(WebhookService webhookService) {
    this.webhookService = webhookService;
  }

  @GetMapping("/{entityType}/config")
  public Flux<EntityConfig> getConfigurations(@PathVariable("entityType") final EntityType entityType) {
    return webhookService.getConfigurations(entityType);
  }
}
