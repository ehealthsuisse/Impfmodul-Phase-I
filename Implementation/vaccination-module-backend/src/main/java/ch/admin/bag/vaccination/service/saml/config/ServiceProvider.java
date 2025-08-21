/**
 * Copyright (c) 2025 eHealth Suisse
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the “Software”), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ch.admin.bag.vaccination.service.saml.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
@ConfigurationProperties
@Getter
@Setter
public class ServiceProvider {
  @Value("${sp.assertionConsumerServiceUrl}")
  private String assertionConsumerServiceUrl;
  @Value("${sp.forwardArtifactToClientUrl}")
  private String forwardArtifactToClientUrl;

  @Value("${sp.keystore.keystore-type}")
  private String samlKeystoreType;
  @Value("${sp.keystore.keystore-path}")
  private String samlKeystorePath;
  @Value("${sp.keystore.keystore-password}")
  private String samlKeystorePassword;
  @Value("${sp.keystore.sp-alias}")
  private String samlSpAlias;

  private String tlsKeystoreType;
  private String tlsKeystorePath;
  private String tlsKeystorePassword;

  @Autowired
  private Environment environment;

  @PostConstruct
  public void init() {
    this.tlsKeystoreType = environment.getProperty("sp.tlsKeystore.keystore-type");
    this.tlsKeystorePath = environment.getProperty("sp.tlsKeystore.keystore-path");
    this.tlsKeystorePassword = environment.getProperty("sp.tlsKeystore.keystore-password");
  }
}
