package com.kylas.sales.workflow.domain.workflow.action.webhook;

import static java.nio.charset.StandardCharsets.UTF_8;

import com.kylas.sales.workflow.domain.processor.exception.WorkflowExecutionException;
import com.kylas.sales.workflow.error.ErrorCode;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CryptoService {

  private static final String CIPHER = "AES/ECB/PKCS5Padding";
  private static final String AES = "AES";
  private static final String SHA_1 = "SHA-1";
  private static SecretKeySpec secretKey;
  private static String secret;

  @Value("${security.crypto.key}")
  public void setSecret(String secret) {
    CryptoService.secret = secret;
  }

  public static void setKey(String myKey) throws NoSuchAlgorithmException {
    byte[] key = myKey.getBytes(UTF_8);
    var sha = MessageDigest.getInstance(SHA_1);
    key = sha.digest(key);
    key = Arrays.copyOf(key, 16);
    secretKey = new SecretKeySpec(key, AES);
  }

  public String encrypt(String text) {
    try {
      setKey(secret);
      var cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey);
      return Base64.getEncoder().encodeToString(cipher.doFinal(text.getBytes(UTF_8)));
    } catch (Exception e) {
      log.error("Exception while encrypting.", e);
      throw new WorkflowExecutionException(ErrorCode.CRYPTO_FAILURE);
    }
  }

  public String decrypt(String strToDecrypt) {
    try {
      setKey(secret);
      var cipher = Cipher.getInstance(CIPHER);
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
      return new String(cipher.doFinal(Base64.getDecoder().decode(strToDecrypt)));
    } catch (Exception e) {
      log.error("Exception while decrypting.", e);
      throw new WorkflowExecutionException(ErrorCode.CRYPTO_FAILURE);
    }
  }
}
