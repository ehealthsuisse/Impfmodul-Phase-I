# Testfiles

Sammlungen von Dokumenten, welche vom Impfmodul ertellt wurden.  

## Validierung

java -jar validator_cli.jar record/Bundle-RDA01.json -version 4.0 -ig http://fhir.ch/ig/ch-vacd/ImplementationGuide/ch.fhir.ig.ch-vacd

java -jar validator_cli.jar record/generated.json -version 4.0 -ig http://fhir.ch/ig/ch-vacd/ImplementationGuide/ch.fhir.ig.ch-vacd
