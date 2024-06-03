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

import ch.admin.bag.vaccination.service.saml.SAMLServiceIfc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.CorsFilter;

/**
 * Local, non productive, security config which is not using SAML authentication.
 *
 * <p>
 * <b>Attention</b> Running the non productive profile is a security risk as all services are
 * exposed.
 * </p>
 */
@Configuration
@Profile("local | dev")
public class LocalSecurityConfiguration extends AbsSecurityConfiguration {

  @Autowired
  private CorsFilter corsFilter;

  @Value("${application.frontendDomain}")
  private String frontendDomain;

  @Autowired
  private SAMLServiceIfc samlService;

  @Bean
  SecurityFilterChain localFilterChain(HttpSecurity http) throws Exception {
    http.addFilter(corsFilter)
        .csrf()
        .csrfTokenRepository(createCsrfTokenRepository(frontendDomain))
        .ignoringAntMatchers("/saml/**", "/signature/validate")
        .and()
        .authorizeRequests()
        .anyRequest().anonymous()
        .and()
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .logout(createLogoutConfig(samlService));
    return http.build();
  }

}
