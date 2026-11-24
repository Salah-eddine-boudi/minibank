# ADR-014 — Bonita pour les processus à tâches humaines

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 13/11/2026 | Salah Eddine Boudi |

## Contexte

Certains processus impliquent des personnes, des délais et des escalades (arrivée et départ d'un conseiller, ouverture de compte). Le cours ITSM utilise Bonitasoft.

## Décision

Les processus à **tâches humaines** sont modélisés en BPMN 2.0 et exécutés dans **Bonita Community**. Les tâches automatiques appellent les API MiniBank par des connecteurs REST, via la passerelle. Les processus **entièrement automatiques** (le virement) restent dans le code des services ; leur BPMN sert de spécification.

## Conséquences

- Positives : même outil pour l'examen et le projet ; séparation claire entre orchestration humaine et logique métier.
- Vigilance : ne jamais déplacer une règle de gestion dans le BPMS.

## Alternatives écartées

- Camunda ou Flowable : très utilisés en Java, à citer en entretien ; Bonita retenu pour l'alignement avec le cours.
