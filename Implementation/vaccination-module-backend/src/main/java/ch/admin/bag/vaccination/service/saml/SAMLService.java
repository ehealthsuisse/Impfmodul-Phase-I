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

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.HttpSessionUtils;
import ch.admin.bag.vaccination.service.saml.config.IdentityProviderConfig;
import ch.admin.bag.vaccination.service.saml.config.IdpProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;

import lombok.extern.slf4j.Slf4j;
import net.shibboleth.shared.component.ComponentInitializationException;

import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.xml.security.c14n.Canonicalizer;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Marshaller;
import org.opensaml.messaging.context.MessageContext;
import org.opensaml.messaging.encoder.MessageEncodingException;
import org.opensaml.saml.common.SAMLObjectContentReference;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.common.binding.SAMLBindingSupport;
import org.opensaml.saml.common.messaging.context.SAMLEndpointContext;
import org.opensaml.saml.common.messaging.context.SAMLPeerEntityContext;
import org.opensaml.saml.saml2.binding.encoding.impl.HTTPPostEncoder;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.saml.saml2.core.impl.AssertionBuilder;
import org.opensaml.xmlsec.keyinfo.KeyInfoGenerator;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.impl.SignatureBuilder;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;

@EnableScheduling
@Service
@Slf4j
public class SAMLService implements SAMLServiceIfc {
  public static final String FORWARD_TOKEN = "FORWARD;;";

  @Autowired
  private IdpProvider idpProviders;

  @Autowired
  private IdPAdapter idpAdapter;

  @Autowired
  private ProfileConfig profilConfig;

  @Value("${idp.knownEntityId}")
  private String spEntityId;

  @Value("${sp.assertionConsumerServiceUrl}")
  private String assertionConsumerServiceUrl;

  private final Map<String, SecurityContext> sessionIdToSecurityContext = new ConcurrentHashMap<>();
  private final Map<String, String> nameToSessionId = new ConcurrentHashMap<>();

  @Override
  public ArtifactResolve buildArtifactResolve(IdentityProviderConfig idpConfig, Artifact artifact) {
    ArtifactResolve artifactResolve = SAMLUtils.createUnsignedArtifactResolveRequest(getEntityId(idpConfig),
        idpConfig.getArtifactResolutionServiceURL(), artifact);
    signRequest(artifactResolve, idpConfig);

    return artifactResolve;
  }

  @Override
  public boolean checkAndUpdateSessionInformation(HttpSession httpSession) {
    String currentSessionId = httpSession.getId();

    SecurityContext currentSecurityContext =
        (SecurityContext) httpSession.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    SAMLAuthentication authentication = (SAMLAuthentication) currentSecurityContext.getAuthentication();
    String subjectName = authentication.getName();
    String oldSessionId = nameToSessionId.remove(subjectName);
    if (oldSessionId != null) {
      sessionIdToSecurityContext.remove(oldSessionId);
      sessionIdToSecurityContext.put(currentSessionId, currentSecurityContext);
      nameToSessionId.put(subjectName, currentSessionId);
      return true;
    }

    log.debug("Removing spring context information.");
    return false;
  }

  @Override
  public void createAuthenticatedSession(String idp, HttpServletRequest request, Assertion assertion) {
    AuthenticatedPrincipal principal =
        () -> assertion.getSubject() != null ? assertion.getSubject().getNameID().getValue() : idp;

    Authentication auth = new SAMLAuthentication(principal, idp, assertion);
    SecurityContext securityContext = SecurityContextHolder.getContext();
    securityContext.setAuthentication(auth);

    HttpSession session = request.getSession();
    HttpSessionUtils.setParameterInSession(HttpSessionUtils.IDP, idp);
    HttpSessionUtils.setParameterInSession(HttpSessionUtils.AUTHENTICATED_SESSION_ATTRIBUTE, true);
    HttpSessionUtils.setParameterInSession(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
        securityContext);

    log.debug("add {} {}", session.getId(), principal.getName());
    addAuthenticatedSessionId(session.getId(), securityContext);
    SecurityContextHolder.setContext(securityContext);
  }

  @Override
  public void createDummyAuthentication(HttpServletRequest request) {
    Assertion emptyAssertion = new AssertionBuilder().buildObject();
    createAuthenticatedSession("Dummy", request, emptyAssertion);
  }

  @Override
  public LogoutResponse createLogoutResponse(String idp, LogoutRequest logoutRequest, HttpServletRequest request) {
    IdentityProviderConfig idpConfig = getIdpConfig(idp);
    String logoutURL = idpConfig != null ? idpConfig.getLogoutURL() : null;
    if (logoutURL == null) {
      log.debug("IDP for Session to logout could not determined. Returning vaccination URL as default destination. "
          + "Please check IDP config to set logoutURLs.");
      logoutURL = request.getRequestURI();
    }
    String entityId = getEntityId(idpConfig);
    LogoutResponse response = SAMLUtils.createUnsignedLogoutResponse(logoutRequest, entityId, logoutURL);
    signRequest(response, idpConfig);

    return response;
  }

  @Override
  public IdentityProviderConfig getIdpConfig(String idpIdentifier) {
    return idpIdentifier != null ? idpProviders.getProviderConfig(idpIdentifier) : null;
  }

  @Override
  public int getNumberOfSessions() {
    return sessionIdToSecurityContext.size();
  }

  @Override
  public String logout(String name) {
    log.debug("Logout NameId {}", name);

    String sessionId = nameToSessionId.remove(name);
    if (sessionId != null) {
      return removeSession(sessionId);
    }

    SecurityContextHolder.clearContext();
    return null;
  }

  @Override
  public void redirectToIdp(String idpIdentifier, HttpServletResponse httpServletResponse) {
    IdentityProviderConfig idpConfig = idpProviders.getProviderConfig(idpIdentifier);

    MessageContext context = new MessageContext();
    AuthnRequest authnRequest = createAuthnRequest(idpConfig);
    context.setMessage(authnRequest);
    SAMLBindingSupport.setRelayState(context, idpConfig.getIdentifier());

    SAMLPeerEntityContext peerEntityContext = context.ensureSubcontext(SAMLPeerEntityContext.class);

    SAMLEndpointContext endpointContext = peerEntityContext.ensureSubcontext(SAMLEndpointContext.class);
    endpointContext.setEndpoint(SAMLUtils.createEndpoint(idpConfig.getAuthnrequestURL()));
    idpAdapter.addSigningParametersToContext(context, idpConfig);

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

  /**
   * Checks every 20 secs if there is any IDP token which needs to be refreshed.
   */
  @Scheduled(fixedDelay = 20000)
  public void refreshIdpTokens() {
    if (profilConfig.isLocalMode()) {
      log.debug("Refreshing IDP Tokens is skipped due to local mode!");
      return;
    }

    List<String> toRemove = new ArrayList<>();
    log.debug("Refreshing IDP tokens, currently {} active sessions.", sessionIdToSecurityContext.entrySet().size());
    for (Entry<String, SecurityContext> entry : sessionIdToSecurityContext.entrySet()) {
      SecurityContext context = entry.getValue();
      SAMLAuthentication samlAuthentication = (SAMLAuthentication) context.getAuthentication();
      Assertion assertion = samlAuthentication.getAssertion();

      if (assertion != null) {
        String idp = samlAuthentication.getIdp();
        Instant instant = assertion.getConditions().getNotOnOrAfter();

        long millisUntilExpiry = instant.toEpochMilli() - Instant.now().toEpochMilli();
        long oneMinutesInMillis = 60 * 1000;

        if (millisUntilExpiry <= 0) {
          log.warn("Removing expired session for {} and IDP {}.", samlAuthentication.getName(), idp);
          toRemove.add(entry.getKey());
          SecurityContextHolder.clearContext();
        } else if (millisUntilExpiry < oneMinutesInMillis) {
          log.debug("Refreshing token for {} and IDP {}", samlAuthentication.getName(), idp);
          IdentityProviderConfig idpConfig = idpProviders.getProviderConfig(idp);
          String stsURL = idpConfig.getSecurityTokenServiceURL();
          if (stsURL != null) {
            try {
              Assertion renewedAssertion = idpAdapter.refreshToken(assertion, stsURL, idpConfig);
              samlAuthentication.setAssertion(renewedAssertion);
              log.debug("Renewal successful.");
            } catch (Exception ex) {
              log.error("Refresh failed for {} and IDP {}.", samlAuthentication.getName(), idp);
            }
          } else {
            log.warn("Security token service URL not set for IDP {}.", idp);
          }
        }
      } else {
        // remove any incomplete sessions without assertion
        toRemove.add(entry.getKey());
      }
    }

    toRemove.forEach(this::removeSession);
    synchronizeSessionMaps();
  }

  @Override
  public String removeSession(String sessionId) {
    log.debug("remove {}", sessionId);
    SecurityContext securityContext = sessionIdToSecurityContext.remove(sessionId);
    if (securityContext != null) {
      log.debug("Remove sessionId {}", sessionId);
      if (securityContext.getAuthentication() != null && securityContext.getAuthentication().getName() != null) {
        nameToSessionId.remove(securityContext.getAuthentication().getName());
      }
    }

    return securityContext != null && securityContext.getAuthentication() instanceof SAMLAuthentication
        ? ((SAMLAuthentication) securityContext.getAuthentication()).getIdp()
            : null;
  }

  @Override
  public ArtifactResponse sendAndReceiveArtifactResolve(IdentityProviderConfig idpConfig,
      ArtifactResolve artifactResolve) {
    return idpAdapter.sendAndReceiveArtifactResolve(idpConfig, artifactResolve);
  }

  @Override
  public void sendLogoutToOtherNode(String otherNodeLogoutURL, String logoutRequestBody) {
    if (otherNodeLogoutURL == null || otherNodeLogoutURL.isBlank()) {
      log.warn("No other node logout URL configured, skipping logout to other node.");
      return;
    }

    try {
      log.debug("Sending logout request to other node: {}", otherNodeLogoutURL);
      HttpClient httpClient = HttpClient.newBuilder().sslContext(SSLContext.getDefault()).build();
      HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(otherNodeLogoutURL))
        .header("Content-Type", "application/soap+xml")
        .POST(HttpRequest.BodyPublishers.ofString(FORWARD_TOKEN + logoutRequestBody))
        .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      log.debug("Successfully sent logout request to other node, received statusCode {}.", response.statusCode());
    } catch (Exception e) {
      log.error("Failed to redirect to other node {} for logout", otherNodeLogoutURL, e);
    }
  }

  private void addAuthenticatedSessionId(String sessionId, SecurityContext securityContext) {
    log.debug("add {} {}", sessionId, securityContext.getAuthentication().getName());
    sessionIdToSecurityContext.put(sessionId, securityContext);
    nameToSessionId.put(securityContext.getAuthentication().getName(), sessionId);
  }

  private AuthnRequest createAuthnRequest(IdentityProviderConfig idpConfig) {
    AuthnRequest authnRequest = SAMLUtils.createUnsignedAuthnRequest(getEntityId(idpConfig),
        idpConfig.getAuthnrequestURL(), assertionConsumerServiceUrl);
    signRequest(authnRequest, idpConfig);

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

  private String getEntityId(IdentityProviderConfig idpConfig) {
    return idpConfig != null && idpConfig.getEntityId() != null ? idpConfig.getEntityId() : spEntityId;
  }

  private void removeSecurityContextFromSession(HttpSession httpSession) {
    SecurityContextHolder.clearContext();
    removeSession(httpSession.getId());
  }

  private void signRequest(SignableSAMLObject request, IdentityProviderConfig idpConfig) {
    try {
      SignatureBuilder signFactory = new SignatureBuilder();
      Signature signature = signFactory.buildObject(Signature.DEFAULT_ELEMENT_NAME);
      signature.setCanonicalizationAlgorithm(Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
      signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
      signature.setSigningCredential(idpConfig.getSamlCredential(true));

      X509KeyInfoGeneratorFactory x509Factory = new X509KeyInfoGeneratorFactory();
      x509Factory.setEmitEntityCertificate(true);
      KeyInfoGenerator generator = x509Factory.newInstance();
      KeyInfo keyInfo = generator.generate(idpConfig.getSamlCredential(true));
      signature.setKeyInfo(keyInfo);

      request.setSignature(signature);
      ((SAMLObjectContentReference) signature.getContentReferences().getFirst())
      .setDigestAlgorithm(SignatureConstants.ALGO_ID_DIGEST_SHA256);

      Marshaller marshaller =
          XMLObjectProviderRegistrySupport.getMarshallerFactory().getMarshaller(request);
      marshaller.marshall(request);

      Signer.signObject(signature);
    } catch (Exception ex) {
      log.error("Error occurred while retrieving encoded cert", ex);
      log.error("failed to sign a SAML object", ex);
    }
  }

  // local logouts can lead to orphan entries in the nameToSessionId-map
  private void synchronizeSessionMaps() {
    if (nameToSessionId.size() != sessionIdToSecurityContext.size()) {
      Map<String, String> map = new ConcurrentHashMap<>(sessionIdToSecurityContext.size());
      sessionIdToSecurityContext.forEach((sessionId, value) -> {
        if (value != null && value.getAuthentication() != null) {
          String name = value.getAuthentication().getName();
          if (name != null) {
            map.put(name, sessionId);
          }
        }
      });
      nameToSessionId.clear();
      nameToSessionId.putAll(map);
    }
  }
}
