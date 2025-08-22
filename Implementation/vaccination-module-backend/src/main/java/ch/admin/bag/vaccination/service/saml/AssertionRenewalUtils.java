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

import static ch.admin.bag.vaccination.service.saml.SAMLUtils.buildSAMLObject;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.xml.namespace.QName;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.xml.QNameSupport;
import net.shibboleth.shared.xml.SerializeSupport;
import org.apache.commons.lang3.Validate;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.ByteArrayEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpResponse;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.decoder.MessageDecodingException;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.messaging.handler.AbstractMessageHandler;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.messaging.pipeline.httpclient.BasicHttpClientMessagePipeline;
import org.opensaml.messaging.pipeline.httpclient.HttpClientMessagePipeline;
import org.opensaml.saml.common.SAMLObject;
import org.opensaml.saml.saml2.binding.decoding.impl.HttpClientResponseSOAP11Decoder;
import org.opensaml.saml.saml2.binding.encoding.impl.HttpClientRequestSOAP11Encoder;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.soap.client.http.AbstractPipelineHttpSOAPClient;
import org.opensaml.soap.common.SOAPException;
import org.opensaml.soap.messaging.context.SOAP11Context;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.soap11.Header;
import org.opensaml.soap.soap11.impl.BodyBuilder;
import org.opensaml.soap.soap11.impl.EnvelopeBuilder;
import org.opensaml.soap.soap11.impl.HeaderBuilder;
import org.opensaml.soap.util.SOAPSupport;
import org.opensaml.soap.wssecurity.BinarySecurityToken;
import org.opensaml.soap.wssecurity.Created;
import org.opensaml.soap.wssecurity.Expires;
import org.opensaml.soap.wssecurity.Security;
import org.opensaml.soap.wssecurity.SecurityTokenReference;
import org.opensaml.soap.wssecurity.Timestamp;
import org.opensaml.soap.wssecurity.WSSecurityConstants;
import org.opensaml.soap.wssecurity.impl.CreatedBuilder;
import org.opensaml.soap.wssecurity.impl.ExpiresBuilder;
import org.opensaml.soap.wssecurity.impl.TimestampBuilder;
import org.opensaml.soap.wstrust.RenewTarget;
import org.opensaml.soap.wstrust.Renewing;
import org.opensaml.soap.wstrust.RequestSecurityToken;
import org.opensaml.soap.wstrust.RequestType;
import org.opensaml.soap.wstrust.TokenType;
import org.opensaml.soap.wstrust.WSTrustConstants;
import org.opensaml.soap.wstrust.impl.RenewTargetBuilder;
import org.opensaml.soap.wstrust.impl.RenewingBuilder;
import org.opensaml.soap.wstrust.impl.RequestTypeBuilder;
import org.opensaml.soap.wstrust.impl.TokenTypeBuilder;
import org.opensaml.xmlsec.keyinfo.KeyInfoSupport;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.X509IssuerName;
import org.opensaml.xmlsec.signature.X509IssuerSerial;
import org.opensaml.xmlsec.signature.X509SerialNumber;
import org.opensaml.xmlsec.signature.support.SignatureConstants;

/**
 * The Assertion renewal is an EPD extension to SAML, i.e. there is no particular framework which
 * implements it. Therefor, it is a lot of manual work. Thanks to our colleagues with SwissSign, we
 * could adapt our implementation.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class AssertionRenewalUtils {
  private static final int EPD_MESSAGE_VALIDITY_IN_MIN = 5;

  private static final String ID_ATTRIBUTE = "Id";
  private static final String TOKEN_TYPE = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
  private static final String RENEW_REQUEST_TYPE = WSTrustConstants.WST_NS + "/Renew";
  private static final QName BST_VALUE_TYPE =
      QNameSupport.constructQName("", BinarySecurityToken.VALUE_TYPE_ATTRIB_NAME, "");
  private static final QName WSU_ID_NAME =
      QNameSupport.constructQName(WSSecurityConstants.WSU_NS, ID_ATTRIBUTE, WSSecurityConstants.WSU_PREFIX);

  public static Envelope createRenewalRequest(Assertion assertion, BasicX509Credential spCredential) {
    try {
      X509Certificate spCertificate = spCredential.getEntityCertificate();
      Signature signature = createSignature(spCertificate);

      Security security = createSecurity(spCertificate, signature);
      RequestSecurityToken requestSecurityToken = createRequestSecurityToken(assertion);

      String bodyId = SAMLUtils.generateSecureRandomId();
      Envelope request = toSOAPRequest(security, requestSecurityToken, bodyId);
      request = SignatureUtils.signEnvelope(request, spCertificate, spCredential.getPrivateKey());
      SignatureUtils.verifyEnvelopeSignature(request, spCredential.getPublicKey());

      log.debug("CreateRenewalRequest: Renewal request creation was successful");
      return request;
    } catch (Exception e) {
      throw new RuntimeException("Renewal request creation has failed", e);
    }
  }

  /**
   * Creates a soap client which is not only allowing {@link SAMLObject} extensions to be received.
   * Additionally, it ignores encoder but instead injects a given request which is to be sent. This is
   * necessary for the idp renewal request because signature here is (other than the default
   * implementation) added to the header and not to the body!
   *
   * @param request envelope request which is send independently of what the encoder encodes.
   * @param httpClient {@link HttpClient} to be used for transmission.
   * @return ready to use soap client.
   */
  public static AbstractPipelineHttpSOAPClient createRenewalSoapClient(Envelope request, HttpClient httpClient) {
    AbstractPipelineHttpSOAPClient soapClient = new AbstractPipelineHttpSOAPClient() {

      @Override
      protected HttpClientMessagePipeline newPipeline() throws SOAPException {
        return createRenewalPipeline(request);
      }
    };

    soapClient.setHttpClient(httpClient);
    return soapClient;
  }

  private static BinarySecurityToken createBinarySecurityToken(String id, String value) {
    BinarySecurityToken bst = buildSAMLObject(BinarySecurityToken.class);
    bst.setWSUId(id);
    bst.setValueType(WSSecurityConstants.X509_V3);
    bst.setValue(value);

    // there is a marshalling bug that's why we add this attribute manually!
    bst.getUnknownAttributes().put(BST_VALUE_TYPE, WSSecurityConstants.X509_V3);
    return bst;
  }

  private static HttpClientResponseSOAP11Decoder createDecoder() {
    HttpClientResponseSOAP11Decoder decoder = new HttpClientResponseSOAP11Decoder() {
      @Override
      protected void doDecode() throws MessageDecodingException {
        try {
          super.doDecode();
        } catch (MessageDecodingException ex) {
          MessageContext messageContext = getMessageContext();
          if (messageContext == null || !(messageContext.getMessage() instanceof XMLObject)) {
            getResponseInformation(ex);
            throw ex;
          }
        }
      }

      private void getResponseInformation(MessageDecodingException ex) {
        if (log.isDebugEnabled()) {
          BasicClassicHttpResponse response = (BasicClassicHttpResponse)this.getHttpResponse();
          log.debug("Exception occured during response decoding.");
          if (response != null) {
            log.debug("Http response code: {}", response.getCode());
            log.debug("Http response reason: {}", response.getReasonPhrase());
            HttpEntity entity = response.getEntity();
            if (entity != null) {
              log.debug("Content-Type: {}, Content-Length: {}", entity.getContentType(), entity.getContentLength());
              log.debug(
                  "To debug message content, activate log level TRACE on org.opensaml.core.xml.util.XMLObjectSupport");
            }
          }
        }
      }
    };

    // replace default message handler due to restriction
    // that response needs to extend SAML Object class
    decoder.setBodyHandler(new AbstractMessageHandler() {

      @Override
      protected void doInvoke(MessageContext messageContext) throws MessageHandlerException {
        SOAP11Context soap11Context = messageContext.getSubcontext(SOAP11Context.class);
        if (soap11Context == null) {
          throw new MessageHandlerException("SOAP 1.1 context was not present in message context");
        }
        Envelope soapMessage = soap11Context.getEnvelope();
        if (soapMessage == null) {
          throw new MessageHandlerException("SOAP 1.1 envelope was not present in SOAP context");
        }

        List<XMLObject> soapBodyChildren = soapMessage.getBody().getUnknownXMLObjects();
        if (soapBodyChildren.size() != 1) {
          log.error("Unexpected number of children in the SOAP body, " + soapBodyChildren.size()
              + ".  Unable to extract SAML message");
          throw new MessageHandlerException(
              "Unexpected number of children in the SOAP body, unable to extract SAML message");
        }

        XMLObject incomingMessage = soapBodyChildren.getFirst();
        messageContext.setMessage(incomingMessage);
      }
    });

    return decoder;
  }

  private static HttpClientRequestSOAP11Encoder createEncoder(XMLObject request) {
    return new HttpClientRequestSOAP11Encoder() {

      @Override
      protected HttpEntity createRequestEntity(Envelope message, Charset charset) throws MessageEncodingException {
        try {
          final ByteArrayOutputStream arrayOut = new ByteArrayOutputStream();
          SerializeSupport.writeNode(XMLObjectSupport.marshall(request), arrayOut);
          return new ByteArrayEntity(arrayOut.toByteArray(), ContentType.create("text/xml", charset));
        } catch (final MarshallingException e) {
          throw new MessageEncodingException("Unable to marshall SOAP envelope", e);
        }
      }
    };
  }

  private static KeyInfo createKeyInfo(String keyInfoId, String securityTokenReferenceId,
      X509Certificate spCertificate) throws CertificateException {
    String x509IssuerName = spCertificate.getIssuerX500Principal().getName();
    BigInteger x509SerialNumber = spCertificate.getSerialNumber();
    org.opensaml.xmlsec.signature.X509Certificate xmlCert = KeyInfoSupport.buildX509Certificate(spCertificate);

    X509IssuerName issuerName = buildSAMLObject(X509IssuerName.class);
    issuerName.setValue(x509IssuerName);

    X509SerialNumber serialNumber = buildSAMLObject(X509SerialNumber.class);
    serialNumber.setValue(x509SerialNumber);

    X509IssuerSerial x509IssuerSerial = buildSAMLObject(X509IssuerSerial.class);
    x509IssuerSerial.setX509IssuerName(issuerName);
    x509IssuerSerial.setX509SerialNumber(serialNumber);

    X509Data x509Data = buildSAMLObject(X509Data.class);
    x509Data.getX509IssuerSerials().add(x509IssuerSerial);
    x509Data.getX509Certificates().add(xmlCert);

    SecurityTokenReference ref = buildSAMLObject(SecurityTokenReference.class);
    ref.setWSUId(securityTokenReferenceId);
    ref.getUnknownXMLObjects().add(x509Data);

    KeyInfo keyInfo = buildSAMLObject(KeyInfo.class);
    keyInfo.setID(keyInfoId);
    keyInfo.getXMLObjects().add(ref);

    return keyInfo;
  }

  private static HttpClientMessagePipeline createRenewalPipeline(Envelope request) {
    HttpClientRequestSOAP11Encoder encoder = createEncoder(request);
    HttpClientResponseSOAP11Decoder decoder = createDecoder();
    try {
      decoder.getBodyHandler().initialize();
    } catch (ComponentInitializationException e) {
      throw new RuntimeException(e);
    }

    return new BasicHttpClientMessagePipeline(encoder, decoder);
  }

  private static RequestSecurityToken createRequestSecurityToken(Assertion assertion) {
    Validate.notNull(assertion, "assertion must not be null");

    RequestType type = new RequestTypeBuilder().buildObject();
    type.setURI(RENEW_REQUEST_TYPE);

    TokenType tokenType = new TokenTypeBuilder().buildObject();
    tokenType.setURI(TOKEN_TYPE);

    RenewTarget renewTarget = new RenewTargetBuilder().buildObject();
    renewTarget.setUnknownXMLObject(assertion);
    Renewing renewing = new RenewingBuilder().buildObject();

    RequestSecurityToken rst = SAMLUtils.buildSAMLObject(RequestSecurityToken.class);
    rst.getUnknownXMLObjects().add(type);
    rst.getUnknownXMLObjects().add(tokenType);
    rst.getUnknownXMLObjects().add(renewTarget);
    rst.getUnknownXMLObjects().add(renewing);

    return rst;
  }

  private static Security createSecurity(X509Certificate spCertificate, Signature signature)
      throws CertificateException {
    Validate.notNull(spCertificate, "certificate must not be null.");
    Validate.notNull(signature, "signature must not be null");

    String timestampId = SAMLUtils.generateSecureRandomId();
    String binarySecurityTokenId = SAMLUtils.generateSecureRandomId();
    String binarySecurityTokenValue = Base64.getEncoder().encodeToString(spCertificate.getEncoded());

    Timestamp timestamp = createTimestamp(timestampId);
    BinarySecurityToken token = createBinarySecurityToken(binarySecurityTokenId, binarySecurityTokenValue);

    Security security = buildSAMLObject(Security.class);
    SOAPSupport.addSOAP11MustUnderstandAttribute(security, true);
    security.getUnknownXMLObjects().add(timestamp);
    security.getUnknownXMLObjects().add(token);
    security.getUnknownXMLObjects().add(signature);

    return security;
  }

  private static Signature createSignature(X509Certificate spCertificate) throws CertificateException {
    Validate.notNull(spCertificate, "certificate must not be null.");

    String keyInfoId = SAMLUtils.generateSecureRandomId();
    String securityTokenReferenceId = SAMLUtils.generateSecureRandomId();
    KeyInfo keyInfo = createKeyInfo(keyInfoId, securityTokenReferenceId, spCertificate);

    Signature signature = buildSAMLObject(Signature.class);
    signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
    signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
    signature.setKeyInfo(keyInfo);

    return signature;
  }

  private static Timestamp createTimestamp(String id) {
    Timestamp timestamp = new TimestampBuilder().buildObject();
    Created created = new CreatedBuilder().buildObject();
    created.setDateTime(Instant.now());
    Expires expires = new ExpiresBuilder().buildObject();
    expires.setDateTime(Instant.now().plusSeconds(EPD_MESSAGE_VALIDITY_IN_MIN * 60));
    timestamp.setWSUId(id);
    timestamp.setCreated(created);
    timestamp.setExpires(expires);

    return timestamp;
  }

  private static Envelope toSOAPMessage(XMLObject headerContent, XMLObject bodyContent, String bodyId) {
    Envelope envelope = new EnvelopeBuilder().buildObject();
    if (headerContent != null) {
      Header header = new HeaderBuilder().buildObject();
      header.getUnknownXMLObjects().add(headerContent);
      envelope.setHeader(header);
    }

    Body body = new BodyBuilder().buildObject();
    body.getUnknownXMLObjects().add(bodyContent);

    if (bodyId != null) {
      body.getUnknownAttributes().put(WSU_ID_NAME, bodyId);
    }

    envelope.setBody(body);
    return envelope;
  }

  private static Envelope toSOAPRequest(Security security, RequestSecurityToken request, String bodyId) {
    Validate.notNull(security, "security must not be null");
    Validate.notNull(request, "RequestSecurityToken must not be null");
    Validate.notEmpty(bodyId, "RequestSecurityToken must not be empty");

    return toSOAPMessage(security, request, bodyId);
  }
}
