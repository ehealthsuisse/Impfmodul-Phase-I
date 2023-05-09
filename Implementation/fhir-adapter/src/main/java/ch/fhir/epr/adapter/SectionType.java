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
package ch.fhir.epr.adapter;

import lombok.Getter;

/**
 * Contains the supported section type of a bundle:
 * <ul>
 * <li>allergies</li>
 * <li>pastillnesses</li>
 * <li>administration</li>
 * <li>medicalproblems</li>
 * </ul>
 *
 * @see <a href=
 *      "http://fhir.ch/ig/ch-vacd/vaccination-record-document.html">vaccination-record-document</a>
 * 
 */
@Getter
public enum SectionType {
  ALLERGIES(
      "allergies",
      "Allergies and adverse reactions Document",
      "48765-2",
      "Allergies and adverse reactions Document",
      "http://loinc.org"), //
  PAST_ILLNESSES(
      "pastillnesses",
      "undergone illnesses for immunization",
      "11348-0",
      "Hx of Past illness",
      "http://loinc.org"), //
  IMMUNIZATION(
      "administration",
      "Immunization Administration",
      "11369-6",
      "Hx of Immunization",
      "http://loinc.org"), //
  MEDICAL_PROBLEM(
      "medicalproblems",
      "Problem list Reported",
      "11450-4",
      "Problem list Reported",
      "http://loinc.org");

  private String id;
  private String title;
  private String code;
  private String display;
  private String system;

  private SectionType(String id, String title, String code, String display, String system) {
    this.id = id;
    this.title = title;
    this.code = code;
    this.display = display;
    this.system = system;
  }
}
