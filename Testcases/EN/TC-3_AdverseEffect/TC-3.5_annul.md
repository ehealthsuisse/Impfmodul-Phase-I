## TC 3.5: Annul Adverse Effects

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module authenticates the http call by verifying the digital signature.

The user opens the adverse effects list view. The module displays the adverse effects stored in the Immunization Administration documents of the EPR.

The user selects one entry to view the details and edits the entry using the (Delete) button.

### Preparation:

For running the test the system must be prepared as follows:
- The user runs the portal in a standard browser
- The browser already has a active session with the Identity Provider
- The vaccination module is properly configured to communicate with the Identity Provider and the EPR test system.
- The patient ID is known in the EPR test system.
- A set of vaccination documents for the patient is present in the EPR test system.

### Test run:

The test shall be run as follows:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Nebenwirkungen im Menu auswählen.
- Die Ansicht für Nebenwirkungen öffnen.
- Einen Eintrag in der Liste auswählen und die Detailansicht öffnen.
- Das Formular zur Annullation öffnen.
- Eine Begründung eingeben und die Annullierung bestätigen.
- Verifizieren, dass der originale Eintrag in der Liste der Impfungen angezeigt wird.
- Verifizieren, dass der annullierte Eintrag nicht mehr im Impfausweis angezeigt wird.

### Expected result:

The expected result is:
- Das Formular kann aus der Detailansicht geöffnet werden.
- Eine Begründung muss hinzugefügt werden.
- Die Änderung annulliert den originalen Eintrag im Impfausweis.
- Der originale Eintrag und die Annulation werden in der Liste der Nebenwirkungen angezeigt.
- Der annullierte Eintrag wird nicht mehr im Impfausweis angezeigt.
