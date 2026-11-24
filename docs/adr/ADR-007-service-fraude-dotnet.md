# ADR-007 — Service Fraude en ASP.NET Core

| Statut | Date | Décideur |
|---|---|---|
| **Remplacée par ADR-008** (13/11/2026) | 11/10/2026 | Salah Eddine Boudi |

## Contexte

Le poste visé est « Java ou .NET ». Les systèmes d'information bancaires mélangent souvent plusieurs technologies qui communiquent par contrats d'API. Le service Fraude est autonome, avec une interface simple.

## Décision

Le service Fraude est développé en **C# avec ASP.NET Core** (version LTS), exposant `POST /api/evaluations`. Le contrat est décrit en OpenAPI et testé des deux côtés.

## Conséquences

**Positives**

- Démontre la maîtrise des deux écosystèmes demandés.
- Montre qu'une architecture microservices est indépendante des langages.

**Négatives ou points de vigilance**

- Deux chaînes d'outillage (Maven et dotnet) dans le même dépôt et la même CI.

## Alternatives écartées

- Python FastAPI : plus rapide à écrire, mais moins aligné avec le poste.
- Java : plus simple, mais n'apporte pas la compétence .NET.
