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

import static ch.admin.bag.vaccination.service.husky.HuskyUtils.ASS;
import static ch.admin.bag.vaccination.service.husky.HuskyUtils.HCP;

import ch.admin.bag.vaccination.config.ProfileConfig;
import ch.admin.bag.vaccination.service.HttpSessionUtils;
import ch.admin.bag.vaccination.service.SignatureService;
import ch.admin.bag.vaccination.service.cache.Cache;
import ch.admin.bag.vaccination.service.cache.CacheIdentifierKey;
import ch.admin.bag.vaccination.service.saml.SAMLService;
import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@Tag(name = "Portal Controller", description = "Offers interface to initialize a fresh session")
public class PortalController {

  private static final String IDP = "idp";
  private static final String USER_FAMILY_NAME = "ufname";
  private static final String USER_GIVEN_NAME = "ugname";
  private static final String USER_ROLE = "role";
  private static final String PURPOSE = "purpose";
  private static final String USER_GLN = "ugln";
  private static final String TIMESTAMP = "timestamp";
  private static final String LANG = "lang";

  private static final String PRINCIPAL_NAME = "principalname";
  private static final String PRINCIPAL_ID = "principalid";

  private static final String LOCAL_ASSIGNING_AUTHORITY_OID = "laaoid";
  private static final String LOCAL_PATIENT_ID = "lpid";

  static final String ENDPOINT_VALIDATE = "/signature/validate";

  @Autowired
  private SignatureService signatureService;

  @Autowired
  private SAMLService samlService;

  @Autowired
  private ProfileConfig profileConfig;

  @Autowired
  private Cache cache;

  // A query string does start with the first parameter (no ?)
  // and has the sig parameter last, e.g. idp=test&sig=abc
  @PostMapping(ENDPOINT_VALIDATE)
  @ResponseBody
  @Operation(description = "Validates an URL query string.")
  public boolean validatePortalCall(HttpServletRequest request, @RequestBody String queryString) {
    log.debug("Endpoint call - validate query string {}", queryString);

    boolean validateCall = validateSignature(queryString);
    if (validateCall) {
      log.debug("Signature validation successful.");
      validateCall = initializeSession(request, queryString);
    }

    return validateCall;
  }

  private void clearCacheForPatient(HttpServletRequest request, Map<String, String> paramsList) {
    PatientIdentifier patientIdentifier =
        new PatientIdentifier(null, paramsList.get(LOCAL_PATIENT_ID), paramsList.get(LOCAL_ASSIGNING_AUTHORITY_OID));
    CacheIdentifierKey cacheIdentifier =
        new CacheIdentifierKey(patientIdentifier, new AuthorDTO(null, null, paramsList.get(USER_GLN)));

    request.getSession().setAttribute(HttpSessionUtils.CACHE_PATIENT_IDENTIFIER, patientIdentifier);
    cache.clear(cacheIdentifier);
  }

  private Object getAuthorFromParameters(Map<String, String> paramsList) {
    HumanNameDTO user = new HumanNameDTO();
    user.setFirstName(paramsList.get(USER_GIVEN_NAME));
    user.setLastName(paramsList.get(USER_FAMILY_NAME));
    user.setPrefix(paramsList.get("utitle"));

    AuthorDTO author = new AuthorDTO(
        user,
        null,
        paramsList.get(USER_ROLE),
        paramsList.get(PURPOSE),
        paramsList.get(USER_GLN),
        paramsList.get(PRINCIPAL_ID),
        paramsList.get(PRINCIPAL_NAME));

    log.debug("getAuthor {}", author);
    return author;
  }

  private boolean initializeSession(HttpServletRequest request, String queryString) {
    Map<String, String> paramsList = HttpSessionUtils.getQueryParameters(queryString);
    boolean validateCall = validateParameters(paramsList);
    if (validateCall) {
      clearCacheForPatient(request, paramsList);
      HttpSessionUtils.setParameterInSession(request, HttpSessionUtils.INITIAL_CALL_VALID, validateCall);
      HttpSessionUtils.setParameterInSession(request, HttpSessionUtils.AUTHOR, getAuthorFromParameters(paramsList));
      HttpSessionUtils.setParameterInSession(request, HttpSessionUtils.IDP, paramsList.get(IDP));
      HttpSessionUtils.setParameterInSession(request, HttpSessionUtils.PURPOSE, paramsList.get(PURPOSE));

      if (profileConfig.isLocalMode()) {
        samlService.createDummyAuthentication(request);
      }
    }

    return validateCall;
  }

  private boolean isNotNullOrEmpty(String value) {
    return value != null && !value.isBlank();
  }

  private boolean validateParameters(Map<String, String> params) {
    String role = params.get(USER_ROLE);
    String gln = params.get(USER_GLN);
    boolean mandatoryFieldsSet = isNotNullOrEmpty(params.get(IDP))
        && isNotNullOrEmpty(params.get(LOCAL_ASSIGNING_AUTHORITY_OID)) && isNotNullOrEmpty(params.get(LOCAL_PATIENT_ID))
        && isNotNullOrEmpty(params.get(LANG)) && isNotNullOrEmpty(params.get(PURPOSE))
        && isNotNullOrEmpty(role) && isNotNullOrEmpty(params.get(TIMESTAMP))
        && isNotNullOrEmpty(params.get(USER_FAMILY_NAME)) && isNotNullOrEmpty(params.get(USER_GIVEN_NAME));

    if (!mandatoryFieldsSet) {
      log.debug("Mandatory fields not all set: {}", params);
    }

    boolean hasGlnIfHcpOrAss =
        HCP.equalsIgnoreCase(role) || ASS.equalsIgnoreCase(role) ? isNotNullOrEmpty(gln) : true;

    if (!hasGlnIfHcpOrAss) {
      log.debug("GLN missing for HCP or ASS role: {}", params);
    }

    boolean hasPrincipalIdAndNameIfAss = ASS.equalsIgnoreCase(role)
        ? isNotNullOrEmpty(gln) && isNotNullOrEmpty(params.get(PRINCIPAL_ID))
            && isNotNullOrEmpty(params.get(PRINCIPAL_NAME))
        : true;

    if (!hasPrincipalIdAndNameIfAss) {
      log.debug("principalId or name missing for ASS role: {}", params);
    }

    return mandatoryFieldsSet && hasGlnIfHcpOrAss && hasPrincipalIdAndNameIfAss;
  }

  private boolean validateSignature(String queryString) {
    return signatureService.validateQueryString(queryString);
  }
}
