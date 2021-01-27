package com.kylas.sales.workflow.domain.workflow.action.webhook;

import com.kylas.sales.workflow.config.TestDatabaseInitializer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = Replace.NONE)
@ContextConfiguration(initializers = {TestDatabaseInitializer.class})
class CryptoServiceTest {

  @Autowired
  private CryptoService cryptoService;

  @Test
  public void givenPlainText_shouldEncrypt() {
    var text = "some-random-plain-text";
    var encrypted = cryptoService.encrypt(text);
    Assertions.assertThat(encrypted).isNotEmpty();
  }

  @Test
  public void decryptCipherText_shouldProducePlainText() {
    var text = "some-long-random-plain-text-some-long-random-plain-text"
        + "-some-long-random-plain-text-some-long-random-plain-text"
        + "-some-long-random-plain-text-some-long-random-plain-text"
        + "-some-long-random-plain-text-some-long-random-plain-text"
        + "-some-long-random-plain-text-some-long-random-plain-text"
        + "-some-long-random-plain-text-some-long-random-plain-text"
        + "-some-long-random-plain-text-some-long-random-plain-text";
    var encrypted = cryptoService.encrypt(text);
    var decrypted = cryptoService.decrypt(encrypted);
    Assertions.assertThat(decrypted).isNotEmpty();
    Assertions.assertThat(decrypted).isEqualTo(text);
  }
}