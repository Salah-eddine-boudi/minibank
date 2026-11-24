# ADR-012 — Une base de données par service

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 13/11/2026 | Salah Eddine Boudi |

## Contexte

Règles 3 et 4 des blocs applicatifs (cours 3) : un bloc a ses propres données et ne fournit que celles dont il est propriétaire.

## Décision

Chaque service possède **sa** base (ou au minimum son schéma et son utilisateur dédiés sur une instance partagée en développement). Aucun service ne lit ni n'écrit la base d'un autre ; il passe par son API ou ses événements.

## Conséquences

- Positives : chaque service évolue et se déploie seul ; la propriété des données est claire (gouvernance).
- Vigilance : plus de transaction SQL entre services ; cohérence par saga et compensation (E4-E5) ; rapports transverses via l'entrepôt (E7).

## Alternatives écartées

- Base partagée : simple au début, mais couplage impossible à défaire ensuite.
