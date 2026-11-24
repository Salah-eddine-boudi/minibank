---
title: "Modèle de données — MiniBank"
subtitle: "Du concept à la table : MCD, MLD, normalisation, contraintes, transactions"
author: "Kit projet MiniBank"
date: "Octobre 2026"
lang: fr-FR
---

# Les trois niveaux de modélisation

En France, on parle souvent de la méthode **Merise**, qui distingue trois niveaux. Le diagramme de classes UML joue le rôle du niveau conceptuel.

| Niveau | Question | Ce qu'on y met | Dans le kit |
|---|---|---|---|
| **MCD** (conceptuel) | Quelles informations le métier manipule-t-il ? | Entités, propriétés, associations, cardinalités | Diagramme 05 (classes du domaine) |
| **MLD** (logique) | Comment l'organiser en tables ? | Tables, colonnes, clés primaires et étrangères | Diagramme 12 |
| **MPD** (physique) | Comment l'écrire pour PostgreSQL ? | Types exacts, index, contraintes, triggers | `V1__schema_comptes.sql` |

## Passer du conceptuel au logique : les 4 règles

1. **Une entité devient une table**, son identifiant devient la clé primaire (`compte.id`).
2. **Une association 1–n** : la clé de la table côté « 1 » descend comme clé étrangère côté « n » (`compte.client_id` → `client.id`).
3. **Une association n–n** devient une table d'association avec les deux clés (exemple futur : un compte joint avec plusieurs titulaires → table `titulaire_compte (client_id, compte_id)`).
4. **Une énumération** devient une colonne texte avec une contrainte `CHECK` (ou une table de référence si les valeurs évoluent souvent).

# La normalisation, expliquée avec MiniBank

La normalisation évite la **redondance** (la même information stockée plusieurs fois) et donc les **incohérences**.

**Mauvais exemple de départ** : une seule table « tout-en-un ».

| numero_compte | nom_client | email_client | operations |
|---|---|---|---|
| MB0000000001 | Claire Martin | claire@… | « dépôt 1700 ; virement -500 » |
| MB0000000002 | Claire Martin | claire@… | « dépôt 3000 ; virement +500 » |

**1NF — valeurs atomiques.** Une cellule ne contient qu'une seule valeur. Ici, `operations` contient une liste : impossible de calculer une somme. → On crée une table `operation`, une ligne par mouvement.

**2NF — tout dépend de toute la clé.** Si une table a une clé composée (par exemple `(numero_compte, date_operation)`), chaque colonne doit dépendre des deux parties, pas d'une seule. Le nom du client dépend du compte seulement, pas de la date : il ne doit pas être dans la table des opérations.

**3NF — pas de dépendance entre colonnes non clés.** Dans la table des comptes, `email_client` dépend de `nom_client`, pas du compte : si Claire change d'e-mail, il faudrait le modifier sur chacun de ses comptes, et un oubli crée une incohérence. → On crée une table `client`, et `compte` ne garde que `client_id`.

**BCNF (forme normale de Boyce-Codd).** Tout ce qui détermine une autre colonne doit être une clé candidate. Exemple : si chaque conseiller ne gérait qu'une agence, et qu'on stockait `(client, conseiller, agence)`, alors `conseiller → agence` sans que « conseiller » soit une clé : on sortirait `agence` dans une table `conseiller`.

> **La dénormalisation volontaire.** La colonne `operation.solde_apres` est techniquement redondante (on pourrait la recalculer). On la garde exprès : les relevés et les contrôles sont plus rapides et plus simples. Dénormaliser est acceptable **quand c'est un choix documenté**, pas un oubli. Le contrôle n° 6 de `requetes_utiles.sql` vérifie justement que la redondance reste cohérente.

# Les choix de types

| Donnée | Type PostgreSQL | Type Java | Pourquoi |
|---|---|---|---|
| Identifiant technique | `BIGSERIAL` | `Long` | Auto-incrémenté, assez grand pour des milliards de lignes |
| Montant | `NUMERIC(19,2)` | `BigDecimal` | Exact au centime (ADR-003) |
| Date et heure | `TIMESTAMPTZ` | `Instant` | Instant universel, sans ambiguïté de fuseau ni de changement d'heure |
| Référence de virement | `UUID` | `UUID` | Unique sans coordination, non devinable |
| Statut, type | `VARCHAR` + `CHECK` | `enum` + `@Enumerated(EnumType.STRING)` | Lisible en base ; ne jamais stocker l'ordinal (0, 1, 2) qui casse si on réordonne l'enum |
| Données souples | `JSONB` | `String` ou objet | Détails d'audit, contenu d'événement |

# Les contraintes : la base comme seconde barrière

Les règles métier sont d'abord dans le code (méthodes de l'entité). La base les **revérifie** : si un jour quelqu'un contourne le code (script SQL, bogue), les données restent cohérentes. C'est la **défense en profondeur** appliquée aux données.

| Règle | Dans le code | Dans la base |
|---|---|---|
| RG-01 numéro unique et au bon format | Générateur de numéro | `UNIQUE` + `CHECK (numero ~ '^MB[0-9]{10}$')` |
| RG-02 montant positif | `verifierMontant()` + `@Positive` | `CHECK (montant > 0)` |
| RG-03 solde jamais négatif | `Compte.debiter()` | `CHECK (solde >= 0)` |
| RG-04 pas de virement vers soi-même | Contrôle du service | `CHECK (compte_source <> compte_cible)` |
| RG-08 opérations immuables | Pas de méthode de modification | Trigger qui refuse UPDATE et DELETE |
| RG-10 idempotence | Recherche de la clé | `UNIQUE (cle_idempotence)` |

# Index : accélérer les lectures

Un index est comme l'index d'un livre : il évite de lire toutes les pages. Il accélère les lectures mais ralentit un peu les écritures et prend de la place.

| Index | Requête accélérée |
|---|---|
| `idx_compte_client (client_id)` | « Les comptes du client X » |
| `idx_operation_compte_date (compte_id, date_operation DESC)` | L'historique d'un compte, trié |
| `idx_virement_source_date … WHERE statut = 'EXECUTE'` | Le cumul du jour pour le plafond (index **partiel** : seulement les lignes utiles) |
| `idx_outbox_non_publies … WHERE date_publication IS NULL` | Le relais outbox |

Pour vérifier qu'un index est utilisé : `EXPLAIN ANALYZE` (requête n° 10 de `requetes_utiles.sql`). « Index Scan » : l'index sert ; « Seq Scan » sur une grosse table : il manque probablement un index.

# Transactions et concurrence

## ACID

| Lettre | Propriété | Exemple MiniBank |
|---|---|---|
| **A**tomicité | Tout ou rien | Débit et crédit d'un virement réussissent ensemble ou pas du tout |
| **C**ohérence | Les contraintes restent vraies | Jamais de solde négatif |
| **I**solation | Les transactions simultanées ne se voient pas à moitié | Un relevé ne voit pas un virement à moitié fait |
| **D**urabilité | Ce qui est validé survit à une panne | Après le COMMIT, le virement est sur disque |

## Les niveaux d'isolation (PostgreSQL)

| Niveau | Ce qu'il empêche | Remarque |
|---|---|---|
| READ COMMITTED (défaut) | Lire des données non validées | Suffisant avec le verrouillage optimiste |
| REPEATABLE READ | En plus : relire une ligne et la trouver changée | Plus d'erreurs de sérialisation à gérer |
| SERIALIZABLE | Toute anomalie, comme si les transactions étaient l'une après l'autre | Le plus sûr, le plus coûteux |

## Le problème de la mise à jour perdue

Deux virements de 400 € sur un compte de 500 € (scénario 5 de l'analyse) :

| Temps | Transaction A | Transaction B |
|---|---|---|
| t1 | lit solde = 500, version = 3 | |
| t2 | | lit solde = 500, version = 3 |
| t3 | écrit solde = 100, version = 4 (WHERE version = 3 : OK) | |
| t4 | | écrit solde = 100 (WHERE version = 3 : **0 ligne**, échec → 409) |

Sans `@Version`, B aurait écrit 100 aussi : 800 € seraient sortis d'un compte de 500 €.

# Exercices

1. Écris la requête qui liste les comptes BLOQUE avec le nom de leur titulaire.
2. Écris la requête qui donne, pour chaque type d'opération, le nombre et le montant total.
3. Pourquoi ne faut-il pas mettre de clé étrangère entre `virement.compte_source` et `compte.numero` ?
4. Ajoute au schéma la gestion des comptes joints (plusieurs titulaires). Quelle forme prend la nouvelle table ?
5. Que se passe-t-il si on stocke une enum avec `@Enumerated(EnumType.ORDINAL)` et qu'on ajoute une valeur au milieu de l'enum ?

**Corrigés.**

1. `SELECT c.numero, cl.prenom, cl.nom FROM compte c JOIN client cl ON cl.id = c.client_id WHERE c.statut = 'BLOQUE';`
2. `SELECT type, COUNT(*), SUM(montant) FROM operation GROUP BY type;`
3. Les deux tables sont dans deux bases différentes (une base par service) : c'est techniquement impossible, et ce serait un couplage fort entre services.
4. Une table d'association `titulaire_compte (client_id, compte_id, role)` avec une clé primaire composée `(client_id, compte_id)` ; on retire `compte.client_id`.
5. Les valeurs déjà stockées pointent vers la mauvaise constante (l'ancien 2 devient le nouveau 2) : données corrompues sans aucune erreur. D'où `EnumType.STRING`.
