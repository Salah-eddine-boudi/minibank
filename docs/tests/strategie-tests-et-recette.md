---
title: "Stratégie de tests et plan de recette — MiniBank"
subtitle: "Pyramide des tests, conventions, données de test, cas de recette et gestion des anomalies"
author: "Kit projet MiniBank"
date: "Octobre 2026"
lang: fr-FR
---

# Pourquoi tester ?

Un test automatisé est une **preuve exécutable** qu'une règle est respectée, rejouée à chaque modification. Dans une banque, on ne demande pas « est-ce que ça marche ? » mais « **comment le prouves-tu ?** ».

# La pyramide des tests

| Niveau | Ce qu'on vérifie | Vitesse | Quantité | Outils |
|---|---|---|---|---|
| **Unitaire** (base) | Une classe isolée : règles de `Compte`, logique de `CompteService` avec des faux repositories | Millisecondes | Beaucoup | JUnit 5, AssertJ, Mockito |
| **Intégration** (milieu) | Plusieurs couches réelles : HTTP → service → base | Secondes | Moyen | `@SpringBootTest`, MockMvc, Testcontainers |
| **Bout en bout** (sommet) | Un parcours complet dans le navigateur | Dizaines de secondes | Peu | Playwright ou Cypress |

Plus on monte, plus le test est réaliste, mais lent et fragile. On met donc **beaucoup de tests unitaires**, et seulement quelques tests de bout en bout sur les parcours critiques (le virement).

## Les autres familles de tests

| Type | Question | Phase |
|---|---|---|
| Test de non-régression | Ce qui marchait marche-t-il encore ? | Toute la suite, à chaque *push* |
| Test de concurrence | 100 virements simultanés laissent-ils un solde exact ? | P1 (bonus) |
| Test de contrat | Le service Fraude respecte-t-il le contrat attendu par Virements ? | P6 |
| Test de charge | Tenons-nous 20 utilisateurs simultanés sous 300 ms ? | P8 |
| Test de sécurité | Les accès interdits sont-ils bien refusés ? | P3 |
| Test de mutation | Mes tests détectent-ils un code volontairement cassé ? | P7 (PIT, optionnel) |

# Conventions

**Structure AAA** : chaque test a trois blocs : *Arrange* (préparer), *Act* (agir), *Assert* (vérifier).

**Nommage** : le nom décrit le comportement, lisible comme une phrase : `virement_refuse_si_meme_compte()`, `retrait_au_solde_exact_laisse_zero()`.

**Un test = un comportement.** S'il échoue, on doit savoir ce qui est cassé sans lire le code.

**Tester les limites** : pour une règle « solde ≥ montant », tester juste en dessous (100,01), pile (100,00) et au-dessus (99,99). La plupart des bogues sont aux frontières.

**Indépendance** : un test ne dépend pas de l'ordre ni du résultat d'un autre. Chaque test prépare ses propres données.

## Le partitionnement en classes d'équivalence

Pour le montant d'un virement, plutôt que tester 1 000 valeurs, on teste un représentant de chaque classe :

| Classe | Exemple | Résultat attendu |
|---|---|---|
| Négatif | -5 | 400 |
| Zéro | 0 | 400 |
| Plus de 2 décimales | 12,345 | 400 |
| Valide, inférieur au solde | 250,00 | 201 |
| Égal au solde | 1 000,00 (solde 1 000) | 201, solde 0 |
| Supérieur au solde | 1 000,01 | 422 SOLDE_INSUFFISANT |
| Supérieur au plafond par virement | 5 000,01 | 422 PLAFOND_DEPASSE (P4) |

# Couverture de code

La couverture mesure le pourcentage de lignes exécutées par les tests (outil **JaCoCo**). Objectif : **80 % sur les couches domaine et service** (ENF-09).

> **Attention.** 100 % de couverture ne prouve rien si les tests ne vérifient rien. Un test sans assertion « couvre » des lignes sans rien tester. La couverture indique ce qui n'est **pas** testé ; elle ne garantit pas la qualité de ce qui l'est.

# Données de test

- Toujours **fictives** et **cohérentes** (le solde égale la somme des opérations) : voir `05_Base_de_donnees/V2__donnees_de_test.sql`.
- Chaque test d'intégration part d'un état connu : base vidée ou transaction annulée à la fin du test.
- Les scénarios de l'analyse des besoins (Claire, le propriétaire, le compte joint, la carte volée) servent de jeux de données de recette.

# Plan de recette

La recette vérifie, du point de vue du client, que chaque exigence est respectée. Chaque cas de test (CT) est relié à une exigence ou une règle : c'est la matrice de traçabilité. La version suivie (statut, date, anomalies) est dans l'onglet « Recette » de `13_Outils/MiniBank_Pilotage.xlsx`.

| ID | Exigence | Préconditions | Étapes | Résultat attendu |
|---|---|---|---|---|
| CT-01 | EF-01 | Aucune | Ouvrir un compte COURANT pour « Claire Martin » | 201, numéro MB + 10 chiffres, solde 0,00, statut ACTIF |
| CT-02 | EF-01 | Aucune | Ouvrir un compte sans titulaire | 400, champ `titulaire` signalé |
| CT-03 | EF-02 | Compte MB0000000001 existe | Consulter le compte | 200, données exactes |
| CT-04 | EF-02 | — | Consulter MB9999999999 | 404 COMPTE_INTROUVABLE |
| CT-05 | EF-04 | Solde 1 200,00 | Déposer 100,00 | 200, solde 1 300,00, opération DEPOT créée |
| CT-06 | RG-02 | — | Déposer -5, 0, 12,345 | 400 à chaque fois, solde inchangé |
| CT-07 | EF-05 | Solde 1 200,00 | Retirer 1 200,00 | 200, solde 0,00 |
| CT-08 | RG-03 | Solde 1 200,00 | Retirer 1 200,01 | 422 SOLDE_INSUFFISANT, solde inchangé |
| CT-09 | EF-06 | 25 opérations sur le compte | Historique page 0 puis page 1 | 20 puis 5 lignes, ordre antichronologique |
| CT-10 | EF-07 | Compte ACTIF | Bloquer avec motif, puis retirer 10,00 | Blocage 200 ; retrait 422 COMPTE_INDISPONIBLE |
| CT-11 | RG-06 | Compte BLOQUE | Déposer 50,00 | 200 : un compte bloqué peut recevoir |
| CT-12 | EF-08, RG-07 | Solde 0,01 | Clôturer | 422 CLOTURE_IMPOSSIBLE |
| CT-13 | EF-08 | Solde 0,00 | Clôturer, puis déposer | Clôture 200 ; dépôt 422 COMPTE_INDISPONIBLE |
| CT-14 | EF-10 | A = 1 000,00 ; B = 50,00 | Virer 250,00 de A vers B | 201 ; A = 750,00 ; B = 300,00 ; 2 opérations même référence |
| CT-15 | RG-04 | — | Virer de A vers A | 422 MEME_COMPTE |
| CT-16 | RG-09 | A = 1 000,00 | Virer 1 000,01 de A vers B | 422 ; A et B inchangés ; aucune opération créée |
| CT-17 | EF-11, RG-10 | — | Envoyer deux fois le même virement avec la même clé | Réponses identiques, un seul débit |
| CT-18 | RG-10 | Virement avec clé K déjà fait | Autre montant avec la clé K | 409 IDEMPOTENCE_CONFLIT |
| CT-19 | ENF-02 | A = 10 000,00 | 100 virements simultanés de 100,00 | A = 0,00 ; 100 opérations ; somme des soldes A + B constante |
| CT-20 | EF-12, RG-05 | 8 000,00 déjà virés ce jour | Virer 2 500,00 puis 2 000,00 | 422 PLAFOND_DEPASSE puis 201 |
| CT-21 | EF-23 | Jeton de Claire | Consulter le compte de Thomas | 404, ligne d'audit |
| CT-22 | EF-22 | Jeton CLIENT | Bloquer un compte | 403 |
| CT-23 | ENF-04 | — | Appeler l'API sans jeton, puis avec un jeton modifié | 401 dans les deux cas |
| CT-24 | EF-51, RG-11 | — | Virer 3 500,00 vers un nouveau bénéficiaire à 3 h | 422 RISQUE_FRAUDE |
| CT-25 | EF-40 | — | Faire un virement puis consulter l'audit | Ligne VIREMENT_EXECUTE avec acteur, date, référence |
| CT-26 | EF-30 | Mailpit lancé | Faire un virement | Un e-mail de notification visible dans Mailpit |
| CT-27 | ENF-07 | — | Faire un virement, lire les logs | Numéros masqués, aucun jeton dans les logs |

# Gestion des anomalies

## Sévérité

| Niveau | Définition | Exemple | Délai de correction visé |
|---|---|---|---|
| **Bloquante** | Empêche une fonction essentielle, ou perte et corruption de données | Un virement débite sans créditer | Immédiat, bloque la livraison |
| **Majeure** | Fonction importante dégradée, contournement difficile | Historique faux après un filtre | Avant la livraison |
| **Mineure** | Gêne limitée, contournement simple | Message d'erreur mal formulé | Prochain sprint |
| **Cosmétique** | Présentation | Alignement d'une colonne | Quand possible |

## Le cycle de vie d'une anomalie

Nouvelle → Qualifiée (reproduite, sévérité fixée) → En correction → Corrigée → Vérifiée (retestée par quelqu'un d'autre que le développeur) → Fermée. Si le retest échoue : Réouverte.

Une bonne fiche d'anomalie permet à n'importe qui de **reproduire** le problème : voir `10_Gestion_de_projet/modeles/Fiche_anomalie.md`.

**Règle d'or** : tout bogue corrigé reçoit un test automatisé qui l'aurait détecté. Ainsi il ne revient jamais.

# Qualité du code

| Outil | Rôle | Phase |
|---|---|---|
| SonarQube for IDE (VS Code) | Signale bogues et failles pendant la frappe | P1 |
| JaCoCo | Mesure la couverture | P7 |
| SonarCloud | Analyse complète à chaque *push* : bogues, vulnérabilités, duplications, dette technique ; **quality gate** qui bloque si les seuils ne sont pas atteints | P7 |
| Dependabot / OWASP Dependency-Check | Dépendances vulnérables | P7 |
| GitHub Actions | Exécute tout cela automatiquement | P7 |
