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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.time.Instant;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.AuthnContext;
import org.opensaml.saml.saml2.core.AuthnContextClassRef;
import org.opensaml.saml.saml2.core.AuthnStatement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * Utility class to manipulate SAML-xml and Assertion xml items
 */
@Slf4j
public class SAMLXmlTestUtils {

  static public Assertion createAssertion(String xml) throws Exception {
    Attribute attribute = mock(Attribute.class);
    AttributeStatement attrStatement = mock(AttributeStatement.class);
    when(attrStatement.getAttributes()).thenReturn(List.of(attribute));

    AuthnContextClassRef authncontextClassRef = mock(AuthnContextClassRef.class);
    when(authncontextClassRef.getURI()).thenReturn("URI");

    AuthnContext authNContext = mock(AuthnContext.class);
    when(authNContext.getAuthnContextClassRef()).thenReturn(authncontextClassRef);

    AuthnStatement authStatement = mock(AuthnStatement.class);
    when(authStatement.getAuthnContext()).thenReturn(authNContext);

    Element assertionElement = createXMLElementFromFile(xml);
    Assertion assertion = mock(Assertion.class);
    when(assertion.getAttributeStatements()).thenReturn(List.of(attrStatement));
    when(assertion.getAuthnStatements()).thenReturn(List.of(authStatement));
    when(assertion.getSchemaType()).thenReturn(Assertion.TYPE_NAME);
    when(assertion.getDOM()).thenReturn(assertionElement);
    when(assertion.getIssueInstant()).thenReturn(Instant.now());

    return assertion;
  }

  public static Element createXMLElementFromFile(String path) throws Exception {
    ClassLoader classLoader = SAMLXmlTestUtils.class.getClassLoader();
    File file = new File(classLoader.getResource(path).getFile());
    DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
    DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
    Document document = dBuilder.parse(new InputSource(new FileReader(file)));

    return document.getDocumentElement();
  }

  public static String xml(String path) {
    try {
      ClassLoader classLoader = SAMLUtils.class.getClassLoader();
      File file = new File(classLoader.getResource(path).getFile());
      return Files.readString(file.toPath());
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return "";
    }
  }
}
