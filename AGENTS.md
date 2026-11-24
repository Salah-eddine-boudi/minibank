# AGENTS.md — MiniBank (instructions communes à tous les agents de code)

> Lu automatiquement par **Codex** (fichier `AGENTS.md`) et par **Claude Code** (importé par `CLAUDE.md`).
> Une seule source de vérité : on modifie les règles ici, jamais en double ailleurs.

## Le projet

MiniBank est le système d'information d'une banque fictive 100 % en ligne (Banque du Nord Numérique),
construit comme dans une grande entreprise : services Java/Spring, fronts React et Angular, fraude en Python,
notifications NestJS, Kafka, Keycloak, supervision. Aucune donnée personnelle réelle.

La documentation complète (hors dépôt) est dans `D:\M2_S9\Mini-Bank` :
- état du projet : `01_Pilotage_et_organisation/ETAT_PROJET.md`
- référence du SI : `01_Pilotage_et_organisation/MiniBank_SI_reference.md`
- règles de gestion RG-xx : `03_Besoins_et_conception/_sources/01_Cahier_des_charges_MiniBank.md`
- modèle objet : `03_Besoins_et_conception/_sources/05_Conception_objet_appliquee.md`
- ADR : `05_Architecture/adr/` · conventions d'API : `05_Architecture/_sources/05_Conventions_API.md`
- guide de développement : `08_Developpement_et_tests/_sources/02_Guide_developpement.md`
- tests et définition de « fini » : `08_Developpement_et_tests/_sources/03_Strategie_tests_et_recette.md`

## Coordination entre agents (Claude Code, Codex…) — OBLIGATOIRE

Plusieurs agents travaillent sur ce dépôt, parfois en même temps. Fichiers partagés :
- `D:\M2_S9\Mini-Bank\01_Pilotage_et_organisation\agents\TABLEAU_AGENTS.md` : qui travaille sur quoi.
- `D:\M2_S9\Mini-Bank\01_Pilotage_et_organisation\agents\PASSATION.md` : notes de reprise.

**Au démarrage**, avant toute modification :
1. Lis `TABLEAU_AGENTS.md` et `PASSATION.md` (la note la plus récente est en haut).
2. Lance `git status` et `git log --oneline -10` pour voir où en est ta branche.
3. Vérifie que tu es dans **ton** worktree et sur **ta** branche (ligne du tableau). Ne travaille jamais
   sur `main` directement.
4. Ne modifie aucun fichier réservé par une autre ligne « en cours » du tableau. En cas de doute, demande.

**Avant de t'arrêter** (fin de séance, ou quand l'utilisateur change d'outil) :
1. Ajoute une note **en haut** de `PASSATION.md` avec le modèle du fichier (fait, reste à faire,
   pièges, commandes utiles, état des tests).
2. Mets à jour ta ligne de `TABLEAU_AGENTS.md` (statut, fichiers réservés).
3. Propose un commit (sans le faire sans accord) : le travail non commité n'est visible que dans ce worktree.

## Structure du dépôt

```
minibank/
├── AGENTS.md, CLAUDE.md      instructions des agents
├── .github/                  CI (workflows/ci.yml), modèles de PR et d'issues
├── comptes-service/          service Comptes — Spring Boot 4.1.1, Java 25, package com.minibank.comptes
├── infra/docker-compose.yml  PostgreSQL 17 (port hôte 5433 → 5432), secrets lus dans infra/.env
├── .env.example              modèle de infra/.env (le vrai .env n'est jamais commité)
└── docs/                     sélection de la documentation (conception, architecture, ADR, schémas)
```

## Commandes (PowerShell)

- Une fois : `Copy-Item .env.example infra\.env` puis renseigner le mot de passe.
- `docker compose up -d` (dans `infra`) démarre la base ; `docker compose ps` vérifie.
- Dans `comptes-service` :
- `.\mvnw.cmd test` lance les tests ; `.\mvnw.cmd spring-boot:run` démarre le service.
- `curl.exe http://localhost:8080/actuator/health` doit répondre `{"status":"UP"}`.
- Deux agents en parallèle : le second démarre avec `.\mvnw.cmd spring-boot:run "-Dspring-boot.run.arguments=--server.port=8081"`.
  Les tests Testcontainers prennent des ports aléatoires : pas de conflit.

## Règles de code (non négociables)

- Montants en `BigDecimal` (colonnes `NUMERIC(19,2)`, arrondi `HALF_EVEN`) : jamais `double` ni `float` (ADR-003).
- Modèle métier riche : les règles RG-xx vivent dans les objets (`Compte.debiter`…), pas de `setSolde`.
- Verrouillage optimiste `@Version` sur les entités modifiées en concurrence (ADR-004).
- Schéma géré par **Flyway** ; `spring.jpa.hibernate.ddl-auto=validate`.
- Tests d'intégration avec **Testcontainers** (PostgreSQL réel), jamais H2.
- Erreurs d'API en Problem Details (RFC 9457) ; API versionnées `/api/v1/…` ; contrat OpenAPI à jour.
- Une base par service ; un service ne lit ni n'écrit jamais la base d'un autre (ADR-012).
- Aucun secret dans Git (`.env` ignoré, `.env.example` versionné).
- Images Docker avec une version précise, jamais `latest`.
- Chaque règle de gestion touchée a son test ; le nom du test porte le numéro de la règle
  (ex. `RG03_debit_refuse_si_solde_insuffisant`). Écrire le test avant le code.

## Git

- Une branche `feature/<sujet>` (ou `fix/`, `chore/`, `docs/`) par user story, dans son propre worktree.
- Commits Conventional Commits : `feat(comptes): …`, `fix: …`, `test: …`, `docs: …`, `ci: …`, `chore: …`.
- **Demander l'accord de l'utilisateur avant tout commit, push, fusion, suppression de fichier ou de volume Docker.**
- `main` reste toujours verte ; fusion par pull request.

## Documentation

- Décision difficile à défaire → nouvel ADR dans `D:\M2_S9\Mini-Bank\05_Architecture\adr\` ; on ne modifie jamais un ancien ADR.
- Nouveau flux entre applications → le signaler (matrice des flux du référentiel Excel).
- Ne jamais modifier les PDF de la documentation ; modifier la source `.md` et signaler qu'il faut régénérer le PDF.
