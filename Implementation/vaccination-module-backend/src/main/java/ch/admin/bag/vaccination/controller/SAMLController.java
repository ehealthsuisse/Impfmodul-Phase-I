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

import ch.admin.bag.vaccination.service.saml.SAMLServiceIfc;
import ch.admin.bag.vaccination.service.saml.SAMLUtils;
import ch.admin.bag.vaccination.service.saml.config.IdentityProviderConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Artifact;
import org.opensaml.saml.saml2.core.ArtifactResolve;
import org.opensaml.saml.saml2.core.ArtifactResponse;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
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
  public static final String SSO_ENDPOINT = "/saml/{idp}/sso";

  @Autowired
  private SAMLServiceIfc samlService;

  /**
   * Logouts a session.
   *
   * @param xml the SAML message
   */
  @PostMapping("/saml/logout")
  @Operation(description = "SAML Logout")
  public void logout(@RequestBody String xml) {
    log.info("samlLogout");

    try {
      XMLObject samlObject = SAMLUtils.unmarshall(xml);
      if (samlObject == null) {
        log.warn("samlLogout : samlObject is null");
        return;
      }
      if (samlObject instanceof LogoutRequest logoutRequest) {
        samlService.logout(logoutRequest.getNameID().getValue());
      } else {
        log.warn("xml {} not supported!", samlObject.getClass().getSimpleName());
      }
    } catch (Exception e) {
      log.warn("Exception:{}", e.toString());
    }
  }

  /**
   * <p>
   * Implements the step 5 of the SAML-Authentication. Pushes the SAML-Artifact to the SP.
   * </p>
   * <p>
   * samlArtifact: string provided by the IdP.
   * </p>
   *
   * <a href=
   * "http://docs.oasis-open.org/security/saml/Post2.0/sstc-saml-tech-overview-2.0-cd-02_html_25426336.gif">SAML-Authentication</a>
   *
   * @param samlArtifact The SAML-Artifact provided by the IdP.
   * @param request the HttpServletRequest
   * @param response the HttpServletResponse
   *
   */
  @RequestMapping(value = SSO_ENDPOINT, method = RequestMethod.GET)
  @Operation(description = "Pushes the SAML-Artifact to the SP")
  public void ssoArtifact(@RequestParam(name = "SAMLart") String samlArtifact, @PathVariable String idp,
      HttpServletRequest request, HttpServletResponse response) {

    IdentityProviderConfig idpConfig = samlService.getIdpConfig(idp);
    log.debug("Artifact received");
    Artifact artifact = buildArtifactFromRequest(samlArtifact);
    log.debug("Artifact: " + artifact.getValue());

    ArtifactResolve artifactResolve = samlService.buildArtifactResolve(idpConfig, artifact);
    log.debug("Sending ArtifactResolve");
    log.debug("ArtifactResolve: ");
    SAMLUtils.logSAMLObject(artifactResolve);

    // Step 6/7
    ArtifactResponse artifactResponse =
        samlService.sendAndReceiveArtifactResolve(idpConfig, artifactResolve);
    log.debug("ArtifactResponse received");
    log.debug("ArtifactResponse: ");
    SAMLUtils.logSAMLObject(artifactResponse);

    samlService.validateArtifactResponse(artifactResponse, request);

    Assertion assertion = getAssertion(artifactResponse);
    logAssertionForDebug(assertion);

    samlService.createAuthenticatedSession(request,
        SAMLUtils.getSamlMessageAsString(artifactResponse),
        assertion);
    redirectToGotoURL(request, response);
  }

  private Artifact buildArtifactFromRequest(String samlArt) {
    Artifact artifact = SAMLUtils.buildSAMLObject(Artifact.class);
    artifact.setValue(samlArt);
    return artifact;
  }

  private Assertion getAssertion(ArtifactResponse artifactResponse) {
    Response response = (Response) artifactResponse.getMessage();
    return response.getAssertions().get(0);
  }

  private void logAssertionAttributes(Assertion assertion) {
    for (Attribute attribute : assertion.getAttributeStatements().get(0).getAttributes()) {
      log.debug("Attribute name: " + attribute.getName());
      for (XMLObject attributeValue : attribute.getAttributeValues()) {
        log.debug("Attribute value: " + ((XSString) attributeValue).getValue());
      }
    }
  }

  private void logAssertionForDebug(Assertion assertion) {
    SAMLUtils.logSAMLObject(assertion);

    logAssertionAttributes(assertion);
    logAuthenticationInstant(assertion);
    logAuthenticationMethod(assertion);
  }

  private void logAuthenticationInstant(Assertion assertion) {
    log.debug("Authentication instant: " + assertion.getAuthnStatements().get(0).getAuthnInstant());
  }

  private void logAuthenticationMethod(Assertion assertion) {
    log.debug("Authentication method: "
        + assertion.getAuthnStatements().get(0).getAuthnContext().getAuthnContextClassRef()
            .getURI());
  }

  private void redirectToGotoURL(HttpServletRequest req, HttpServletResponse resp) {
    String gotoURL = (String) req.getSession().getAttribute("gotoURL");
    log.debug("Redirecting to requested URL: " + gotoURL);
    try {
      resp.sendRedirect(gotoURL);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
