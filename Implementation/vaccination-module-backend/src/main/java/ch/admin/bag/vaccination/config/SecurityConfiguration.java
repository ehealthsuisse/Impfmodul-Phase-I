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
import ch.admin.bag.vaccination.service.saml.SAMLAuthProvider;
import ch.admin.bag.vaccination.service.saml.SAMLFilter;
import ch.admin.bag.vaccination.service.saml.SAMLServiceIfc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.CorsFilter;

/**
 * Security configuration used for productive environments. Secures the productive endpoints using
 * SAML authentication.
 */
@Configuration
@Profile("!test & !local & !dev")
public class SecurityConfiguration extends AbsSecurityConfiguration {

  @Autowired
  private CorsFilter corsFilter;

  @Autowired
  private SAMLServiceIfc samlService;

  @Autowired
  private ProfileConfig profileConfig;

  @Autowired
  private FilterChainExceptionHandler filterChainExceptionHandler;

  @Autowired
  private SAMLAuthProvider samlAuthProvider;

  @Value("${application.frontendDomain}")
  private String frontendDomain;

  /** Include our saml authentication provider in the list of providers */
  @Bean
  AuthenticationManager authManager(HttpSecurity http) throws Exception {
    AuthenticationManagerBuilder authenticationManagerBuilder =
        http.getSharedObject(AuthenticationManagerBuilder.class);
    authenticationManagerBuilder.authenticationProvider(samlAuthProvider);

    return authenticationManagerBuilder.build();
  }

  @Bean
  SecurityFilterChain filterChain(HttpSecurity http, AuthenticationManager authManager) throws Exception {
    HttpSessionSecurityContextRepository securityContextRepository = new HttpSessionSecurityContextRepository();
    http.addFilter(corsFilter)
        .csrf()
        .csrfTokenRepository(createCsrfTokenRepository(frontendDomain))
        .ignoringAntMatchers("/saml/**", "/signature/validate")
        .and()
        .authorizeRequests()
        // allow SAML authentication and back channel logout
        .antMatchers("/saml/sso", "/saml/logout", "/saml/isAuthenticated").permitAll()

        // allow access to actuators
        .antMatchers("/actuator/health", "/actuator/health/**").permitAll()

        // allow access to signature service
        .antMatchers("/signature/**").permitAll()

        // allow access to utility controller
        .antMatchers("/utility/**").permitAll()

        // forbid swagger
        .antMatchers("/swagger", "/swagger-ui/**", "/v3/api-docs/**").denyAll()
        .anyRequest().authenticated()
        .and()
        .addFilterBefore(createSAMLFilter(samlService, profileConfig), UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(createSAMLAuthFilter(authManager, securityContextRepository), SAMLFilter.class)
        .addFilterBefore(filterChainExceptionHandler, LogoutFilter.class)
        .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        .and()
        .authenticationManager(authManager)
        .securityContext((securityContext) -> securityContext
            .securityContextRepository(securityContextRepository)
            .requireExplicitSave(true))
        .logout(createLogoutConfig(samlService));

    return http.build();
  }

}

