## TC 3.1: View Adverse Effects

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module authenticates the http call by verifying the digital signature.

The user opens the adverse effects list view. The module displays the adverse effects stored in the Immunization Administration documents of the EPR.

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
- Verifizieren, dass alle Nebenwirkungen aus dem EPD Testsystem (Gazelle, EPD Playground, Testsystem einer Gemeinschaft) in einer Tabelle angezeigt werden.

### Expected result:

The expected result is:
- Das Impfmodul zeigt alle Nebenwirkungen aus dem EPD Testsystem zeilenweise an.
- Nebenwirkungen werden zeitgeordnet vom neuesten zum ältesten Eintrag angezeigt.  

Nebenwirkungen werden mit den folgenden Attributen angezeigt:
1.	Zeitpunkt (recordedDate)
2.	Name der Nebenwirkung (code)
3.	Kritikalität (criticality)
4.	Status (clinicalStatus)
5.	Gesundheitsfachperson (recorder)
