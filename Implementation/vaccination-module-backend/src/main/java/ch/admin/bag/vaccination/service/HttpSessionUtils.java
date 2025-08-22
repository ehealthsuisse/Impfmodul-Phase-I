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
package ch.admin.bag.vaccination.service;

import ch.fhir.epr.adapter.data.PatientIdentifier;
import ch.fhir.epr.adapter.data.dto.AuthorDTO;
import ch.fhir.epr.adapter.data.dto.HumanNameDTO;
import ch.fhir.epr.adapter.exception.TechnicalException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class HttpSessionUtils {
  public static final String AUTHENTICATED_SESSION_ATTRIBUTE = "authenticated";
  public static final String INITIAL_CALL_VALID = "initialCallValid";
  public static final String AUTHOR = "author";
  public static final String IDP = "idp";
  public static final String CACHE_PATIENT_IDENTIFIER = "patientIdentifier";
  public static final String PURPOSE = "purpose";

  public static AuthorDTO getAuthorFromSession() {
    return getParameterFromSession(AUTHOR);
  }

  public static String getIdpFromSession() {
    return getParameterFromSession(IDP);
  }

  public static boolean getIsAuthenticatedFromSession() {
    return Boolean.TRUE.equals(getParameterFromSession(AUTHENTICATED_SESSION_ATTRIBUTE));
  }

  public static boolean getIsInitialCallValidFromSession() {
    return Boolean.TRUE.equals(getParameterFromSession(INITIAL_CALL_VALID));
  }

  public static PatientIdentifier getPatientIdentifierFromSession() {
    return getParameterFromSession(CACHE_PATIENT_IDENTIFIER);
  }

  public static Map<String, String> getQueryParameters(String queryString) {
    return Stream.of(queryString.split("&"))
        .collect(Collectors.toMap(entry -> entry.substring(0, entry.indexOf("=")).toLowerCase(),
            entry -> decode(entry.substring(entry.indexOf("=") + 1))));
  }

  public static void initializeValidDummySession() {
    setParameterInSession(HttpSessionUtils.INITIAL_CALL_VALID, true);
    setParameterInSession(HttpSessionUtils.AUTHOR, new AuthorDTO(
        new HumanNameDTO("Peter", "Müller", null, null, null), "HCP", "7600000000000"));
    setParameterInSession(HttpSessionUtils.IDP, "Dummy");
    setParameterInSession(HttpSessionUtils.PURPOSE, "NORM");
  }

  public static void setParameterInSession(String paramName, Object value) {
    RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes instanceof ServletRequestAttributes attributes) {
      HttpServletRequest request = attributes.getRequest();
      request.getSession().setAttribute(paramName, value);
    }
  }

  private static String decode(String value) {
    try {
      return URLDecoder.decode(value, StandardCharsets.UTF_8.toString());
    } catch (UnsupportedEncodingException e) {
      return value;
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> T getParameterFromSession(String param) {
    HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
    HttpSession session = request.getSession(false);

    if (session == null) {
      log.debug("Could not request {} from session because session is null.", param);
      return null;
    }

    if ("patientIdentifier".equals(param) && session.getAttribute(param) == null) {
      throw new TechnicalException("Session is not initialised.");
    }

    return (T) session.getAttribute(param);
  }
}
