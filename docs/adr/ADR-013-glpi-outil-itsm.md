# ADR-013 — GLPI comme outil de gestion des services IT

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 13/11/2026 | Salah Eddine Boudi |

## Contexte

Il faut un outil pour le catalogue de services, les tickets (incidents, demandes), les changements et la CMDB, gratuit et utilisable sur un poste.

## Décision

**GLPI 11**, open source, en conteneur Docker (image officielle, version fixée), base MariaDB. Les alertes de la supervision créent les tickets par l'API REST de GLPI.

## Conséquences

- Positives : outil réel, très répandu en France ; couvre tout le périmètre ITIL du cours.
- Vigilance : changer les comptes par défaut à l'installation ; sauvegarder sa base.

## Alternatives écartées

- ServiceNow (leader du marché, cité par le cours) : instance de développeur possible, mais hors de ton poste et non pérenne.
- iTop : bonne CMDB, moins répandu.
