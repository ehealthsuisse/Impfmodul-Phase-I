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

import ch.admin.bag.vaccination.service.SignatureService;
import ch.admin.bag.vaccination.service.saml.config.IdentityProviderConfig;
import ch.admin.bag.vaccination.service.saml.config.IdpProvider;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.xml.security.c14n.Canonicalizer;
import org.opensaml.core.xml.config.XMLObjectProviderRegistry;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.messaging.handler.MessageHandlerException;
import org.opensaml.saml.common.SAMLObjectContentReference;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.binding.security.impl.MessageLifetimeSecurityHandler;
import org.opensaml.saml.common.binding.security.impl.ReceivedEndpointSecurityHandler;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLMessageInfoContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.common.xml.SAMLConstants;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.metadata.Endpoint;
import org.opensaml.saml.saml2.metadata.SingleSignOnService;
import org.opensaml.security.credential.Credential;
import org.opensaml.xmlsec.SignatureSigningParameters;
import org.opensaml.xmlsec.context.SecurityParametersContext;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.X509Certificate;
import org.opensaml.xmlsec.signature.X509Data;
import org.opensaml.xmlsec.signature.impl.SignatureBuilder;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SAMLService implements SAMLServiceIfc {
  public static final String AUTHENTICATED_SESSION_ATTRIBUTE = "authenticated";
  public static final String IDP_IDENTIFIER_ATTRIBUTE = "idpIdentifier";
  public static final String IDP_ASSERTION = "idpAssertion";

  /** necessary to be initialized which is done using the binding */
  @SuppressWarnings("unused")
  @Autowired
  private XMLObjectProviderRegistry registry;

  @Autowired
  private SignatureService signatureService;

  @Autowired
  private IdpProvider idpProviders;

  @Autowired
  private IdPAdapter idpAdapter;

  @Value("${idp.knownEntityId}")
  private String spEntityId;

  @Value("${sp.assertionConsumerServiceUrl}")
  private String assertionConsumerServiceUrl;

  // TODO 20220812 session management clean up
  private Map<String, SecurityContext> sessionIdToSecurityContext = new ConcurrentHashMap<>();
  private Map<String, String> nameToSessionId = new ConcurrentHashMap<>();

  @Override
  public ArtifactResolve buildArtifactResolve(IdentityProviderConfig idpConfig, Artifact artifact) {
    ArtifactResolve artifactResolve = SAMLUtils.buildSAMLObject(ArtifactResolve.class);

    Issuer issuer = SAMLUtils.buildSAMLObject(Issuer.class);
    issuer.setValue(spEntityId);
    artifactResolve.setIssuer(issuer);
    artifactResolve.setIssueInstant(Instant.now());
    artifactResolve.setID(SAMLUtils.generateSecureRandomId());
    artifactResolve.setDestination(idpConfig.getArtifactResolutionServiceURL());
    artifactResolve.setArtifact(artifact);

    signRequest(artifactResolve);

    return artifactResolve;
  }

  @Override
  public void createAuthenticatedSession(HttpServletRequest request, String saml2Reponse,
      Assertion assertion) {
    createAuthenticatedSession(request, saml2Reponse, assertion, null);
  }

  @Override
  public void createDummyAuthentication(HttpServletRequest request) {
    createAuthenticatedSession(request, "Dummy", null, "Dummy");
  }

  @Override
  public IdentityProviderConfig getIdpConfig(String idpIdentifier) {
    return idpProviders.getProviderConfig(idpIdentifier);
  }

  @Override
  public int getNumberOfSessions() {
    return sessionIdToSecurityContext.size();
  }

  @Override
  public void logout(String name) {
    log.info("logout {}", name);

    String sessionId = nameToSessionId.get(name);
    if (sessionId != null) {
      remove(sessionId);
    }
  }

  @Override
  public void redirectToIdp(String idpIdentifier, HttpServletResponse httpServletResponse) {
    IdentityProviderConfig idpConfig = idpProviders.getProviderConfig(idpIdentifier);

    MessageContext context = new MessageContext();
    AuthnRequest authnRequest = createAuthnRequest(idpConfig);
    context.setMessage(authnRequest);

    SAMLPeerEntityContext peerEntityContext =
        context.getSubcontext(SAMLPeerEntityContext.class, true);

    SAMLEndpointContext endpointContext =
        peerEntityContext.getSubcontext(SAMLEndpointContext.class, true);
    endpointContext.setEndpoint(getIdpEndpoint(idpConfig.getAuthnrequestURL()));

    SignatureSigningParameters signatureSigningParameters = new SignatureSigningParameters();
    signatureSigningParameters.setSigningCredential(getServiceProviderSignatureCredential());
    signatureSigningParameters
        .setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);

    context.getSubcontext(SecurityParametersContext.class, true)
        .setSignatureSigningParameters(signatureSigningParameters);

    HTTPPostEncoder encoder = new HTTPPostEncoder();
    VelocityEngine vEngine = createEngine();
    encoder.setVelocityEngine(vEngine);
    encoder.setMessageContext(context);
    encoder.setHttpServletResponseSupplier(() -> httpServletResponse);

    try {
      encoder.initialize();
    } catch (ComponentInitializationException e) {
      throw new RuntimeException(e);
    }

    log.info("Redirecting to IDP " + idpIdentifier.toUpperCase());
    try {
      encoder.encode();
      SAMLUtils.logSAMLObject(authnRequest);
    } catch (MessageEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public ArtifactResponse sendAndReceiveArtifactResolve(IdentityProviderConfig idpConfig,
      ArtifactResolve artifactResolve) {
    return idpAdapter.sendAndReceiveArtifactResolve(idpConfig, artifactResolve);
  }

  @Override
  public void validateArtifactResponse(ArtifactResponse artifactResponse,
      HttpServletRequest request) {
    MessageContext context = new MessageContext();
    context.setMessage(artifactResponse);

    SAMLMessageInfoContext messageInfoContext =
        context.getSubcontext(SAMLMessageInfoContext.class, true);
    messageInfoContext.setMessageIssueInstant(artifactResponse.getIssueInstant());

    try {
      validateMessageLifetime(context);
      validateDestination(request, context);
    } catch (ComponentInitializationException e) {
      throw new RuntimeException(e);
    } catch (MessageHandlerException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void validateSecurityContext(HttpSession httpSession,
      SecurityContext authenticatedSecurityContext) {
    String sessionId = httpSession.getId();
    SecurityContext securityContext = sessionIdToSecurityContext.get(sessionId);

    if (securityContext == null || authenticatedSecurityContext == null
        || !securityContext.getAuthentication()
            .equals(authenticatedSecurityContext.getAuthentication())) {
      removeSecurityContextFromSession(httpSession);
      throw new TechnicalException("invalidSecurityContext detected in session.");
    }

    SecurityContextHolder.setContext(securityContext);
  }

  private Issuer buildIssuer() {
    Issuer issuer = SAMLUtils.buildSAMLObject(Issuer.class);
    issuer.setValue(spEntityId);
    return issuer;
  }

  private NameIDPolicy buildNameIdPolicy() {
    NameIDPolicy nameIDPolicy = SAMLUtils.buildSAMLObject(NameIDPolicy.class);
    nameIDPolicy.setAllowCreate(true);
    nameIDPolicy.setFormat(NameIDType.PERSISTENT);

    return nameIDPolicy;
  }

  private void createAuthenticatedSession(HttpServletRequest request, String saml2Reponse,
      Assertion assertion, String dummyNamedID) {
    AuthenticatedPrincipal principal = new AuthenticatedPrincipal() {

      @Override
      public String getName() {
        return assertion != null ? assertion.getSubject().getNameID().getValue() : dummyNamedID;
      }
    };

    Authentication auth = new SAMLAuthentication(principal, saml2Reponse);
    SecurityContext securityContext = SecurityContextHolder.getContext();
    securityContext.setAuthentication(auth);

    HttpSession session = request.getSession();
    session.setAttribute(AUTHENTICATED_SESSION_ATTRIBUTE, true);
    session.setAttribute(IDP_ASSERTION, assertion);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        securityContext);

    log.debug("add {} {}", session.getId(), principal.getName());
    put(session.getId(), securityContext);
    SecurityContextHolder.setContext(securityContext);
  }

  private AuthnRequest createAuthnRequest(IdentityProviderConfig idpConfig) {
    AuthnRequest authnRequest = SAMLUtils.buildSAMLObject(AuthnRequest.class);
    authnRequest.setIssueInstant(Instant.now());
    authnRequest.setDestination(idpConfig.getAuthnrequestURL());
    authnRequest.setProtocolBinding(SAMLConstants.SAML2_ARTIFACT_BINDING_URI);
    authnRequest.setAssertionConsumerServiceURL(
        assertionConsumerServiceUrl.replace("{idp}", idpConfig.getIdentifier().toLowerCase()));
    authnRequest.setID(SAMLUtils.generateSecureRandomId());
    authnRequest.setIssuer(buildIssuer());
    authnRequest.setNameIDPolicy(buildNameIdPolicy());

    signRequest(authnRequest);

    return authnRequest;
  }

  private VelocityEngine createEngine() {
    try {
      VelocityEngine velocityEngine = new VelocityEngine();
      velocityEngine.setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath");
      velocityEngine.setProperty(
          "classpath.resource.loader.class",
          "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
      velocityEngine.init();
      return velocityEngine;
    } catch (Exception e) {
      throw new RuntimeException("Error configuring velocity", e);
    }
  }

  private Endpoint getIdpEndpoint(String idpAuthNRequestURL) {
    SingleSignOnService endpoint = SAMLUtils.buildSAMLObject(SingleSignOnService.class);
    endpoint.setBinding(SAMLConstants.SAML2_REDIRECT_BINDING_URI);
    endpoint.setLocation(idpAuthNRequestURL);

    return endpoint;
  }

  private Credential getServiceProviderSignatureCredential() {
    return signatureService.getSamlSPCredential();
  }

  private void put(String sessionId, SecurityContext securityContext) {
    log.debug("add {} {}", sessionId, securityContext.getAuthentication().getName());
    sessionIdToSecurityContext.put(sessionId, securityContext);
    nameToSessionId.put(securityContext.getAuthentication().getName(), sessionId);
  }

  private void remove(String sessionId) {
    log.debug("remove {}", sessionId);
    SecurityContext securityContext = sessionIdToSecurityContext.get(sessionId);
    if (securityContext != null) {
      sessionIdToSecurityContext.remove(sessionId);
      log.debug("remove {} {}", sessionId, securityContext.getAuthentication().getName());
      nameToSessionId.remove(securityContext.getAuthentication().getName());
    }
  }

  private void removeSecurityContextFromSession(HttpSession httpSession) {
    SecurityContextHolder.clearContext();
    remove(httpSession.getId());
    httpSession.removeAttribute(AUTHENTICATED_SESSION_ATTRIBUTE);
    httpSession.removeAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
  }

  private void signRequest(SignableSAMLObject authnRequest) {
    try {
      SignatureBuilder signFactory = new SignatureBuilder();
      Signature signature = signFactory.buildObject(Signature.DEFAULT_ELEMENT_NAME);
      signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
      signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
      signature.setSigningCredential(getServiceProviderSignatureCredential());
      KeyInfo keyInfo = SAMLUtils.buildSAMLObject(KeyInfo.class);
      X509Data data = SAMLUtils.buildSAMLObject(X509Data.class);

      X509Certificate cert = SAMLUtils.buildSAMLObject(X509Certificate.class);
      String value = Base64.getEncoder()
          .encodeToString(getServiceProviderSignatureCredential().getPublicKey().getEncoded());
      cert.setValue(value);
      data.getX509Certificates().add(cert);
      keyInfo.getX509Datas().add(data);
      signature.setKeyInfo(keyInfo);

      authnRequest.setSignature(signature);
      ((SAMLObjectContentReference) signature.getContentReferences().get(0))
          .setDigestAlgorithm(SignatureConstants.ALGO_ID_DIGEST_SHA256);

      Marshaller marshaller =
          XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(authnRequest);
      marshaller.marshall(authnRequest);

      Signer.signObject(signature);
      String sig = signature.getDOM().getChildNodes().item(3).getFirstChild().getNodeValue();
      sig = sig.replace("\n", "").replace("\r", "");
      signature.getDOM().getChildNodes().item(3).getFirstChild().setNodeValue(sig);
    } catch (Exception ex) {
      log.error("Error occurred while retrieving encoded cert", ex);
      log.error("failed to sign a SAML object", ex);
    }
  }

  private void validateDestination(HttpServletRequest request, MessageContext context)
      throws ComponentInitializationException, MessageHandlerException {
    ReceivedEndpointSecurityHandler receivedEndpointSecurityHandler =
        new ReceivedEndpointSecurityHandler();
    receivedEndpointSecurityHandler.setHttpServletRequestSupplier(() -> request);
    receivedEndpointSecurityHandler.initialize();
    receivedEndpointSecurityHandler.invoke(context);
  }

  private void validateMessageLifetime(MessageContext context)
      throws ComponentInitializationException, MessageHandlerException {
    MessageLifetimeSecurityHandler lifetimeSecurityHandler = new MessageLifetimeSecurityHandler();
    lifetimeSecurityHandler.setClockSkew(Duration.ofMillis(1000));
    lifetimeSecurityHandler.setMessageLifetime(Duration.ofMillis(2000));
    lifetimeSecurityHandler.setRequiredRule(true);
    lifetimeSecurityHandler.initialize();
    lifetimeSecurityHandler.invoke(context);
  }

}
