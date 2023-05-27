## TC 3.2: Adverse Effect Details

A user starts the Vaccination Module from the EPR portal for health professionals or patients by clicking the http link. The portal performs a http call to the vaccination module URL and conveys the parameter required by the vaccination module. The vaccination module authenticates the http call by verifying the digital signature.

The user opens the adverse effects list view. The module displays the adverse effects stored in the Immunization Administration documents of the EPR.

The user selects one entry to view the details.

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
- Genau eine Nebenwirkung in der Liste per Mausklick selektieren und die Detailansicht öffnen.
- Verifizieren, dass alle erforderlichen Attribute der Nebenwirkung angezeigt werden.

### Expected result:

The expected result is:
- Das Impfmodul zeigt alle Nebenwirkungen aus dem EPD Testsystem zeilenweise an.
- Die Nebenwirkungen werden zeitgeordnet vom neuesten zum ältesten Eintrag angezeigt.  
- Die Detailansicht einer Nebenwirkung öffnet sich per Doppelklick in der Zeile.

Nebenwirkungen müssen mit den folgenden Attributen angezeigt werden:
1.	Zeitpunkt (recordedDate)
2.	Name der Nebenwirkung (code)
3.	Kritikalität (criticality)
4.	Status (clinicalStatus)
5.	Gesundheitsfachperson (recorder)
6.	Art der Nebenwirkung (type)
7.	Verifiziert (verificationStatus)
8.	Kategorie (category, nur Medication)
