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

import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * Basic web configuration for the backend application.
 */
@Slf4j
@Configuration
public class WebConfiguration implements ServletContextInitializer {

  private final Environment environment;

  /**
   * Configurable frontend URL to avoid cors problems.
   */
  @Value("${application.frontendUrl}")
  private String frontendUrl;

  public WebConfiguration(Environment environment) {
    this.environment = environment;
  }

  @Bean
  public CorsFilter corsFilter() {
    return new CorsFilter(corsConfigurationSource());
  }

  @Override
  public void onStartup(ServletContext servletContext) throws ServletException {
    if (environment.getActiveProfiles().length != 0) {
      log.info("Web application configuration, using profiles: {}",
          (Object[]) environment.getActiveProfiles());
    }

    log.info("Web application fully configured");
  }

  private CorsConfiguration corsConfiguration() {
    var configuration = new CorsConfiguration();
    configuration.setAllowedMethods(List.of("DELETE", "GET", "PUT", "POST"));
    configuration.setAllowedOrigins(List.of(frontendUrl));
    configuration.setAllowedHeaders(List.of("*"));

    return configuration;
  }

  private CorsConfigurationSource corsConfigurationSource() {
    var source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", corsConfiguration());

    return source;
  }
}
