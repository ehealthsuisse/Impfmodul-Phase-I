## Lifecyclemanagement der Impfeinträge

Die juristischen und technischen Randbedingungen des EPD stellen besondere
Anforderungen an das Lifecyclemanagement von Impfdaten.

Das Impfmodul muss daher die folgenden Einschränkungen des EPD berücksichtigen:
- Dokumente können nicht gemeinschaftsübergreifend gespeichert werden
- Patienten und Patientinnen sind berechtigt Dokumente zu löschen
- Patienten und Patientinnen sind berechtigt die Sichtbarkeit der Dokumente
über die Vertraulichkeitsstufe zu steuern, insbesondere für Dokumente auch die
Vertraulichkeitsstufe "geheim" einzustellen
- Patienten und Patientinnen müssen Gesundheitsfachpersonen direkt oder über
eine Gruppenberechtigung für den Zugriff auf ihr EPD zu berechtigen

### Funktionsweise

Im laufenden Betrieb (z.B. zur Anzeige des Impfausweises) lädt das Impfmodul die
Impfdaten aus dem EPD der Patienten und Patientinnen und zeigt die Daten in den UI
des Impfmoduls an.

Welche Dokumente dabei aus dem EPD der Patienten und Patientinnen geladen werden
ist durch die Autorisierungsmechanismen des EPD geregelt. Dabei gilt:
- Patienten und Patientinnen können alle Impfdokumente im Impfmodul visualisieren
- Gesundheitsfach- und Hilfspersonen können nur die Impfdokumente im Impfmodul
visualisieren, für welche sie der Patient oder die Patientin entweder direkt oder
über eine Gruppe berechtigt hat

Das Impfmodul übrrsteuert die o.g. Regeln nicht und visualisiert die Impfdaten
ausschliesslich auf der Basis der Dokumente, welche aus dem EPD der Patienten und
Patientinnen geladen werden können. Dabei wertet das Impfmodul die Regeln
des Lifecycles von Impfdokumenten aus.

### Lifecycle

Die Regeln des Lifecycles von Impfdaten müssen berücksichtigt werden, um den
Impfausweis im EPD abbilden zu können, insbesondere weil:
- Dokumente nicht gemeinschaftsübergreifend gespeichert werden können
- Dokumente im EPD nicht geändert werden können und mit neuen Versionen überschrieben werden müssen
- Gesundheitsfach- und Hilfspersonen Dokumente im EPD nicht löschen können

Impfdokumente aus dem EPD von Patienten und Patientinnen können einen gegenseitigen
Bezug aufeinander haben, insbesondere:
- Impfdaten in einem Dokument können die Impfdaten aus einem anderen Dokument
korrigieren oder ergänzen
- Impfdaten in einem Dokument können die Impfdaten aus einem anderen Dokument
annullieren
- Impfdaten in einem Dokument können die Impfdaten aus einem anderen Dokument
kommentieren

Der Lifecycle von Impfdaten wird daher technisch mit Attributen in Impfdokumenten
wie folgt abgebildet:
- **TODO: Beschreibung der einzelnen Fälle**
