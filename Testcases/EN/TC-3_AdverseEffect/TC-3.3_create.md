## TC 3.3: Create Adverse Effects

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module authenticates the http call by verifying the digital signature.

The user opens the adverse effects list view. The module displays the adverse effects stored in the Immunization Administration documents of the EPR.

The user opens the form to add an entry using the (+) button.

### Preparation:

For running the test the system must be prepared as follows:
- The user runs the portal in a standard browser
- The browser already has a active session with the Identity Provider
- The vaccination module is properly configured to communicate with the Identity Provider and the EPR test system.
- The patient ID is known in the EPR test system.


### Test run:

The test shall be run as follows:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext und der Rolle GFP und ASSISTANT starten.
- Die Ansicht für Nebenwirkungen im Menu auswählen.
- Die Ansicht für Nebenwirkungen öffnen.
- Das Formular zur Erfassung neuer Nebenwirkungen öffnen.
- Eine neue Nebenwirkung erfassen.  
- Verifizieren, dass die Unverträglichkeit im EPD gespeichert wurde und in der Tabelle der Nebenwirkung angezeigt wird.


### Expected result:

The expected result is:
- Das Impfmodul zeigt alle Nebenwirkungen aus dem EPD Testsystem zeilenweise an und die neu erfasste Nebenwirkung erscheint in der Tabelle der Nebenwirkungen.
- Die Detailansicht einer Nebenwirkung öffnet sich per Doppelklick in der Zeile der neue erfassten Nebenwirkung und die Attribute in der Detailansicht sind identisch mit den eingegeben Werten.

Eine neue Unverträglichkeit muss mit den folgenden Attributen erfasst werden können:  
1.	Zeitpunkt (recordedDate)
2.	Name der Nebenwirkung (code)
3.	Kritikalität (criticality, optional)
4.	Status (clinicalStatus, optional)
5.	Gesundheitsfachperson (recorder)
6.	Art der Nebenwirkung (type, Nebenwirkung oder Intoleranz, optional)
7.	Verifiziert (verificationStatus, optional)
8.	Kategorie (category, nur Medication)
