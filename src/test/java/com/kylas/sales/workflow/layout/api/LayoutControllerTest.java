package com.kylas.sales.workflow.layout.api;

import static org.mockito.BDDMockito.given;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kylas.sales.workflow.api.WorkflowService;
import com.kylas.sales.workflow.layout.api.exception.LayoutNotFoundException;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
@WebMvcTest(controllers = LayoutController.class)
class LayoutControllerTest {

  @Autowired
  private MockMvc mockMvc;
  @MockBean
  private LayoutService layoutService;
  @MockBean
  WorkflowService workflowService;
  @Autowired
  private ResourceLoader resourceLoader;

  @WithMockUser
  @Test
  public void givenNonExistingViewName_shouldReturnNotFound() throws Exception {
    // given
    given(layoutService.getListLayout()).willThrow(new LayoutNotFoundException());

    // when
    var mvcResult =
        mockMvc
            .perform(
                get("/v1/workflows/layout/list")
                    .contentType(APPLICATION_JSON)
                    .accept(APPLICATION_JSON))
            .andExpect(status().isNotFound())
            .andReturn();

    // then
    var expectedResponse =
        getResourceAsString("classpath:contracts/layout/responses/layout-not-found.json");
    var response = mvcResult.getResponse().getContentAsString();
    JSONAssert.assertEquals(expectedResponse, response, JSONCompareMode.LENIENT);
  }

  private String getResourceAsString(String resourcePath) throws IOException {
    var resource = resourceLoader.getResource(resourcePath);
    var file = resource.getFile();
    return FileUtils.readFileToString(file, "UTF-8");
  }

}