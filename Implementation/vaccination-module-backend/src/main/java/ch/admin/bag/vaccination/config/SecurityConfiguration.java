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

import ch.admin.bag.vaccination.exception.FilterChainExceptionHandler;
import ch.admin.bag.vaccination.service.saml.SAMLFilter;
import ch.admin.bag.vaccination.service.saml.SAMLService;
import javax.servlet.Filter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.filter.CorsFilter;

/**
 * Security configuration used for productive environments. Secures the productive endpoints using
 * SAML authentication.
 */
@Configuration
@Profile("!test & !local")
public class SecurityConfiguration {

  @Autowired
  private CorsFilter corsFilter;

  @Autowired
  private SAMLService samlService;

  @Autowired
  private ProfileConfig profileConfig;

  @Autowired
  private FilterChainExceptionHandler filterChainExceptionHandler;

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.addFilter(corsFilter)
        .csrf().disable()
        .authorizeRequests()

        // allow SAML authentication
        .antMatchers("/saml/sso", "/saml/login/**", "/saml/authentication/**").permitAll()

        // allow access to actuators
        .antMatchers("/actuator/health", "/actuator/health/**").permitAll()

        // allow access to signature service
        .antMatchers("/signature/**").permitAll()

        // forbid swagger
        .antMatchers("/swagger", "/swagger-ui/**", "/v3/api-docs/**").denyAll()
        .anyRequest().authenticated()
        .and()
        .addFilterBefore(createSAMLFilter(), UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(filterChainExceptionHandler, LogoutFilter.class)
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    return http.build();
  }

  @Bean
  InMemoryUserDetailsManager userDetailsService() {
    // generate no user for basic authentication
    return new InMemoryUserDetailsManager();
  }

  private Filter createSAMLFilter() {
    return new SAMLFilter(samlService, profileConfig);
  }

}

