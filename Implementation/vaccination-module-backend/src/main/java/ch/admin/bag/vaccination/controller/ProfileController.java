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
package ch.admin.bag.vaccination.controller;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.cache.Cache;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints depending on the profile - local mode - SAML-Authentication are enabled only if !prod
 *
 */
@Slf4j
@RestController
@Tag(name = "Profile Controller", description = "Controller to active/deactive both localMode and SAML-Authentication")
@Profile("!prod")
public class ProfileController {

  @Autowired
  private ProfileConfig profileConfig;

  @Autowired
  private Cache cache;

  @GetMapping("/utility/setLocalMode/{active}")
  public String setLocalMode(@PathVariable boolean active) {
    log.debug("setLocalMode:{}", active);
    profileConfig.setLocalMode(active);
    profileConfig.setHuskyLocalMode(active);
    cache.clear();

    return "Local mode set to " + String.valueOf(active);
  }

  @GetMapping("/saml/authentication/{active}")
  public String setSamlActivation(@PathVariable boolean active) {
    log.debug("setSamlActivation:{}", active);
    profileConfig.setSamlAuthenticationActive(active);
    return "SAML authentication was set to " + String.valueOf(active);
  }

}
