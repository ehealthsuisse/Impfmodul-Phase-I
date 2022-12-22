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

import java.util.Locale;

/**
 * 
 * i18n key to generate PDF in several pieces of language
 *
 */
public enum I18nKey {

  VACCINATION_RECORD("Vaccination Record", "Impfausweis", "Carnet de vaccination", ""), //
  VACCINATION("Vaccination", "Impfung", "Vaccination", ""), //
  ALLERGY("Allergy", "Allergie", "Allergie", ""), //
  PASTILLNESS("PastIllness", "Vorerkrankungen", "maladie préexistante", ""), //

  FIRSTNAME("First name", "Vorname", "Prénom", ""), //
  LASTNAME("Last name", "Name", "Nom", ""), //
  BIRTHDAY("Birthday", "Geburtsdatum", "Date de naissance", ""), //
  GENDER("Gender", "Gender", "Genre", ""), //

  DATE("Date", "Datum", "Date", ""), //
  CLINICAL_STATUS("Clinical Status", "Klinischer Status", "Status Clinique", ""), //
  VACCINE("Vaccine", "Impfstoff", "Vaccin", ""), //
  DISEASE("Disease", "Imfschutz", "Maladie", ""), //
  DOSE("Dose", "Dosis", "Dose", ""), //
  TREATING("Treating", "Behandelnder", "Traitant", ""), //

  DATE_FORMAT("dd MMMM yyyy", "dd MMMM yyyy", "dd MMMM yyyy", "dd MMMM yyyy"),

  DUMMY("", "", "", "");

  private String en;
  private String de;
  private String fr;
  private String it;

  private I18nKey(String en, String de, String fr, String it) {
    this.en = en;
    this.de = de;
    this.fr = fr;
    this.it = it;
  }

  public String getTranslation(String lang) {
    if (lang == null) {
      return en;
    }
    switch (lang) {
      case "de":
        return de;
      case "fr":
        return fr;
      case "it":
        return it;
      default:
        return en;
    }
  }


  public Locale getLocale(String lang) {
    if (lang == null) {
      return Locale.ENGLISH;
    }
    switch (lang) {
      case "de":
        return Locale.GERMAN;
      case "fr":
        return Locale.FRENCH;
      case "it":
        return Locale.ITALIAN;
      default:
        return Locale.ENGLISH;
    }
  }
}
