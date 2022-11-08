## Lifecyclemanagement der Impfdaten

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

Der Lifecycle von Impfdaten wird daher technisch mit Attributen in den Impfdokumenten
wie folgt abgebildet:

#### Level Composition

Eine Datenstruktur vom Typ Composition kann auf eine andere Datenstruktur vom Typ
Composition verweisen. Der Verweis ist im Element **relatesTo** mit den folgenden
Attributen angegeben:

- **relatesTo[0].code** : Im Impfmodul wird nur der Wert **replaces** verwendet
- **relatesTo[0].targetReference.reference** : Die UUID der Composition auf die verwiesen wird

Dabei muss der Verweis im Impfmodul eindeutig sein. D.h. eine Composition kann immer
nur auf genau eine andere Composition verweisen, nicht auf mehrere.  

##### Beispiel

Siehe Zeile 56 im [Testbeispiel für Updates](../Testfiles/lifecycle/Update-f852a5a7-16ea-46a2-9f0b-e1805b3e96b1.json):

```
"relatesTo": [ {
  "code": "replaces",
  "targetReference": {
    "reference": "urn:uuid:e1141328-d12a-46bf-8034-a301fbfb8734"
  }
 } ]
```

Diese Relation verweist auf die Composition im [Testbeispiel für die Erstellung](../Testfiles/lifecycle/Create-6214bb05-3858-480c-aa63-2450dde50e25.json).


#### Level Immunization Ressource

Eine Ressource vom Typ Immunization kann auf eine andere Ressource vom Typ Immunization
verweisen. Der Verweis ist in einem Element vom Typ **extension** mit den folgenden
Attributen angegeben:

- **url** : Muss den Wert **http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference** haben um als Referenz erkannt zu werden.

Der Verweis muss in einer untergeordneten **extension** mit den folgenden 3 Elementen
angegeben werden:

##### Entry

- **extension.url** : Muss den Wert **entry** haben.
- **extension.valueReference.reference** : UUID der Ressource vom Typ Immunization auf der Verweis zeigt.

##### Document

- **extension.url** : Muss den Wert **document** haben.
- **extension.valueReference.reference** : Die UUID der Composition auf die der Verweis zeigt.

##### Relation Code

- **extension.url** : Muss den Wert **relationcode** haben.
- **extension.valueCode** : Muss den Wert **replaces** haben.

##### Beispiel

Siehe Zeile 176 im [Testbeispiel für Updates](../Testfiles/lifecycle/Update-f852a5a7-16ea-46a2-9f0b-e1805b3e96b1.json):

```
"extension": [
{
  "url": http://fhir.ch/ig/ch-vacd/StructureDefinition/ch-vacd-ext-cross-reference,
  "extension": [ {
    "url": "entry",
      "valueReference": {
        "reference": "urn:uuid:1d1a586c-7526-4101-bb8e-174610e7babc"
      }
    }, {
      "url": "document",
      "valueReference": {
        "reference": "urn:uuid:e1141328-d12a-46bf-8034-a301fbfb8734"
      }
    }, {
      "url": "relationcode",
      "valueCode": "replaces"
    } ]
]    
```

Diese Relation verweist auf eine Immunization Resource in der Composition im [Testbeispiel für die Erstellung](../Testfiles/lifecycle/Create-6214bb05-3858-480c-aa63-2450dde50e25.json).


**TODO**
- Prüfen der Dokumentation
- Redundanz des Verweises auf die Composition
- Option der Referenz auf ein Dokument auf Level Composition
