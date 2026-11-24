# ADR-010 — Service Notifications en Node.js (NestJS)

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 13/11/2026 | Salah Eddine Boudi |

## Contexte

Les notifications consomment des événements Kafka et envoient des e-mails : un service d'entrées-sorties, sans calcul lourd, idéal pour Node.js. Le CV mentionne Node.js.

## Décision

Le Service Notifications est écrit en **TypeScript avec NestJS** (structure en modules proche de Spring), consomme `virements.v1.executes` et `fraude.v1.alertes`, envoie par SMTP (Mailpit en développement).

## Conséquences

- Positives : Node.js pratiqué dans un rôle où il excelle ; TypeScript partagé avec les fronts.
- Vigilance : idempotence obligatoire (un même événement ne doit pas envoyer deux e-mails).

## Alternatives écartées

- Spring Boot : cohérent mais sans nouvelle compétence.
