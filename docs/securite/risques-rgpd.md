---
title: "Risques, RGPD, DORA et gouvernance des données"
subtitle: "Ce que la BNN doit protéger, contre qui, comment, et comment ses données sont gérées tout au long de leur vie"
author: "Projet MiniBank — dossier 07 Sécurité et données"
date: "Novembre 2026"
---

::: casquette
**RSSI et analyste de risques** (besoins de sécurité, scénarios, mesures), **DPO** (RGPD), **data owner, data steward et data engineer** (gouvernance des données). Le *Guide de sécurité* du kit (même dossier) couvre déjà l'OWASP Top 10 et la sécurité des API ; ce document ajoute la vision risques et données.
:::

# Les besoins de sécurité : DICT

Pour chaque bien à protéger, on note de 1 (faible) à 4 (vital) le besoin en **D**isponibilité, **I**ntégrité, **C**onfidentialité et **T**raçabilité (preuve). Ton cours ITSM parle de DIC ; la banque ajoute la traçabilité, indispensable pour les audits.

| Bien essentiel | D | I | C | T | Justification |
|---|---|---|---|---|---|
| Soldes et opérations | 4 | 4 | 3 | 4 | Une erreur de solde est une perte d'argent ; chaque mouvement doit être prouvable |
| Ordres de virement | 3 | 4 | 3 | 4 | Un virement modifié en route est une fraude |
| Données d'identité des clients | 2 | 3 | 4 | 3 | Données personnelles : une fuite engage la BNN devant la CNIL |
| Identifiants de connexion | 4 | 4 | 4 | 3 | Sans eux, personne n'entre ; volés, n'importe qui entre |
| Scores et alertes de fraude | 3 | 3 | 4 | 4 | Divulguer les règles aide le fraudeur ; les décisions doivent être justifiables |
| Indicateurs de pilotage | 1 | 3 | 2 | 1 | Peu urgent, mais doit être juste |
| Tickets et CMDB | 2 | 2 | 2 | 2 | Outil interne, utile en cas de crise |

::: debutant
Cette note guide les choix techniques. Intégrité 4 sur les opérations → opérations **immuables** (RG-08), verrouillage optimiste, journal d'audit. Confidentialité 4 sur les identifiants → MiniBank **ne stocke aucun mot de passe** : c'est Keycloak qui s'en charge, avec un hachage robuste. Disponibilité 1 sur les indicateurs → un batch de nuit suffit, pas besoin de temps réel.
:::

# L'analyse de risques, version simplifiée d'EBIOS RM

EBIOS Risk Manager est la méthode de l'ANSSI, utilisée par les banques et les administrations françaises. Elle se déroule en 5 ateliers ; voici leur version MiniBank.

| Atelier | Question | Réponse MiniBank |
|---|---|---|
| 1. Cadrage et socle | Que protège-t-on, avec quelles règles de base ? | Les biens du tableau DICT ; socle : guide d'hygiène de l'ANSSI, OWASP |
| 2. Sources de risque | Qui pourrait attaquer, et pourquoi ? | Fraudeur (argent), cybercriminel (données à revendre), collaborateur malveillant ou négligent |
| 3. Scénarios stratégiques | Par quel chemin l'attaquant atteint-il son but ? | Prendre le compte d'un client ; usurper un conseiller ; passer par un fournisseur |
| 4. Scénarios opérationnels | Concrètement, techniquement ? | Les scénarios R1 à R6 ci-dessous |
| 5. Traitement du risque | Que fait-on, et que reste-t-il ? | Les mesures, rangées par état stable |

| N° | Scénario | Bien touché | Mesures | Quand |
|---|---|---|---|---|
| R1 | Un fraudeur teste des mots de passe volés ailleurs (*credential stuffing*) et prend le compte de Claire | Identifiants, soldes | Keycloak : limitation des tentatives, second facteur pour les virements, alerte sur connexions inhabituelles | E3 |
| R2 | Un client connecté appelle `/comptes/{id}` avec l'identifiant d'un autre client (OWASP A01) | Soldes, identités | Contrôle de propriété **à chaque requête**, tests automatiques 403/404 | E3 |
| R3 | Un mot de passe de base de données est poussé sur GitHub | Toutes les données | `.env` hors Git, analyse des secrets dans la CI (gitleaks), rotation | E1 |
| R4 | Une bibliothèque vulnérable est embarquée (OWASP A03) | Tout le SI | Dependabot, analyse des images (Trivy), versions fixées | E1-E5 |
| R5 | Un conseiller qui quitte la BNN garde ses accès | Identités, soldes | Processus de départ (Bonita), désactivation le jour J, revue trimestrielle des droits | E3 |
| R6 | Kafka ou la base des comptes tombe pendant la journée | Disponibilité | Modes dégradés, sauvegardes, PRA testé | E5-E8 |

# RGPD : le registre des traitements

Le RGPD impose de tenir un registre de chaque traitement de données personnelles. Voici celui de la BNN.

| Traitement | Finalité | Base légale | Données | Conservation (indicative) |
|---|---|---|---|---|
| T1 Tenue des comptes et opérations | Exécuter le contrat bancaire | Contrat | Identité, coordonnées, numéros de compte, opérations | Durée de la relation, puis 5 ans (obligations anti-blanchiment) |
| T2 Entrée en relation | Ouvrir un compte, vérifier l'identité | Obligation légale (connaissance client) | Identité, pièces justificatives | 5 ans après la fin de la relation |
| T3 Lutte contre la fraude | Bloquer les opérations frauduleuses | Intérêt légitime et obligations légales | Opérations, scores, décisions | À définir avec le DPO |
| T4 Notifications | Informer le client | Contrat | E-mail, événements | Durée de la relation |
| T5 Journaux techniques | Sécurité, diagnostic | Intérêt légitime | Adresse IP, identifiant, actions | 6 mois à 1 an (recommandation CNIL) |
| T6 Comptes des collaborateurs | Gérer les accès | Contrat de travail | Identité, fonction, droits | Durée du contrat |

::: attention
Les durées sont **indicatives** : en entreprise, elles sont fixées par le DPO et le service juridique. Retiens surtout le principe : chaque donnée a une **finalité**, une **base légale** et une **durée**. Le traitement T3 fait du profilage : il justifierait une **analyse d'impact (AIPD)**.
:::

Les principes que MiniBank applique dans le code :

- **Minimisation** (ENF-08) : seuls nom, prénom et e-mail sont collectés en V1 ; pas de date de naissance « au cas où ».
- **Pseudonymisation des journaux** (ENF-07) : numéros de compte masqués (`MB******4567`), jamais de jeton ni de mot de passe dans un log.
- **Données de test synthétiques**, générées (Faker), jamais copiées d'une vraie base.

::: scenario
**Claire écrit au DPO** : « Supprimez toutes mes données. » Que répond la BNN ? Ses données de prospection et de notification sont effacées. En revanche, ses opérations et son dossier d'identité doivent être **conservés 5 ans** au titre des obligations anti-blanchiment : la BNN le lui explique, restreint l'accès à ces données (archivage) et les supprime à l'échéance. Le droit à l'effacement n'est pas absolu quand une obligation légale impose de conserver.
:::

# DORA : la résilience numérique des banques

Le règlement européen **DORA** (*Digital Operational Resilience Act*) s'applique aux entités financières depuis le **17 janvier 2025**. Il est au cœur des projets SI bancaires actuels : le citer correctement en entretien est un vrai plus.

| Pilier DORA | Ce qu'il exige | Ce que MiniBank montre |
|---|---|---|
| Gestion du risque informatique | Un cadre de gestion des risques TIC | Ce document : DICT, scénarios, mesures |
| Incidents informatiques | Classer, gérer et déclarer les incidents majeurs | Processus incident ITIL et GLPI (dossier 06) |
| Tests de résilience | Tester régulièrement la résistance | Exercice de restauration (PRA) et tests de charge en E8 |
| Risque lié aux prestataires | Maîtriser les fournisseurs informatiques critiques | Liste des UC (hébergeur, e-mail), plan de sortie |
| Partage d'informations | Échanger sur les menaces | Hors périmètre (cité pour la complétude) |

# Gouvernance des données

Ton cours d'urbanisation donne déjà la règle de base : **un bloc est propriétaire de ses données**. La gouvernance des données la complète.

| Rôle | Responsabilité | Dans MiniBank |
|---|---|---|
| **Data owner** | Décide qui accède à la donnée et ce qu'elle doit contenir | Le bloc propriétaire : Comptes possède *Compte* et *Opération* |
| **Data steward** | Veille à la qualité au quotidien | Règles de validation (IBAN valide, e-mail unique) et contrôles dans l'ETL |
| **Data engineer** | Construit les flux et l'entrepôt | ETL Python, schéma en étoile (E7) |
| **Data analyst** | Produit les indicateurs | Tableaux Metabase (E7) |
| **DPO** | Conformité RGPD | Registre ci-dessus |

Quatre outils simples à tenir :

1. **Dictionnaire de données** : chaque champ, son sens, son format, son propriétaire (le modèle de données du kit, dossier 05, en est la base).
2. **Classification** : Public, Interne, Confidentiel, Secret. Les soldes sont *Confidentiels*, les identifiants *Secrets*.
3. **Règles de qualité** mesurées dans l'ETL : aucun virement sans compte source, somme des soldes = somme des opérations.
4. **Lignage** : d'où vient chaque indicateur (événement Kafka → ETL → table de faits → graphique).

# À faire et à éviter

::: faire
- Noter le besoin DICT **avant** de choisir une technologie de stockage ou d'échange.
- Relier chaque mesure de sécurité à un scénario de risque (pas de sécurité « au hasard »).
- Mentionner la finalité et la durée de conservation dans chaque user story qui touche des données personnelles.
:::

::: eviter
- De vraies données personnelles, même « juste pour tester ».
- Des journaux qui contiennent des jetons, des mots de passe ou des numéros de compte complets.
- Confondre DORA (règlement) et les métriques DORA (DevOps).
:::
