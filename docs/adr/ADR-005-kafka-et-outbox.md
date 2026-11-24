# ADR-005 — Kafka et pattern outbox pour les événements

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 11/10/2026 | Salah Eddine Boudi |

## Contexte

Après un virement, plusieurs services doivent réagir (notifications, audit), et d'autres pourront s'ajouter. Les appeler un par un en synchrone ralentirait le virement et le ferait échouer si l'un d'eux est en panne.

## Décision

Le service Virements publie un événement **`VirementExecute`** dans **Kafka** (topic `virements`, clé = numéro du compte source pour garder l'ordre par compte). L'événement est d'abord écrit dans une table **`outbox_evenement`** dans la même transaction que le changement de statut, puis publié par un relais. Les consommateurs sont **idempotents** (ils ignorent un `eventId` déjà traité).

## Conséquences

**Positives**

- Découplage : on ajoute un consommateur sans toucher au service Virements.
- Aucun événement perdu, même en cas de panne entre la base et Kafka.
- Rejeu possible des événements.

**Négatives ou points de vigilance**

- Infrastructure supplémentaire (Kafka) à faire tourner.
- Cohérence à terme : l'audit est à jour quelques millisecondes après le virement, pas instantanément.
- Il faut gérer les doublons côté consommateur.

## Alternatives écartées

- Appels REST synchrones vers chaque service : couplage fort.
- RabbitMQ : très bien aussi, mais Kafka est plus demandé dans les offres visées et permet le rejeu.
