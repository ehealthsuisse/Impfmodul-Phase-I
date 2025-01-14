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

import java.security.Provider;
import java.security.Security;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.xml.ParserPool;
import net.shibboleth.shared.xml.impl.BasicParserPool;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.xmlsec.config.impl.JavaCryptoValidationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration of the SAML security mechanisms
 */
@Configuration
@Slf4j
public class SAMLConfig {

  /** Used to build the SAML objects */
  @Bean
  XMLObjectProviderRegistry xmlObjectProviderRegistry() {
    JavaCryptoValidationInitializer javaCryptoValidationInitializer =
        new JavaCryptoValidationInitializer();
    XMLObjectProviderRegistry registry = new XMLObjectProviderRegistry();
    ConfigurationService.register(XMLObjectProviderRegistry.class, registry);

    registry.setParserPool(getParserPool());
    try {
      javaCryptoValidationInitializer.init();

      for (Provider jceProvider : Security.getProviders()) {
        log.info(jceProvider.getInfo());
      }

      InitializationService.initialize();
    } catch (InitializationException e) {
      log.error(e.getMessage(), e);
    }

    return registry;
  }

  private ParserPool getParserPool() {
    BasicParserPool parserPool = new BasicParserPool();
    parserPool.setMaxPoolSize(100);
    parserPool.setCoalescing(true);
    parserPool.setIgnoreComments(true);
    parserPool.setIgnoreElementContentWhitespace(true);
    parserPool.setNamespaceAware(true);
    parserPool.setExpandEntityReferences(false);
    parserPool.setXincludeAware(false);

    final Map<String, Boolean> features = new HashMap<>();
    features.put("http://xml.org/sax/features/external-general-entities", Boolean.FALSE);
    features.put("http://xml.org/sax/features/external-parameter-entities", Boolean.FALSE);
    features.put("http://apache.org/xml/features/disallow-doctype-decl", Boolean.TRUE);
    features.put("http://apache.org/xml/features/validation/schema/normalized-value",
        Boolean.FALSE);
    features.put("http://javax.xml.XMLConstants/feature/secure-processing", Boolean.TRUE);

    parserPool.setBuilderFeatures(features);
    parserPool.setBuilderAttributes(new HashMap<>());

    try {
      parserPool.initialize();
    } catch (ComponentInitializationException e) {
      log.error(e.getMessage(), e);
    }

    return parserPool;
  }
}
