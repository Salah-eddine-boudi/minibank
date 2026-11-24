# ADR-002 — Monolithe modulaire d'abord, microservices ensuite

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 11/10/2026 | Salah Eddine Boudi |

## Contexte

L'architecture cible est en microservices, mais le domaine n'est pas encore bien connu et l'équipe est réduite. Découper trop tôt multiplie les problèmes (réseau, transactions distribuées, déploiement) avant même d'avoir une fonctionnalité qui marche.

## Décision

Les phases 1 à 3 utilisent **un seul service** (Comptes) organisé en modules clairs. Le découpage en microservices intervient en **phase 4**, quand les règles des virements sont stabilisées et testées.

## Conséquences

**Positives**

- Fonctionnalités livrées vite, transactions locales simples (`@Transactional`).
- Le découpage se fait sur des frontières déjà comprises.
- On vit concrètement le passage au distribué, ce qui permet d'expliquer en entretien pourquoi et quand découper.

**Négatives ou points de vigilance**

- Il faudra déplacer du code en phase 4 : les modules doivent rester bien séparés dès le départ.
- Les tests d'intégration seront à adapter lors du découpage.

## Alternatives écartées

- Microservices dès le départ : complexité prématurée.
- Rester en monolithe : ne permettrait pas d'apprendre Kafka, la passerelle et la résilience.
