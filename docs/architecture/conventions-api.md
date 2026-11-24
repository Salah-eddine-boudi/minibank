---
title: "Conventions d'API REST — MiniBank"
subtitle: "Ressources, verbes, codes HTTP, erreurs, pagination, idempotence, versionnement"
author: "Kit projet MiniBank"
date: "Octobre 2026"
lang: fr-FR
---

# Pourquoi des conventions ?

Une API est un **contrat** : le front, les autres services et parfois des partenaires en dépendent. Des règles communes rendent l'API prévisible (« si je connais une route, je devine les autres ») et évitent les débats à chaque nouvelle route.

Le contrat complet est dans `openapi.yaml` (ouvrable dans VS Code avec l'extension « OpenAPI (Swagger) Editor », ou sur editor.swagger.io). Les appels prêts à exécuter sont dans `minibank.http` (extension VS Code « REST Client »).

# Ressources et URL

| Règle | Bon | Mauvais |
|---|---|---|
| Des **noms** au pluriel, pas des verbes | `POST /api/virements` | `POST /api/faireVirement` |
| Hiérarchie quand la ressource appartient à une autre | `GET /api/comptes/{numero}/operations` | `GET /api/operations?compte=…` (acceptable, mais moins clair) |
| Identifiant métier dans l'URL | `/api/comptes/MB0000000001` | `/api/comptes/42` (expose l'id technique) |
| Minuscules, tirets si besoin | `/api/comptes/{numero}/blocage` | `/api/Comptes/{numero}/Blocage` |
| Actions métier modélisées comme des ressources | `POST /api/comptes/{numero}/blocage` (créer un blocage), `DELETE …/blocage` (le lever) | `POST /api/comptes/{numero}/bloquer` |

# Verbes HTTP

| Verbe | Usage | Idempotent ? | Exemple |
|---|---|---|---|
| GET | Lire, sans effet de bord | Oui | Consulter un compte |
| POST | Créer, ou déclencher une action | **Non** (d'où la clé d'idempotence) | Ouvrir un compte, virer |
| PUT | Remplacer entièrement une ressource | Oui | (peu utilisé ici) |
| PATCH | Modifier partiellement | Pas forcément | Modifier l'e-mail d'un client |
| DELETE | Supprimer (ou lever un état) | Oui | Débloquer un compte |

**Idempotent** : appeler deux fois produit le même état qu'une fois. `DELETE …/blocage` deux fois laisse le compte débloqué ; `POST /api/virements` deux fois débiterait deux fois **sans** clé d'idempotence.

# Codes de statut

| Code | Signification | Quand MiniBank le renvoie |
|---|---|---|
| 200 OK | Succès avec contenu | Lecture, dépôt, retrait |
| 201 Created | Ressource créée | Compte ouvert, virement exécuté (+ en-tête `Location`) |
| 204 No Content | Succès sans contenu | Déblocage |
| 400 Bad Request | Requête mal formée ou invalide | JSON invalide, champ manquant, montant ≤ 0 |
| 401 Unauthorized | Non authentifié | Jeton absent, expiré ou invalide |
| 403 Forbidden | Authentifié mais pas le droit | Un client tente une action de conseiller |
| 404 Not Found | Ressource inexistante (ou non révélée) | Compte inconnu, ou compte d'un autre client |
| 409 Conflict | Conflit avec l'état actuel | Version modifiée en parallèle ; clé d'idempotence réutilisée pour une autre demande |
| 422 Unprocessable Content | Requête valide mais refusée par une règle métier | Solde insuffisant, plafond dépassé, risque de fraude |
| 429 Too Many Requests | Trop de requêtes | Limitation de débit de la passerelle |
| 500 Internal Server Error | Erreur imprévue | Jamais volontairement ; message générique, détail dans les logs |
| 503 Service Unavailable | Dépendance indisponible | Service Fraude ou Comptes injoignable |

> **400 ou 422 ?** 400 : la demande est mal écrite (le serveur ne peut même pas la traiter). 422 : la demande est bien écrite, mais le métier la refuse. C'est une question fréquente en entretien.

# Format des erreurs : Problem Details (RFC 9457)

Toutes les erreurs suivent le même format JSON, supporté nativement par Spring (`ProblemDetail`) :

```json
{
  "type": "https://minibank.exemple/erreurs/solde-insuffisant",
  "title": "Solde insuffisant",
  "status": 422,
  "detail": "Le solde du compte MB******0001 ne permet pas ce virement.",
  "instance": "/api/virements",
  "code": "SOLDE_INSUFFISANT",
  "traceId": "4bf92f3577b34da6"
}
```

Pour les erreurs de validation (400), on ajoute la liste des champs :

```json
{
  "title": "Requête invalide",
  "status": 400,
  "code": "VALIDATION",
  "erreurs": [
    { "champ": "montant", "message": "doit être strictement positif" }
  ]
}
```

## Codes d'erreur métier

| Code | HTTP | Règle |
|---|---|---|
| `VALIDATION` | 400 | Champs invalides |
| `MONTANT_INVALIDE` | 400 | RG-02 |
| `COMPTE_INTROUVABLE` | 404 | — |
| `MEME_COMPTE` | 422 | RG-04 |
| `SOLDE_INSUFFISANT` | 422 | RG-03 |
| `PLAFOND_DEPASSE` | 422 | RG-05 |
| `COMPTE_INDISPONIBLE` | 422 | RG-06 (compte bloqué ou clôturé) |
| `CLOTURE_IMPOSSIBLE` | 422 | RG-07 (solde non nul) |
| `LIMITE_COMPTES` | 422 | RG-15 |
| `RISQUE_FRAUDE` | 422 | RG-11 |
| `CONFLIT_VERSION` | 409 | ADR-004 |
| `IDEMPOTENCE_CONFLIT` | 409 | RG-10 |
| `CONTROLE_INDISPONIBLE` | 503 | Service Fraude injoignable |

Le **code** est stable et sert au front pour afficher le bon message ; le **detail** est lisible par un humain et peut changer. Ne jamais mettre de trace d'exception (*stack trace*) dans une réponse.

# Pagination

```
GET /api/comptes/MB0000000001/operations?page=0&size=20&du=2026-09-01&au=2026-09-30
```

Réponse :

```json
{
  "contenu": [ { "date": "2026-09-28T10:15:00Z", "type": "VIREMENT_EMIS", "montant": 650.00, "soldeApres": 550.00 } ],
  "page": 0,
  "taille": 20,
  "totalElements": 134,
  "totalPages": 7
}
```

Taille maximale : 100 par page, pour protéger le serveur (OWASP API4 : consommation de ressources non limitée).

# Idempotence des virements

Le client génère un identifiant unique (UUID) **par intention de virement** (à l'ouverture du formulaire) et l'envoie dans l'en-tête :

```
POST /api/virements
Idempotency-Key: 7f3c2a1e-9b8d-4c6f-a2e1-5d4b3c2a1f00
```

| Situation | Réponse |
|---|---|
| Première demande avec cette clé | Traitement normal (201 ou erreur) |
| Même clé, même contenu (double clic, nouvelle tentative réseau) | **Même réponse** que la première, sans nouveau débit |
| Même clé, contenu différent | 409 `IDEMPOTENCE_CONFLIT` |
| Clé absente | 400 (obligatoire sur cette route) |

# Formats de données

| Donnée | Format | Exemple |
|---|---|---|
| Montant | Nombre décimal, point comme séparateur, 2 décimales | `250.00` |
| Date et heure | ISO 8601 en UTC | `2026-10-11T08:30:00Z` |
| Date seule | ISO 8601 | `2026-10-11` |
| Numéro de compte | Chaîne | `"MB0000000001"` |
| Noms de champs JSON | camelCase français | `soldeApres`, `compteCible` |

# Versionnement

Tant qu'une API n'a qu'un consommateur (phases 1 à 3), on peut la faire évoluer librement. Dès qu'elle en a plusieurs (phase 4 : le front et le service Virements appellent Comptes), on la **versionne** : `/api/v1/comptes`. Règles :

- Ajouter un champ dans une réponse : compatible, pas de nouvelle version.
- Supprimer ou renommer un champ, changer un type, rendre un champ obligatoire : **incompatible** → nouvelle version, l'ancienne est maintenue un temps puis dépréciée.

# Sécurité de l'API (rappel)

- Toutes les routes exigent `Authorization: Bearer <jeton>` (sauf `/actuator/health`).
- HTTPS obligatoire hors poste de développement.
- CORS limité à l'origine de l'application web.
- Ne jamais renvoyer l'entité JPA : toujours un DTO (sinon fuite de champs internes).
- Détails : `08_Securite/Guide_securite.md`.
