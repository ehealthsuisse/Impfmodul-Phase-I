## Systemarchitektur

Das Impfmodul ist als Rich Internet Applikation implementiert und erfüllt die folgenden Kriterien:

1. Die Fach- und Kommunikationslogik wird als Web Applikation auf dem Web Applikationsserver betrieben.
2. Die Präsentationslogik nutzt Standard Web Browser mit html und Javascript bzw. Angular Komponenten.
3. Die Angular Komponenten der Präsentationslogik kommunizieren über eine RESTful API mit der Web Fach- und Kommunikationslogik der Web Applikation.   

![Bild: Impfmoduls Architektur](Images/ria-1.JPG)

Abbildung: Skizze der Architektur des Impfmoduls
