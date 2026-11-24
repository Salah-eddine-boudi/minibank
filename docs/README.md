# Documentation de MiniBank

Une sélection du dossier du système d'information de la **Banque du Nord Numérique** (banque fictive) :
les documents qui expliquent **ce que fait** MiniBank, **comment il est conçu** et **pourquoi**.
Tout est en Markdown, lisible directement sur GitHub ; les diagrammes UML sont fournis avec leur source PlantUML.

## Par où commencer

| Ordre | Document | Pourquoi |
|---|---|---|
| 1 | [Cahier des charges](cahier-des-charges.md) | Exigences EF/ENF, règles de gestion RG-01 à RG-15, diagrammes UML |
| 2 | [Dossier d'architecture](architecture/dossier-architecture.md) | Vues C4, choix techniques et leur justification |
| 3 | [Décisions d'architecture (ADR)](adr/) | 14 décisions datées, dont une remplacée (ADR-007 → ADR-008) |
| 4 | [Conception objet](conception-objet.md) | Le modèle métier riche du Service Comptes (héritage, interfaces, patrons) |

## Tous les documents

| Casquette | Document | Contenu |
|---|---|---|
| MOA | [Cahier des charges](cahier-des-charges.md) | Besoins, règles de gestion, cas d'utilisation, états, séquences |
| Architecte applicatif | [Conception objet](conception-objet.md) | Classes `Compte`, `CompteCourant`, `LivretEpargne`, `Montant`… |
| Architecte | [Dossier d'architecture](architecture/dossier-architecture.md) | Contexte, conteneurs, principes, registre des ADR |
| Architecte données | [Modèle de données](architecture/modele-de-donnees.md) | Tables, contraintes, `NUMERIC(19,2)`, audit |
| Architecte API | [Conventions d'API](architecture/conventions-api.md) · [contrat OpenAPI](api/openapi.yaml) · [requêtes de test](api/minibank.http) | REST `/api/v1`, Problem Details (RFC 9457), idempotence |
| Urbaniste | [Cartographie fonctionnelle](urbanisation/cartographie-fonctionnelle.md) · [applicative et flux](urbanisation/cartographie-applicative-et-flux.md) | Zones, quartiers, blocs ; matrice des flux F01 à F17 |
| Testeur | [Stratégie de tests et recette](tests/strategie-tests-et-recette.md) | Pyramide de tests, Testcontainers, définition de « fini » |
| RSSI, DPO | [Guide de sécurité](securite/guide-securite.md) · [Risques et RGPD](securite/risques-rgpd.md) | Menaces, mesures, données personnelles |

## Schémas

- [`schemas/uml/`](schemas/uml/) : 17 diagrammes UML et C4 (`.png`) et leur source PlantUML (`.puml`).
- [`schemas/urbanisation/`](schemas/urbanisation/) : cartographies fonctionnelle et applicative, schéma des flux.
- [`schemas/bpmn/`](schemas/bpmn/) : ouverture de compte, virement avec contrôle de fraude.

> Les blocs `::: …` sont des encadrés (définition, point clé, attention) : ils s'affichent en texte simple sur GitHub.
>
> Projet d'apprentissage : la banque, ses clients et toutes les données sont fictifs.
