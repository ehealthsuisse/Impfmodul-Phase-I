## TC 3.4: Edit Adverse Effects

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module authenticates the http call by verifying the digital signature.

The user opens the adverse effects list view. The module displays the adverse effects stored in the Immunization Administration documents of the EPR.

The user selects one entry to view the details and edits the entry using the (Edit) button.


### Preparation:

For running the test the system must be prepared as follows:
- The user runs the portal in a standard browser
- The browser already has a active session with the Identity Provider
- The vaccination module is properly configured to communicate with the Identity Provider and the EPR test system.
- The patient ID is known in the EPR test system.
- A set of vaccination documents for the patient is present in the EPR test system.

### Test run:

Der Test wie folgt durchgeführt:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Nebenwirkungen im Menu auswählen.
- Die Ansicht für Nebenwirkungen öffnen.
- Einen Eintrag in der Liste auswählen und die Detailansicht öffnen.
- Das Formular zum Bearbeiten öffnen.  
- Ausgewählte Attribute des Eintrags ändern und die Änderung bestätigen.
- Verifizieren, dass die Änderungen in einem neuen Dokument im EPD gespeichert wurde.
- Verifizieren, dass der geänderte Eintrag in der Liste die Nebenwirkungen erscheint.
- Verifizieren, dass die Änderung den originalen Eintrag im Impfausweis überschreibt.


### Expected result:

The expected result is:
- Das Formular kann per Kontextmenü geöffnet werden.
- Alle Attribute können bearbeitet werden.
- Es wird ein Dialog zur Bestätigung angezeigt.
- Der geänderte Eintrag erscheint in der Liste der Nebenwirkungen.
- Die Änderung überschreibt den originalen Eintrag im Impfausweis.
- Die Kommentare zur Nebenwirkung wurden aus dem Original übernommen.
