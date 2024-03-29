/**
 * Copyright (c) 2022 eHealth Suisse
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
package ch.admin.bag.vaccination.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Contains the variable depending on a profile
 *
 */
@Configuration
@Getter
public class ProfileConfig {
  @Value("${spring.profiles.active}")
  private String activeProfile;
  @Value("${application.localMode}")
  private boolean localMode = false;
  @Value("${application.huskyLocalMode}")
  private Boolean huskyLocalMode = false;
  private boolean samlAuthenticationActive = false;

  public boolean isSamlAuthenticationActive() {
    return samlAuthenticationActive || isProd();
  }

  /**
   * Allows setting the local mode.
   *
   * @param huskyLocalMode true to write local file, false to write in the EPD, null to write nothing
   */
  public void setHuskyLocalMode(Boolean huskyLocalMode) {
    if (!isProd()) {
      this.huskyLocalMode = huskyLocalMode;
    }
  }

  public void setLocalMode(boolean localMode) {
    if (!isProd()) {
      this.localMode = localMode;
    }
  }

  public void setSamlAuthenticationActive(boolean samlAuthentication) {
    if (!isProd()) {
      this.samlAuthenticationActive = samlAuthentication;
    }
  }

  private boolean isProd() {
    return "prod".equalsIgnoreCase(activeProfile);
  }
}
