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

import java.util.Arrays;

/**
 *
 * i18n key to generate PDF in several pieces of language
 *
 */
public enum I18nKey {
  VACCINATION_RECORD("Vaccination certificate", "Impfausweis", "Carnet de vaccination",
      "Libretto delle vaccinazioni"), //
  BASIC_VACCINATION("Basic vaccinations", "Basisimpfungen", "Vaccinations de base", "Vaccinazioni di base"), //
  OTHER_VACCINATION("Other vaccinations", "Andere Impfungen", "Autres vaccinations", "Altere vaccinazioni"), //
  ADVERSE_EVENTS("Adverse Events", "Nebenwirkungen (UIE)", "Effets secondaires (EIV)",
      "Effetti indesiderati delle vaccinazioni (EIV)"), //
  PASTILLNESSES("Infectious Diseases", "Infektionskrankheiten", "Maladies infectieuses", "Malattie infettive"), //
  RISK_FACTORS("Risk Factors", "Risikofaktoren ", "Facteurs de risque", "Fattori di rischio"), //
  PASTILLNESS("Infectious Disease", "Infektionskrankheit", "Maladie", "Malattie"), //
  RISK_FACTOR("Risk Factor", "Risikofaktor", "Facteur de risqué", "Fattore di rischio"), //

  FIRSTNAME("First name", "Vorname", "Prénom", "Nome"), //
  LASTNAME("Last name", "Name", "Nom", "Cognome"), //
  BIRTHDAY("Birthday", "Geburtsdatum", "Date de naissance", "Data di nascita"), //
  GENDER("Gender", "Geschlecht", "Genre", "Genere"), //
  MALE("male", "männlich", "masculin", "maschile"), //
  FEMALE("female", "weiblich", "féminin", "femminile"), //
  UNDIFFERENTIATED("undifferentiated", "ohne Angabe", "non-binaire", "indifferenziato"), //
  UNKNOWN("not specified", "keine Angabe", "aucune indication", "non specificato"), //
  DATE("Date", "Datum", "Date", "Data"), //
  BEGIN("Begin", "Beginn", "Début", "Data di inizio"), //
  END("End", "Ende", "Fin", "Data di fine"), //
  CLINICAL_STATUS("Clinical Status", "Klinischer Status", "Status Clinique", "Stato Clinico"), //
  VACCINE("Vaccine", "Impfstoff", "Vaccin", "Vaccino"), //
  DISEASE("Disease", "Impfschutz", "Maladie", "Malattia"), //
  DOSE("Dose", "Dosis", "Dose", "Dose"), //
  TREATING("Performer", "Geimpft Von", "Vacciné Par", "Vaccinato da"), //
  VALIDATED("Validated", "Validiert", "Validé", "Validato"), //
  PRINTED1("EPR Vaccination certificate", "EPD Impfausweis", "DEP carnet de vaccination",
      "CIP Libretto delle vaccinazioni"), //
  PRINTED2("Printed on: ", "Gedruckt am: ", "Imprimé sur: ", "Stampato su: "), //
  LEGAL_REMARK("The vaccination card is not an official document.", "Der Impfausweis ist kein amtliches Dokument.",
      "Le carnet de vaccination n'est pas un document officiel.",
      "Il libretto di vaccinazione non è un documento ufficiale."), //
  LOGO_FILE_NAME("/Logo_EN_EPR.jpg", "/Logo_DE_EPD.jpg", "/Logo_FR_DEP.jpg", "/Logo_IT_CIP.jpg"),
  ACTIVE("Active", "Aktiv", "Actif", "Attivo"),
  INACTIVE("Inactive", "Inaktiv", "Inactif", "Inattivo"),
  LAST_MODIFIED_BY("Last modified by: ", "Zuletzt geändert von: ", "Dernière modification par: ", "Ultima modifica di: ");

  public static boolean exists(String gender) {
    return Arrays.stream(values()).anyMatch(value -> value.name().equals(gender));
  }

  private final String en;
  private final String de;
  private final String fr;
  private final String it;

  I18nKey(String en, String de, String fr, String it) {
    this.en = en;
    this.de = de;
    this.fr = fr;
    this.it = it;
  }

  public String getTranslation(String lang) {
    if (lang == null) {
      return en;
    }
    return switch (lang) {
      case "de" -> de;
      case "fr" -> fr;
      case "it" -> it;
      default -> en;
    };
  }
}
