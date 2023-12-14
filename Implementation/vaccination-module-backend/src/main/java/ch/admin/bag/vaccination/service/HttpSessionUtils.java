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
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  public static void initializeValidDummySession(HttpServletRequest request) {
    setParameterInSession(request, HttpSessionUtils.INITIAL_CALL_VALID, true);
    setParameterInSession(request, HttpSessionUtils.AUTHOR, new AuthorDTO(
        new HumanNameDTO("Peter", "Müller", null, null, null), "HCP", "GLN"));
    setParameterInSession(request, HttpSessionUtils.IDP, "Dummy");
    setParameterInSession(request, HttpSessionUtils.PURPOSE, "NORM");
  }

  public static void setParameterInSession(HttpServletRequest request, String paramName, Object value) {
    request.getSession().setAttribute(paramName, value);
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
      log.warn("Could not request {} from session because session is null.", param);
      return null;
    }

    return (T) session.getAttribute(param);
  }



}
