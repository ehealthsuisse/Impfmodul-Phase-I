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

import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.DisablePayloadCollectingDeactivationInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.InPayloadLoggerInterceptor;
import org.openehealth.ipf.commons.ihe.ws.cxf.payload.OutPayloadLoggerInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * xml exchange message interceptor for log
 */
@AutoConfiguration
@Slf4j
public class IpfApplicationConfig {

  @Value("${epdbackend.activateSoapLogging}")
  Boolean activeSoapLogging = false;

  @Value("${epdbackend.soapLoggingPath}")
  String soapLoggingPath;

  @Bean
  AbstractPhaseInterceptor<?> serverInLogger() {
    if (activeSoapLogging) {
      log.info("Logging soap incoming messages to {}", soapLoggingPath);
      return new InPayloadLoggerInterceptor(Path.of(soapLoggingPath, "incomingSoapMessages.log").toString());
    }

    log.info("Logging of soap incoming messages is disabled.");
    return new DisablePayloadCollectingDeactivationInterceptor();
  }

  @Bean
  AbstractPhaseInterceptor<?> serverOutLogger() {
    if (activeSoapLogging) {
      log.info("Logging soap outgoing messages to {}", soapLoggingPath);
      return new OutPayloadLoggerInterceptor(Path.of(soapLoggingPath, "outgoingSoapMessages.log").toString());
    }

    log.info("Logging of soap outgoing messages is disabled.");
    return new DisablePayloadCollectingDeactivationInterceptor();
  }

}
