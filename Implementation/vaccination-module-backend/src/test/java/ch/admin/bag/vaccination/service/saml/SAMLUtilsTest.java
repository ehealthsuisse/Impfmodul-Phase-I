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
package ch.admin.bag.vaccination.service.saml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 *
 * Test the {@link SAMLUtils} utility class
 *
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class SAMLUtilsTest {

  public static String replaceInstantByNow(String xml) {
    Instant now = Instant.now();
    return xml.replace("2014-07-18T01:13:06Z", now.toString());
  }

  @Test
  public void replaceInstantByNow() {
    Instant now = Instant.now();
    now = now.minusMillis(1000);
    String xml = SAMLXmlTestUtils.xml("saml/samlLogoutRequest.xml");
    xml = SAMLUtilsTest.replaceInstantByNow(xml);
    LogoutRequest logoutRequest = (LogoutRequest) SAMLUtils.unmarshall(xml);
    assertThat(logoutRequest.getNameID().getValue()).isEqualTo("remery");
    assertThat(logoutRequest.getIssuer().getValue()).isEqualTo("http://sp.example.com/demo1/metadata.php");
    assertThat(logoutRequest.getIssueInstant()).isAfter(now);
  }

  @Test
  public void unmarshall_logoutRequest() {
    LogoutRequest logoutRequest =
        (LogoutRequest) SAMLUtils.unmarshall(SAMLXmlTestUtils.xml("saml/samlLogoutRequest.xml"));
    assertThat(logoutRequest.getNameID().getValue()).isEqualTo("remery");
    assertThat(logoutRequest.getIssuer().getValue()).isEqualTo("http://sp.example.com/demo1/metadata.php");
    assertThat(logoutRequest.getIssueInstant().toString()).isEqualTo("2014-07-18T01:13:06Z");
  }

  @Test
  void testAddEnvelope_removesXmlDeclaration() {
    String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root><child>value</child></root>";
    String expected =
        """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
           <soapenv:Header/>
           <soapenv:Body><root><child>value</child></root></soapenv:Body>
        </soapenv:Envelope>""";
    assertEquals(expected, SAMLUtils.addEnvelope(input));
  }

  @Test
  void testAddEnvelope_withWhitespaceAfterDeclaration() {
    String input = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>   \n\n<root/>";
    String expected =
        """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
           <soapenv:Header/>
           <soapenv:Body><root/></soapenv:Body>
        </soapenv:Envelope>""";
    assertEquals(expected, SAMLUtils.addEnvelope(input));
  }
}
