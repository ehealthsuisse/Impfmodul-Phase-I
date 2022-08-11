## TC 1.3: Impfmodul starten, ungültige Parameter
Ein Benutzer startet das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen oder Patienten und Patientinnen.  Dabei werden erforderlichen Attribute übergeben, damit das Impfmodul die Impfdaten des Patienten oder der Patientin aus dem EPD abfragen kann.
Die digitale Signatur des Aufrufs soll korrekt sein, aber die für den Aufruf benötigten Parameter sollen unvollständig sein.  


### Vorbereitung:
Für den Test müssen die folgenden Vorbereitungen getroffen werden:
- Der Benutzer nutzt das EPD Portal über einen Standard Browser
- Der Browser hat bereits eine aktive Session mit dem Identity Provider, bzw. der Benutzer hat sich im EPD Portal authentisiert.
- Der Benutzer startet das Impfmodul aus dem EPD Portal, per Button, Link, o.ä.

### Durchführung:
Der Test wie folgt durchgeführt:
Iteration 1:
- Das Impfmodul aus einem EPD Portal für Gesundheitsfachpersonen mit Patientenkontext starten.
- Verifizieren, dass das Impfmodul eine Fehlerseite anzeigt mit einer Mitteilung, dass das Parameter für den Aufruf unvollständig sind.


### Erwartetes Ergebnis:
Das erwartete Resultat des Tests ist wie folgt:
- Das Impfmodul startet im gleichen Browser-Fenster oder in einem neuen Browser-Tab.
- Das Impfmodul zeigt eine Fehlerseite an.
