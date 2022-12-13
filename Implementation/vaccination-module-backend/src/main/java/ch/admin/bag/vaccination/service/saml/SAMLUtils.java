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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.security.impl.RandomIdentifierGenerationStrategy;
import net.shibboleth.utilities.java.support.xml.SerializeSupport;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.saml.common.SignableSAMLObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Taken from https://github.com/rasmusson/OpenSAML-sample-code/
 *
 * <p>
 * Helps to create and log SAML requests.
 * </p>
 */
@Slf4j
public class SAMLUtils {
  private static RandomIdentifierGenerationStrategy secureRandomIdGenerator;

  static {
    secureRandomIdGenerator = new RandomIdentifierGenerationStrategy();
  }

  @SuppressWarnings("unchecked")
  public static <T> T buildSAMLObject(final Class<T> clazz) {
    T object = null;
    try {
      XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
      QName defaultElementName = (QName) clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
      object = (T) builderFactory.getBuilder(defaultElementName).buildObject(defaultElementName);
    } catch (IllegalAccessException | NoSuchFieldException ex) {
      throw new IllegalArgumentException("Could not create SAML object");
    }

    return object;
  }

  public static String generateSecureRandomId() {
    return secureRandomIdGenerator.generateIdentifier();
  }

  public static String getSamlMessageAsString(XMLObject object) {
    Element element = null;

    if (object instanceof SignableSAMLObject
        && ((SignableSAMLObject) object).isSigned()
        && object.getDOM() != null) {
      element = object.getDOM();
    } else {
      try {
        Marshaller out =
            XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(object);
        out.marshall(object);
        element = object.getDOM();
      } catch (MarshallingException e) {
        log.error(e.getMessage(), e);
      }
    }

    return SerializeSupport.prettyPrintXML(element);
  }

  public static void logSAMLObject(XMLObject object) {
    log.debug(getSamlMessageAsString(object));
  }

  public static XMLObject unmarshall(String xmlString) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setNamespaceAware(true);
      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));

      Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport
          .getUnmarshallerFactory()
          .getUnmarshaller(doc.getDocumentElement());
      XMLObject xmlObject = unmarshaller.unmarshall(doc.getDocumentElement());

      return xmlObject;
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return null;
    }
  }

  public static String xml(String path) {
    try {
      ClassLoader classLoader = SAMLUtils.class.getClassLoader();
      File file = new File(classLoader.getResource(path).getFile());
      return Files.readString(file.toPath());
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return null;
    }
  }
}
