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

import static ch.admin.bag.vaccination.service.saml.SAMLUtils.convertElementToXMLObject;
import static ch.admin.bag.vaccination.service.saml.SAMLUtils.convertXMLObjectToElement;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.wssecurity.WSSecurityConstants;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Different signature algorithms due to EPD extension. Having signature in the header is definitely
 * not standard.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignatureUtils {
  private static final String CERTIFICATE_MUST_NOT_BE_NULL = "certificate must not be null";
  private static final String SIGNATURE_INSTANCE_TYPE = "DOM";
  private static final String NAMESPACE_PREFIX = "ds";
  private static final String ID_ATTRIBUTE = "ID";
  private static final String WSU_ID_ATTRIBUTE = "Id";
  private static final String REF_ID_PREFIX = "#";
  private static final String XPATH_BODY_EXPR = "//*[local-name()='Envelope']//*[local-name()='Body']";
  private static final String XPATH_HEADER_EXPR = "//*[local-name()='Envelope']//*[local-name()='Header']";
  private static final String XPATH_SECURITY_EXPR = XPATH_HEADER_EXPR + "//*[local-name()='Security']";
  private static final String XPATH_TIMESTAMP_EXPR = XPATH_SECURITY_EXPR + "//*[local-name()='Timestamp']";
  private static final String XPATH_SIGNATURE_EXPR = "//*[local-name()='Envelope']//*[local-name()='Signature']";
  private static final String ENVELOPE_MUST_NOT_BE_NULL = "envelope must not be null";

  /**
   * Signes an envelope - please not that the signature is put in the header unlike regular signatures
   * according to opensaml.
   *
   * @param envelope {@link Envelope}
   * @param spCertificate
   * @param privateKey private the of the sender.
   * @return
   * @throws XMLSignatureException
   */
  public static Envelope signEnvelope(Envelope envelope, X509Certificate spCertificate, PrivateKey privateKey)
      throws XMLSignatureException {
    Validate.notNull(envelope, ENVELOPE_MUST_NOT_BE_NULL);

    try {
      Element element = convertXMLObjectToElement(envelope, Envelope.class);
      signEnvelope(element, spCertificate, privateKey, true);
      return (Envelope) convertElementToXMLObject(element, Envelope.class);
    } catch (MarshallingException | UnmarshallingException | MarshalException | XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Verifies the signature of an envelope. Note that the signature is provided in the header
   * information.
   *
   * @param envelope {@link Envelope}
   * @param publicKey public key of the sender.
   * @throws XMLSignatureException
   */
  public static void verifyEnvelopeSignature(Envelope envelope, PublicKey publicKey) throws XMLSignatureException {
    Validate.notNull(envelope, ENVELOPE_MUST_NOT_BE_NULL);
    try {
      Element element = convertXMLObjectToElement(envelope, Envelope.class);
      SignatureUtils.verifyEnvelopeSignature(element, publicKey);
      log.debug("Envelop has a valid signature.");
    } catch (MarshallingException | MarshalException | XPathExpressionException e) {
      throw new RuntimeException(e);
    }
  }

  private static void configureIdAttribute(Element node, boolean isWsuId) {
    if (isWsuId) {
      node.setIdAttributeNS(WSSecurityConstants.WSU_NS, WSU_ID_ATTRIBUTE, true);
    } else {
      node.setIdAttribute(ID_ATTRIBUTE, true);
    }
  }

  private static DOMSignContext createDomSignContext(PrivateKey privateKey, Element nodeToSign, Node insertionNode) {
    DOMSignContext context;

    if (insertionNode == null) {
      context = new DOMSignContext(privateKey, nodeToSign);
    } else {
      context = new DOMSignContext(privateKey, nodeToSign, insertionNode);
    }

    context.setDefaultNamespacePrefix(NAMESPACE_PREFIX);

    return context;
  }

  private static KeyInfo createKeyInfo(X509Certificate certificate, XMLSignatureFactory factory) {
    Validate.notNull(certificate, CERTIFICATE_MUST_NOT_BE_NULL);
    KeyInfoFactory kiFactory = factory.getKeyInfoFactory();

    List<Object> content = new ArrayList<>();
    content.add(certificate);
    X509Data data = kiFactory.newX509Data(content);

    return kiFactory.newKeyInfo(Collections.singletonList(data));
  }

  private static SignedInfo createSignedInfo(String timestampId, String bodyId, XMLSignatureFactory factory) {
    try {
      // Create a CanonicalizationMethod which specify how the XML will be canonicalized before signed.
      CanonicalizationMethod canonicalizationMethod =
          factory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, (C14NMethodParameterSpec) null);

      // Create a SignatureMethod which specify how the XML will be signed.
      SignatureMethod signatureMethod = factory.newSignatureMethod(SignatureMethod.RSA_SHA256, null);

      // Create an Array of Transform, add it one Transform which specify the Signature ENVELOPED method.
      Transform transform = factory.newTransform(CanonicalizationMethod.EXCLUSIVE, (TransformParameterSpec) null);
      List<Transform> transformList = new ArrayList<>(1);
      transformList.add(transform);

      // Create a Reference which contain: An URI to the Assertion ID, the Digest Method and the Transform
      // List which specify the Signature ENVELOPED method.
      Reference timestampRef = factory
          .newReference(REF_ID_PREFIX + timestampId, factory.newDigestMethod(DigestMethod.SHA256, null), transformList,
              null, null);
      Reference bodyRef = factory.newReference(REF_ID_PREFIX + bodyId,
          factory.newDigestMethod(DigestMethod.SHA256, null), transformList, null, null);
      List<Reference> references = new ArrayList<>();
      references.add(timestampRef);
      references.add(bodyRef);

      // Create a SignedInfo with the pre-specified: Canonicalization Method, Signature Method and List of
      // References.
      return factory.newSignedInfo(canonicalizationMethod, signatureMethod, references);
    } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException(e);
    }
  }

  private static XPath createXPath() {
    return XPathFactory.newInstance().newXPath();
  }

  private static String findId(Element node, boolean isWsuId) {
    if (isWsuId) {
      return node.getAttributeNodeNS(WSSecurityConstants.WSU_NS, WSU_ID_ATTRIBUTE).getValue();
    }

    return node.getAttribute(ID_ATTRIBUTE);
  }

  private static void fixEnvelopeStructure(Element envelope) throws XPathExpressionException {
    Node correctKeyInfo =
        searchNode(envelope, XPATH_SECURITY_EXPR + "//*[local-name()='Signature']//*[local-name()='KeyInfo']");
    Node correctSignature = searchNode(envelope, XPATH_SIGNATURE_EXPR);

    Node wrongSignature = searchNode(envelope, XPATH_SECURITY_EXPR + "//*[local-name()='Signature']");
    Node wrongKeyInfo = searchNode(envelope, XPATH_SIGNATURE_EXPR + "//*[local-name()='KeyInfo']");

    correctSignature = envelope.removeChild(correctSignature);
    correctSignature.replaceChild(correctKeyInfo, wrongKeyInfo);

    Node security = searchNode(envelope, XPATH_SECURITY_EXPR);
    security.replaceChild(correctSignature, wrongSignature);
  }

  private static Node searchNode(Node node, String xpathExpression) throws XPathExpressionException {
    XPathExpression expression = createXPath().compile(xpathExpression);
    return (Node) expression.evaluate(node, XPathConstants.NODE);
  }

  private static void signEnvelope(Element envelope, X509Certificate certificate, PrivateKey privateKey,
      boolean isWsuId)
      throws MarshalException, XMLSignatureException, XPathExpressionException {
    Validate.notNull(envelope, ENVELOPE_MUST_NOT_BE_NULL);
    Validate.notNull(privateKey, "privateKey must not be null");

    Element body = (Element) searchNode(envelope, XPATH_BODY_EXPR);
    // configure what an id attribute is, otherwise search methods find nothing!!!
    configureIdAttribute(body, isWsuId);

    Element timestamp = (Element) searchNode(envelope, XPATH_TIMESTAMP_EXPR);
    configureIdAttribute(timestamp, isWsuId);

    Element header = (Element) searchNode(envelope, XPATH_HEADER_EXPR);

    String bodyId = findId(body, isWsuId);
    String timestampId = findId(timestamp, isWsuId);

    XMLSignatureFactory factory = XMLSignatureFactory.getInstance(SIGNATURE_INSTANCE_TYPE);

    DOMSignContext context = createDomSignContext(privateKey, envelope, header);
    SignedInfo signedInfo = createSignedInfo(timestampId, bodyId, factory);
    KeyInfo keyInfo = createKeyInfo(certificate, factory);

    XMLSignature signature = factory.newXMLSignature(signedInfo, keyInfo);
    signature.sign(context);

    fixEnvelopeStructure(envelope);
  }

  private static void verifyEnvelopeSignature(Element envelope, PublicKey publicKey)
      throws MarshalException, XMLSignatureException, XPathExpressionException {
    Validate.notNull(envelope, ENVELOPE_MUST_NOT_BE_NULL);
    Validate.notNull(publicKey, "publicKey must not be null");

    Node sigNode = searchNode(envelope, XPATH_SECURITY_EXPR + "//*[local-name()='Signature']");
    Element body = (Element) searchNode(envelope, XPATH_BODY_EXPR);
    // configure what an id attribute is, otherwise search methods find nothing!!!
    configureIdAttribute(body, true);

    Element timestamp = (Element) searchNode(envelope, XPATH_TIMESTAMP_EXPR);
    configureIdAttribute(timestamp, true);

    DOMValidateContext context = new DOMValidateContext(publicKey, sigNode);
    XMLSignatureFactory factory = XMLSignatureFactory.getInstance(SIGNATURE_INSTANCE_TYPE);
    XMLSignature signature = factory.unmarshalXMLSignature(context);
    boolean valid = signature.validate(context);

    if (!valid) {
      throw new XMLSignatureException("Found Signature is not valid");
    }
  }
}
