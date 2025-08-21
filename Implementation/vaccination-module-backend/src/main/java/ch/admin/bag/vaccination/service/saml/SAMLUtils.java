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

import ch.admin.bag.vaccination.controller.AssertionUtils;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.shared.component.ComponentInitializationException;
import net.shibboleth.shared.security.impl.RandomIdentifierGenerationStrategy;
import net.shibboleth.shared.xml.SerializeSupport;
import org.apache.commons.lang3.Validate;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.binding.security.impl.MessageLifetimeSecurityHandler;
import org.opensaml.saml.common.messaging.context.SAMLMessageInfoContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.impl.StatusBuilder;
import org.opensaml.saml.saml2.core.impl.StatusCodeBuilder;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.soap.wstrust.RequestSecurityTokenResponse;
import org.opensaml.soap.wstrust.RequestedSecurityToken;
import org.projecthusky.xua.communication.xua.XUserAssertionResponse;
import org.projecthusky.xua.communication.xua.impl.XUserAssertionResponseImpl;
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
  private static final String OBJECT_MUST_NOT_BE_NULL = "object must not be null";
  private static final String CLAZZ_MUST_NOT_BE_NULL = "clazz must not be null";
  private static final String ELEMENT_MUST_NOT_BE_NULL = "element must not be null";

  static {
    secureRandomIdGenerator = new RandomIdentifierGenerationStrategy();
  }

  /**
   * Wraps a given XML request body in a SOAP envelope.
   *
   * <p>This method removes the XML declaration (e.g., {@code <?xml version="1.0" encoding="UTF-8"?>})
   * from the beginning of the input string, if present, and surrounds the resulting content with a
   * SOAP 1.1 envelope containing an empty header and a body that includes the original request.
   *
   * <p>The XML declaration is removed using a regular expression:
   * <ul>
   *   <li>{@code ^} ensures it only matches at the beginning of the string.</li>
   *   <li>{@code \\s*} matches any number of whitespace characters (including {@code \n}, {@code \r}, tabs, etc.).</li>
   *   <li>{@code replaceFirst} is used because the XML declaration should appear only once at the top.</li>
   * </ul>
   * @param request the raw XML request string (optionally starting with an XML declaration)
   * @return the input XML wrapped inside a SOAP envelope
   */
  public static String addEnvelope(String request) {
    request = request.replaceFirst("^<\\?xml version=\"1\\.0\" encoding=\"UTF-8\"\\?>\\s*", "");
    String open = """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
           <soapenv:Header/>
           <soapenv:Body>""";
    String close = """
        </soapenv:Body>
        </soapenv:Envelope>""";
    return open + request + close;
  }

  /**
   * Builds a Artifact message from a saml artifact.
   *
   * @param samlArtifact one-time-token
   * @return {@link Artifact}i
   */
  public static Artifact buildArtifactFromRequest(String samlArtifact) {
    Artifact artifact = SAMLUtils.buildSAMLObject(Artifact.class);
    artifact.setValue(samlArtifact);
    return artifact;
  }

  @SuppressWarnings("unchecked")
  public static <T> T buildSAMLObject(final Class<T> clazz) {
    Validate.notNull(clazz, CLAZZ_MUST_NOT_BE_NULL);
    XMLObjectBuilderFactory builderFactory = XMLObjectProviderRegistrySupport.getBuilderFactory();
    QName elementName = findQName(clazz);

    return (T) builderFactory.getBuilder(elementName).buildObject(elementName);
  }

  public static String convertElementToString(XMLObject logoutResponse)
      throws MarshallingException {
    Marshaller marshaller = org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport
        .getMarshallerFactory().getMarshaller(logoutResponse);
    Element element = marshaller.marshall(logoutResponse);

    return SerializeSupport.prettyPrintXML(element);
  }

  public static XMLObject convertElementToXMLObject(final Element element, final Class<?> clazz)
      throws UnmarshallingException {
    Validate.notNull(element, ELEMENT_MUST_NOT_BE_NULL);
    Validate.notNull(clazz, CLAZZ_MUST_NOT_BE_NULL);

    QName elementName = findQName(clazz);
    Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory().getUnmarshaller(elementName);

    if (unmarshaller == null) {
      throw new UnmarshallingException("no unmarshaller found for class " + clazz.getSimpleName());
    }

    return unmarshaller.unmarshall(element);
  }

  /**
   * Converts the xml object to string.
   *
   * @param object any saml xml object
   * @return {@link String}
   */
  public static String convertSamlMessageToString(XMLObject object) {
    if (object == null) {
      log.warn("SAML object cannot be converted as it is null.");
      return "Undefined";
    }

    Element element = null;
    if (object instanceof SignableSAMLObject signableObject
        && signableObject.isSigned()
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

  public static Element convertXMLObjectToElement(XMLObject object, Class<?> clazz)
      throws MarshallingException {
    Validate.notNull(object, OBJECT_MUST_NOT_BE_NULL);
    Validate.notNull(clazz, CLAZZ_MUST_NOT_BE_NULL);

    QName elementName = findQName(clazz);
    Marshaller marshaller = XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(elementName);

    if (marshaller == null) {
      throw new MarshallingException("no marshaller found for class " + clazz.getSimpleName());
    }

    return marshaller.marshall(object);
  }

  /**
   * Builds {@link Endpoint} for authnrequest.
   *
   * @param authnrequestURL request url for authnrequest
   * @return {@link Endpoint}
   */
  public static Endpoint createEndpoint(String authnrequestURL) {
    Endpoint endpoint = buildSAMLObject(SingleSignOnService.class);
    endpoint.setBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
    endpoint.setLocation(authnrequestURL);
    return endpoint;
  }

  /**
   * Builds an {@link ArtifactResolve}Request
   *
   * @param spEntityId unique id of the service provider
   * @param artifactResolutionServiceURL url to resolve the assertion of the IDP
   * @param artifact {@link Artifact}
   * @return {@link ArtifactResolve}
   */
  public static ArtifactResolve createUnsignedArtifactResolveRequest(String spEntityId,
      String artifactResolutionServiceURL, Artifact artifact) {
    ArtifactResolve artifactResolve = SAMLUtils.buildSAMLObject(ArtifactResolve.class);

    Issuer issuer = SAMLUtils.buildSAMLObject(Issuer.class);
    issuer.setValue(spEntityId);
    artifactResolve.setIssuer(issuer);
    artifactResolve.setIssueInstant(Instant.now());
    artifactResolve.setID(SAMLUtils.generateSecureRandomId());
    artifactResolve.setDestination(artifactResolutionServiceURL);
    artifactResolve.setArtifact(artifact);

    return artifactResolve;
  }

  /**
   * Creates the {@link AuthnRequest} to forward the client to the IDP
   *
   * @param spEntityId unique identity of the service provider
   * @param requestUrl url of the IDP for authnrequests.
   * @param assertionConsumerServiceUrl url on service provider side to receive the saml artifact.
   * @return {@link AuthnRequest} which still needs to be signed
   */
  public static AuthnRequest createUnsignedAuthnRequest(String spEntityId, String requestUrl,
      String assertionConsumerServiceUrl) {
    AuthnRequest authnRequest = SAMLUtils.buildSAMLObject(AuthnRequest.class);
    authnRequest.setIssueInstant(Instant.now());
    authnRequest.setDestination(requestUrl);
    authnRequest.setProtocolBinding(SAMLConstants.SAML2_ARTIFACT_BINDING_URI);
    authnRequest.setAssertionConsumerServiceURL(assertionConsumerServiceUrl);
    authnRequest.setID(SAMLUtils.generateSecureRandomId());
    authnRequest.setIssuer(buildIssuer(spEntityId));
    authnRequest.setNameIDPolicy(buildNameIdPolicy());

    return authnRequest;
  }

  public static LogoutResponse createUnsignedLogoutResponse(LogoutRequest logoutRequest, String spEntityId,
      String logoutURL) {
    LogoutResponse response = buildSAMLObject(LogoutResponse.class);
    response.setDestination(logoutURL);
    response.setInResponseTo(logoutRequest.getID());
    response.setIssueInstant(Instant.now());
    response.setIssuer(buildIssuer(spEntityId));
    response.setVersion(SAMLVersion.VERSION_20);
    response.setID(generateSecureRandomId());
    response.setStatus(buildStatus());

    return response;
  }

  public static String generateSecureRandomId() {
    return secureRandomIdGenerator.generateIdentifier();
  }

  /**
   * Returns the assertion from the {@link ArtifactResponse}
   *
   * @param artifactResponse {@link ArtifactResponse}
   * @return {@link Assertion}
   */
  public static Assertion getAssertion(ArtifactResponse artifactResponse) {
    Response response = (Response) artifactResponse.getMessage();
    if (response != null) {
      if (log.isTraceEnabled()) {
        SAMLUtils.logSAMLObject(response);
      }
      return response.getAssertions().getFirst();
    }

    String errorMsg = "Artifact response message is empty and does not contain any assertion.";
    log.error(errorMsg);
    throw new TechnicalException(errorMsg);
  }

  public static org.projecthusky.xua.saml2.Assertion getXuaAssertionFromResponse(
      List<XUserAssertionResponse> response) {
    RequestSecurityTokenResponse responseCollection = ((XUserAssertionResponseImpl) response.getFirst()).getWrappedObject();

    // copied from XUserAssertionResponseImpl, husky-xua-gen-impl
    List<XMLObject> requestedTokens = responseCollection.getUnknownXMLObjects(new QName(
        "http://docs.oasis-open.org/ws-sx/ws-trust/200512", "RequestedSecurityToken"));
    if (!requestedTokens.isEmpty()) {
      RequestedSecurityToken token = (RequestedSecurityToken) requestedTokens.getFirst();
      org.opensaml.saml.saml2.core.Assertion openSamlAssertion =
          (org.opensaml.saml.saml2.core.Assertion) token.getUnknownXMLObject();
      org.projecthusky.xua.saml2.Assertion xua = AssertionUtils.convertSamlToHuskyAssertion(openSamlAssertion);
      SAMLUtils.logSAMLObject((XMLObject) xua.getWrappedObject());
      return xua;
    }

    throw new TechnicalException("Received XUA assertion does not contain any requestSecurityTokens.");
  }

  /**
   * Logs the attributes and methods of a assertion for debugging reasons.
   *
   * @param assertion {@link Assertion}
   */
  public static void logAssertionForDebug(Assertion assertion) {
    SAMLUtils.logSAMLObject(assertion);

    logAssertionAttributes(assertion);
    logAuthenticationInstant(assertion);
    logAuthenticationMethod(assertion);
  }

  public static void logSAMLObject(XMLObject object) {
    if (log.isDebugEnabled()) {
      log.debug(convertSamlMessageToString(object));
    }
  }

  public static XMLObject unmarshall(String xmlString) {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setIgnoringComments(true);
      dbf.setNamespaceAware(true);
      dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
      dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
      dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

      DocumentBuilder db = dbf.newDocumentBuilder();
      Document doc = db.parse(new ByteArrayInputStream(xmlString.getBytes("UTF-8")));

      Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport
          .getUnmarshallerFactory()
          .getUnmarshaller(doc.getDocumentElement());
      return unmarshaller.unmarshall(doc.getDocumentElement());
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
      return null;
    }
  }

  /**
   * Validates the Response, e.g. checking lifetimes and received endpoints.
   *
   * @param artifactResponse {@link ArtifactResponse}
   * @param request {@link HttpServletRequest}
   * @param samlMessageClockSkew allowed clock skew derivation in ms
   * @param samlMessageLifetime allowed message time derivation in ms
   */
  public static void validateArtifactResponse(ArtifactResponse artifactResponse,
      HttpServletRequest request, long samlMessageLifetime, long samlMessageClockSkew) {
    MessageContext context = new MessageContext();
    context.setMessage(artifactResponse);

    SAMLMessageInfoContext messageInfoContext =
        context.getSubcontext(SAMLMessageInfoContext.class, true);
    messageInfoContext.setMessageIssueInstant(artifactResponse.getIssueInstant());

    try {
      validateMessageLifetime(context, samlMessageLifetime, samlMessageClockSkew);
    } catch (ComponentInitializationException | MessageHandlerException e) {
      throw new RuntimeException(e);
    }
  }

  private static Issuer buildIssuer(String spEntityId) {
    Issuer issuer = SAMLUtils.buildSAMLObject(Issuer.class);
    issuer.setValue(spEntityId);
    return issuer;
  }

  private static NameIDPolicy buildNameIdPolicy() {
    NameIDPolicy nameIDPolicy = SAMLUtils.buildSAMLObject(NameIDPolicy.class);
    nameIDPolicy.setAllowCreate(true);
    nameIDPolicy.setFormat(NameIDType.PERSISTENT);

    return nameIDPolicy;
  }

  private static Status buildStatus() {
    StatusCode code = new StatusCodeBuilder().buildObject();
    code.setValue(StatusCode.SUCCESS);

    Status status = new StatusBuilder().buildObject();
    status.setStatusCode(code);
    return status;
  }

  private static <T> QName findQName(final Class<T> clazz) {
    Validate.notNull(clazz, CLAZZ_MUST_NOT_BE_NULL);

    try {
      return (QName) clazz.getDeclaredField("DEFAULT_ELEMENT_NAME").get(null);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (NoSuchFieldException e) {
      try {
        return (QName) clazz.getDeclaredField("ELEMENT_NAME").get(null);
      } catch (IllegalAccessException e1) {
        throw new RuntimeException(e1);
      } catch (NoSuchFieldException e1) {
        try {
          return (QName) clazz.getDeclaredField("TYPE_NAME").get(null);
        } catch (IllegalAccessException | NoSuchFieldException e2) {
          throw new RuntimeException(e2);
        }
      }
    }
  }

  private static void logAssertionAttributes(Assertion assertion) {
    for (Attribute attribute : assertion.getAttributeStatements().getFirst().getAttributes()) {
      log.debug("Attribute name: " + attribute.getName());
      for (XMLObject attributeValue : attribute.getAttributeValues()) {
        log.debug("Attribute value: " + ((XSString) attributeValue).getValue());
      }
    }
  }

  private static void logAuthenticationInstant(Assertion assertion) {
    log.debug("Authentication instant: " + assertion.getAuthnStatements().getFirst().getAuthnInstant());
  }

  private static void logAuthenticationMethod(Assertion assertion) {
    log.debug("Authentication method: "
        + assertion.getAuthnStatements().getFirst().getAuthnContext().getAuthnContextClassRef()
        .getURI());
  }

  private static void validateMessageLifetime(MessageContext context, long samlMessageLifetime,
      long samlMessageClockSkew) throws ComponentInitializationException, MessageHandlerException {
    MessageLifetimeSecurityHandler lifetimeSecurityHandler = new MessageLifetimeSecurityHandler();
    lifetimeSecurityHandler.setMessageLifetime(Duration.ofMillis(samlMessageLifetime));
    lifetimeSecurityHandler.setClockSkew(Duration.ofMillis(samlMessageClockSkew));
    lifetimeSecurityHandler.setRequiredRule(true);
    lifetimeSecurityHandler.initialize();
    lifetimeSecurityHandler.invoke(context);
  }
}
