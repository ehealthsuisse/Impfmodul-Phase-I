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
package ch.admin.bag.vaccination.controller;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.HttpSessionUtils;
import ch.admin.bag.vaccination.service.saml.SAMLService;
import ch.admin.bag.vaccination.service.saml.SAMLServiceIfc;
import ch.admin.bag.vaccination.service.saml.SAMLUtils;
import ch.fhir.epr.adapter.exception.TechnicalException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.soap.soap11.Body;
import org.opensaml.soap.soap11.Envelope;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller of the SAML implementation.
 *
 */
@Slf4j
@RestController
@Tag(name = "SAML Controller", description = "Controller used to handle SAML request")
public class SAMLController {
  public static final String SSO_ENDPOINT = "/saml/sso";
  public static final String SSO_ENDPOINT_NEW = "/saml/ssoNew";

  @Value("${sp.forwardArtifactToClientUrl}")
  private String forwardArtifactToClientUrl;

  @Value("${sp.otherNodeLogoutURL:#{null}}")
  private String otherNodeLogoutURL;

  @Autowired
  private SAMLServiceIfc samlService;

  @Autowired
  private ProfileConfig profileConfig;

  /**
   * Returns if session is authenticated
   */
  @GetMapping("/saml/isAuthenticated")
  @Operation(description = "SAML check")
  public boolean isAuthenticated() {
    if (profileConfig.isLocalMode()) {
      return true;
    }

    return HttpSessionUtils.getIsInitialCallValidFromSession() && HttpSessionUtils.getIsAuthenticatedFromSession();
  }

  @GetMapping("/saml/login")
  @Operation(description = "empty login")
  public String login(HttpServletRequest request) throws IOException {
    // For development purpose only!
    // If no session was initiated on local mode, use dummy session
    if (profileConfig.isLocalMode() && !HttpSessionUtils.getIsInitialCallValidFromSession()) {
      HttpSessionUtils.initializeValidDummySession();
    }

    // For local mode, the SAMLFilter is not active, therefor authentication mechanism is not used and
    // user needs to be manually forwarded to the vaccination record
    if (profileConfig.isLocalMode()) {
      boolean isLocalhost =
          "127.0.0.1".equals(request.getRemoteAddr()) || "0:0:0:0:0:0:0:1".equals(request.getRemoteAddr());
      boolean isEmptyPath = request.getContextPath().isBlank();
      String serverName = request.getServerName().replace("-backend", "");
      String contextPath = request.getContextPath().replace("-backend", "-frontend");
      return request.getScheme() + "://"
      + serverName
      + (isLocalhost ? ":" + (isEmptyPath ? "9000" : "8080" + contextPath) : "")
      + "/vaccination-record";
    }

    return null;
  }

  /**
   * Back channel logout from session.
   *
   * @param xml the SAML message
   */
  @PostMapping(path = "/saml/logout", produces = "text/xml")
  @Operation(description = "Back channel SAML Logout")
  public ResponseEntity<String> logout(HttpServletRequest request, @RequestBody String xml) {
    log.debug("Received samlLogout, to switch to TRACE log level");
    log.trace("Saml logout request: {}", xml);
    try {
      String xmlWithoutForwardToken = xml.replace(SAMLService.FORWARD_TOKEN, "");
      XMLObject samlObject = SAMLUtils.unmarshall(xmlWithoutForwardToken);
      if (samlObject == null) {
        log.warn("Received empty logout - ignoring request.");
        return new ResponseEntity<>("SamlObject was not found or could not be parsed.", HttpStatus.BAD_REQUEST);
      }

      SAMLUtils.logSAMLObject(samlObject);
      if (samlObject instanceof LogoutRequest logoutRequest) {
        return new ResponseEntity<>(performLogout(logoutRequest, xml, request), HttpStatus.OK);
      }

      if (samlObject instanceof Envelope envelope) {
        Body body = envelope.getBody();
        List<XMLObject> children = body.getUnknownXMLObjects(LogoutRequest.DEFAULT_ELEMENT_NAME);
        XMLObject child = !children.isEmpty() ? children.getFirst() : null;
        if (child instanceof LogoutRequest logoutRequest) {
          return new ResponseEntity<>(performLogout(logoutRequest, xml, request), HttpStatus.OK);
        }
      }

      log.warn("Samlobject {} not supported!", samlObject.getClass().getSimpleName());
    } catch (Exception ex) {
      log.warn("Exception occured during logout procedure", ex);
    }

    throw new TechnicalException("Logout Request could not be processed.");
  }

  @GetMapping(value = SSO_ENDPOINT)
  @Operation(description = "Pushes the SAML-Artifact to the SP")
  public void ssoArtifactRedirectGet(@RequestParam(name = "SAMLart") String samlArtifact,
      @RequestParam(name = "RelayState") String idpIdentifier, HttpServletResponse response) throws IOException {
    log.debug("Artifact {} received for idp {}", samlArtifact, idpIdentifier);

    response.sendRedirect(forwardArtifactToClientUrl + "?SAMLart=" + samlArtifact + "&RelayState=" + idpIdentifier);
  }

  @PostMapping(value = SSO_ENDPOINT)
  @Operation(description = "Pushes the SAML-Artifact to the SP")
  public void ssoArtifactRedirectPost(@RequestBody String idpResponse, HttpServletResponse response)
      throws IOException {
    log.debug("Artifact request received {}", idpResponse);
    Map<String, String> params = HttpSessionUtils.getQueryParameters(idpResponse);
    String samlArtifact = params.get("samlart");
    String idpIdentifier = params.get("relaystate");

    response.sendRedirect(forwardArtifactToClientUrl + "?SAMLart=" + samlArtifact + "&RelayState=" + idpIdentifier);
  }

  private String performLogout(LogoutRequest logoutRequest, String xml, HttpServletRequest request) throws MarshallingException {
    String idp = samlService.logout(logoutRequest.getNameID().getValue());
    boolean isForwardedRequest = xml.startsWith(SAMLService.FORWARD_TOKEN);
    if (idp == null && !isForwardedRequest) {
      log.debug("Session not found for logout request with NameID: {}", logoutRequest.getNameID().getValue());
      samlService.sendLogoutToOtherNode(otherNodeLogoutURL, xml);
    }

    LogoutResponse logoutResponse = samlService.createLogoutResponse(idp, logoutRequest, request);
    String response = SAMLUtils.addEnvelope(SAMLUtils.convertElementToString(logoutResponse));
    log.debug("Saml logout response: {}", response);

    return response;
  }

}
