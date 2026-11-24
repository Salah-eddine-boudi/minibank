# ADR-009 — Espace client en React, back-office en Angular

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 13/11/2026 | Salah Eddine Boudi |

## Contexte

Deux publics : des clients (application grand public, mobile d'abord) et des conseillers (application métier riche, formulaires, tableaux). Le CV met en avant React et TypeScript ; les DSI bancaires et les ESN françaises utilisent beaucoup Angular pour les back-offices.

## Décision

L'**Espace client** est en **React + TypeScript** (Vite). Le **back-office** est en **Angular** (version stable en cours). Les deux consomment uniquement la passerelle API et s'authentifient par OIDC (Keycloak).

## Conséquences

- Positives : les deux frameworks les plus demandés sont pratiqués sur un vrai cas ; chaque front est adapté à son public.
- Vigilance : deux chaînes de build front ; règles communes (lint, tests, accessibilité ENF-11) à appliquer aux deux.

## Alternatives écartées

- Un seul framework pour les deux : plus simple, mais n'apporte qu'une compétence.
