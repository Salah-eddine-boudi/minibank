# ADR-011 — Kafka comme système de gestion des flux asynchrones (pas d'ESB)

| Statut | Date | Décideur |
|---|---|---|
| Acceptée (précise ADR-005) | 13/11/2026 | Salah Eddine Boudi |

## Contexte

Le cours d'urbanisation impose que toute communication entre blocs passe par un système de gestion des flux, et rappelle qu'un ESB, qui porte de la logique, recrée du couplage dans une architecture microservices.

## Décision

- Flux **synchrones** externes : par la **passerelle API** (Spring Cloud Gateway), contrats OpenAPI versionnés.
- Flux **asynchrones** : par **Apache Kafka**, topics nommés `<domaine>.v<version>.<evenement>`, schémas JSON versionnés, publication via outbox (ADR-005).
- Aucune logique métier dans le transport : Kafka transporte, les services décident.

## Conséquences

- Positives : couplage faible, rejeu possible, plusieurs abonnés par événement.
- Vigilance : consommateurs idempotents, ordre garanti seulement par partition (clé = numéro de compte).

## Alternatives écartées

- ESB : contraire aux principes du cours pour des microservices.
- RabbitMQ : valable, mais Kafka est plus demandé dans les SI bancaires et permet le rejeu.
