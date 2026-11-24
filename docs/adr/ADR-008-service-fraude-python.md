# ADR-008 — Service Fraude en Python (FastAPI)

| Statut | Date | Décideur |
|---|---|---|
| Acceptée — remplace ADR-007 | 13/11/2026 | Salah Eddine Boudi |

## Contexte

Le CV cible désormais un poste d'ingénieur logiciel full stack (Java, TypeScript, Node.js, Python, SQL). La fraude repose d'abord sur des règles (RG-12), puis sur un modèle statistique entraîné sur des données synthétiques : l'écosystème Python (pandas, scikit-learn) est le standard de ce domaine.

## Décision

Le Service Fraude est écrit en **Python avec FastAPI**, expose `POST /api/v1/evaluations` décrit en OpenAPI, et publie les alertes dans Kafka (`fraude.v1.alertes`).

## Conséquences

- Positives : compétence Python démontrée sur un cas réaliste ; passage naturel des règles au modèle statistique ; contrat d'API identique quel que soit le langage.
- Vigilance : une troisième chaîne d'outillage (pip ou uv, pytest) dans la CI ; typage à discipliner (mypy, Pydantic).

## Alternatives écartées

- C# ASP.NET Core (ADR-007) : gardé en bonus si un poste .NET est visé.
- Java : n'apporte pas de nouvelle compétence.
