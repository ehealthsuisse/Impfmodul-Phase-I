/**
 * Copyright (c) 2023 eHealth Suisse
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
package ch.admin.bag.vaccination.service.saml;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.HttpVersion;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.message.StatusLine;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.junit.jupiter.api.Test;
import org.opensaml.messaging.context.InOutOperationContext;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.profile.context.ProfileRequestContext;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.soap.client.http.AbstractPipelineHttpSOAPClient;
import org.opensaml.soap.soap11.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class IdpAdapterTest {

  @Autowired
  private IdPAdapter adapter;

  @Test
  void sendRequest_injectGoodResponse_noExceptionOccurs_assertionExtractedFromRequest() throws Exception {
    ClassLoader classLoader = getClass().getClassLoader();
    String samlEnvelop = Files.readString(Path.of(classLoader.getResource("saml/samlIdpRenewResponse.xml").toURI()));

    InOutOperationContext context = sendSamlEnvelop(samlEnvelop);
    Assertion assertion = adapter.processResponse(context.getInboundMessageContext().getMessage());

    assertNotNull(assertion);
  }

  private BasicClassicHttpResponse mockResponse() throws Exception {
    BasicClassicHttpResponse httpResponse = mock(BasicClassicHttpResponse.class);
    StatusLine statusLine = new StatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, null);
    HttpEntity entity = mock(HttpEntity.class);

    ClassLoader classLoader = getClass().getClassLoader();
    File file = new File(classLoader.getResource("saml/samlIdpRenewResponse.xml").toURI());
    InputStream is = new FileInputStream(file);
    when(entity.getContentLength()).thenReturn(1000L);
    when(entity.getContent()).thenReturn(is);
    when(httpResponse.getVersion()).thenReturn(statusLine.getProtocolVersion());
    when(httpResponse.getCode()).thenReturn(statusLine.getStatusCode());
    when(httpResponse.getEntity()).thenReturn(entity);
    return httpResponse;
  }

  private InOutOperationContext sendSamlEnvelop(String samlEnvelop) throws Exception {
    Envelope envelope = (Envelope) SAMLUtils.unmarshall(samlEnvelop);
    MessageContext contextOut = new MessageContext();
    contextOut.setMessage(envelope);
    InOutOperationContext context = new ProfileRequestContext();
    context.setOutboundMessageContext(contextOut);

    HttpClient httpClient = mock(HttpClient.class);
    ClassicHttpResponse mockResponse = mockResponse();
    when(httpClient.executeOpen(isNull(HttpHost.class), any(ClassicHttpRequest.class), any(HttpContext.class)))
        .thenReturn(mockResponse);

    AbstractPipelineHttpSOAPClient soapClient = AssertionRenewalUtils.createRenewalSoapClient(envelope, httpClient);
    soapClient.send("https://this.is.a.test.url", context);

    return context;
  }
}
