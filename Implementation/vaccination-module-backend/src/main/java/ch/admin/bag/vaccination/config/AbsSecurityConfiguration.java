package ch.admin.bag.vaccination.config;

import ch.admin.bag.vaccination.service.saml.SAMLAuthFilter;
import ch.admin.bag.vaccination.service.saml.SAMLFilter;
import ch.admin.bag.vaccination.service.saml.SAMLServiceIfc;
import javax.servlet.Filter;
import javax.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;

/**
 * Abstract class
 */
public class AbsSecurityConfiguration {
  protected CsrfTokenRepository createCsrfTokenRepository(String frontendDomain) {
    CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookiePath("/");
    if (frontendDomain != null && !frontendDomain.isEmpty()) {
      repository.setCookieDomain(frontendDomain);
    }

    return repository;
  }

  protected Customizer<LogoutConfigurer<HttpSecurity>> createLogoutConfig() {
    return logout -> logout.logoutUrl("/logout")
        .invalidateHttpSession(true)
        .logoutSuccessHandler((request, response, authentication) -> {
          response.setStatus(HttpServletResponse.SC_OK);
        });
  }

  protected Filter createSAMLAuthFilter(AuthenticationManager authManager,
      HttpSessionSecurityContextRepository securityContextRepository) {
    return new SAMLAuthFilter(authManager, securityContextRepository);
  }

  protected Filter createSAMLFilter(SAMLServiceIfc samlService, ProfileConfig profileConfig) {
    return new SAMLFilter(samlService, profileConfig);
  }
}
