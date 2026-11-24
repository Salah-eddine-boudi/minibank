---
title: "Guide sécurité — MiniBank"
subtitle: "Principes, OWASP Top 10 2025, sécurité des API, JWT, secrets, RGPD et scénarios d'attaque"
author: "Kit projet MiniBank"
date: "Octobre 2026"
lang: fr-FR
---

# Pourquoi la sécurité est centrale dans une banque

Une faille dans une application bancaire, c'est de l'argent volé, des données personnelles exposées, des sanctions du régulateur et une confiance perdue. En entretien pour un poste en services financiers, montrer que tu **penses sécurité dès la conception** te distingue immédiatement.

# Les principes

| Principe | Signification | Application MiniBank |
|---|---|---|
| **Moindre privilège** | Chacun n'a que les droits strictement nécessaires | L'administrateur ne fait pas de virements ; un client ne voit que ses comptes |
| **Défense en profondeur** | Plusieurs barrières successives | Validation du DTO + règle dans l'entité + contrainte SQL |
| **Sécurité par défaut** | Tout est fermé sauf ce qui est explicitement ouvert | Toutes les routes exigent un jeton, sauf `/actuator/health` |
| **Ne jamais faire confiance aux entrées** | Tout ce qui vient de l'extérieur est suspect | Validation systématique, requêtes paramétrées |
| **Séparation des tâches** | Une action sensible demande deux rôles différents | Créer un utilisateur (admin) ≠ faire un virement (client) |
| **Traçabilité** | Toute action sensible laisse une trace | Journal d'audit immuable |
| **Échouer de façon sûre** | En cas de doute, refuser | Fraude indisponible → virement refusé, pas accepté |

# OWASP Top 10 (édition 2025)

L'OWASP publie la liste des 10 risques les plus critiques des applications web. Connaître cette liste par cœur est un minimum attendu.

| N° | Risque | Ce que c'est | Parade dans MiniBank | Phase |
|---|---|---|---|---|
| A01 | Contrôle d'accès défaillant | Accéder à ce qu'on ne devrait pas (le compte d'un autre) | Vérifier la propriété à **chaque** requête ; tests 403/404 | P3 |
| A02 | Mauvaise configuration de sécurité | Réglages par défaut dangereux, messages d'erreur trop bavards | Actuator limité à `health`, pas de trace dans les réponses, CORS restreint | P1, P3 |
| A03 | Défaillances de la chaîne d'approvisionnement logicielle | Bibliothèque vulnérable ou malveillante | Versions stables, analyse des dépendances en CI (Dependabot, OWASP Dependency-Check) | P7 |
| A04 | Défaillances cryptographiques | Données sensibles en clair, algorithmes faibles | HTTPS, jetons signés, aucun mot de passe stocké (Keycloak) | P3 |
| A05 | Injection | Du code (SQL…) glissé dans une entrée | Spring Data / requêtes paramétrées, jamais de concaténation SQL | P1 |
| A06 | Conception non sécurisée | Faille dans la logique même | Plafonds, idempotence, contrôle de fraude, modèle de menaces | P1, P6 |
| A07 | Défaillances d'authentification | Mots de passe faibles, sessions mal gérées | Keycloak : politique de mots de passe, second facteur, jetons de 5 min | P3 |
| A08 | Défaillances d'intégrité des logiciels ou des données | Données ou code modifiés sans contrôle | Opérations immuables, signature des jetons, CI contrôlée | P1, P7 |
| A09 | Défaillances de journalisation et d'alerte | Attaque invisible faute de traces | Journal d'audit, logs avec traceId, alerte sur refus répétés | P5 |
| A10 | Mauvaise gestion des situations exceptionnelles | Erreurs mal traitées qui ouvrent une brèche | Gestionnaire d'erreurs central, « échouer de façon sûre » | P1 |

# Sécurité des API : OWASP API Security Top 10 (2023)

Les API ont leur propre liste. Les trois premiers risques sont ceux qui touchent le plus les API bancaires.

| N° | Risque | Exemple d'attaque sur MiniBank | Parade |
|---|---|---|---|
| API1 | **Autorisation au niveau de l'objet défaillante** (BOLA) | Claire remplace `MB0000000001` par `MB0000000003` dans l'URL et voit le compte de Thomas | Vérifier que le compte appartient à l'utilisateur du jeton |
| API2 | Authentification défaillante | Jeton non vérifié, jeton expiré accepté | Validation signature + expiration + émetteur |
| API3 | Autorisation au niveau des propriétés | Le client envoie `"solde": 1000000` ou `"statut": "ACTIF"` | DTO sans ces champs ; jamais d'entité en entrée |
| API4 | Consommation de ressources non limitée | Demander 1 million d'opérations en une page | `size` ≤ 100, limitation de débit (429) |
| API5 | Autorisation au niveau des fonctions | Un client appelle `POST /blocage` | `@PreAuthorize("hasRole('CONSEILLER')")` |
| API6 | Accès non restreint à des flux métier sensibles | Robot qui teste des numéros de compte | Limitation de débit, détection de fraude |
| API7 | Falsification de requête côté serveur (SSRF) | Faire appeler une URL interne par le serveur | Aucun appel vers une URL fournie par l'utilisateur |
| API8 | Mauvaise configuration | Swagger UI ouvert en production | Désactiver la documentation interactive hors développement |
| API9 | Mauvaise gestion de l'inventaire | Ancienne version d'API oubliée et vulnérable | Versionnement, dépréciation documentée |
| API10 | Consommation non sûre d'API tierces | Faire confiance aveuglément à la réponse de la Fraude | Valider aussi les réponses reçues, délais d'attente |

## Le contrôle de propriété (BOLA) en code

```java
// Dans le service, à partir de la phase 3 : l'identité vient du jeton, jamais du JSON.
@Transactional(readOnly = true)
public CompteResponse consulter(String numero, Jwt jeton) {
    Compte compte = trouver(numero);
    boolean estConseiller = aLeRole(jeton, "CONSEILLER");
    boolean estTitulaire = compte.getClient().getIdKeycloak().equals(jeton.getSubject());   // « sub » du JWT
    if (!estConseiller && !estTitulaire) {
        throw new CompteIntrouvableException(numero);   // 404 : on ne révèle pas que le compte existe
    }
    return CompteResponse.de(compte);
}
```

# Authentification et autorisation : JWT expliqué

**Authentification** = « qui es-tu ? » (401 si on ne sait pas). **Autorisation** = « as-tu le droit ? » (403 si non).

Un **JWT** est composé de trois parties encodées en Base64URL, séparées par des points : `en-tête.charge.signature`.

```jsonc
// Charge (payload) décodée d'un jeton Keycloak, simplifiée
{
  "iss": "http://localhost:8180/realms/minibank",    // émetteur : qui a créé le jeton
  "sub": "11111111-1111-1111-1111-111111111111",     // identifiant unique de l'utilisateur
  "exp": 1791712800,                                  // expiration (secondes depuis 1970)
  "preferred_username": "claire.martin",
  "realm_access": { "roles": ["CLIENT"] }
}
```

| À retenir | Pourquoi |
|---|---|
| La charge est **lisible par tout le monde** (simple encodage, pas de chiffrement) | Ne jamais y mettre de donnée secrète |
| La **signature** empêche toute modification | Changer `CLIENT` en `ADMIN` invalide la signature |
| Le service vérifie la signature avec la **clé publique** de Keycloak (JWKS) | Il n'a besoin d'aucun secret partagé |
| Jeton d'accès court (5 min) + jeton de rafraîchissement | Limite l'impact d'un jeton volé |

## Configuration Spring Security (phase 3, aperçu)

```yaml
# application.yml — le service devient un « resource server » OAuth2
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/minibank   # Spring récupère tout seul les clés publiques
```

```java
@Configuration
@EnableMethodSecurity                                         // active @PreAuthorize
public class SecuriteConfig {

    @Bean
    SecurityFilterChain securite(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health/**").permitAll()   // sonde de santé publique
                .anyRequest().authenticated())                        // tout le reste : jeton obligatoire
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> { }))     // validation des JWT
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable());                            // API sans cookie de session : CSRF sans objet
        return http.build();
    }
}
```

> **CSRF désactivé : pourquoi c'est correct ici.** L'attaque CSRF exploite un cookie envoyé automatiquement par le navigateur. Une API *stateless* authentifiée par un en-tête `Authorization` n'utilise pas de cookie de session : la protection CSRF n'apporte rien. Si un jour on utilise des cookies, il faut la réactiver. C'est une question d'entretien classique.

Les rôles Keycloak sont dans `realm_access.roles` : il faut un petit convertisseur pour les transformer en autorités Spring (`ROLE_CLIENT`…). On le détaillera en phase 3.

# Gestion des secrets

| À faire | À ne jamais faire |
|---|---|
| Lire les secrets dans des variables d'environnement : `password: ${DB_PASSWORD}` | Écrire un mot de passe de production dans `application.yml` |
| Fournir un `.env.example` sans valeurs réelles, ignorer `.env` dans Git | Commiter un fichier `.env` |
| Utiliser des secrets GitHub Actions pour la CI | Afficher un secret dans un log de CI |
| Changer immédiatement un secret commité par erreur | Croire qu'un `git rm` suffit : le secret reste dans l'historique |

Les mots de passe `minibank/minibank` du `docker-compose.yml` sont acceptables **uniquement** en local.

# Journalisation sécurisée

- Toujours journaliser : connexions réussies et échouées, refus d'accès (403/404 sur ressource d'autrui), opérations financières, changements de droits.
- Ne jamais journaliser : mots de passe, jetons, numéros complets (masquer : `MB******0001`), données de santé.
- Chaque ligne porte : horodatage UTC, niveau, service, `traceId`, acteur (identifiant, pas le nom).

# RGPD : les essentiels pour un développeur

| Principe | Application MiniBank |
|---|---|
| Minimisation | Seulement nom, prénom, e-mail ; pas de date de naissance ni d'adresse en V1 |
| Finalité | Les données servent à la gestion des comptes, rien d'autre |
| Durée de conservation | Définie et documentée ; les données d'un compte clôturé sont conservées selon les obligations légales, puis supprimées ou anonymisées |
| Droits des personnes | Accès (exporter ses données), rectification (modifier son e-mail) |
| Sécurité | Contrôle d'accès, chiffrement en transit, journalisation |
| Données de test | Toujours **fictives** (`@exemple.test`) ; jamais de copie de vraies données en développement |

# Modèle de menaces du virement (méthode STRIDE)

STRIDE classe les menaces en six familles. On l'applique au cas d'utilisation le plus sensible.

| Menace | Question | Exemple sur le virement | Parade |
|---|---|---|---|
| **S**poofing (usurpation) | Quelqu'un peut-il se faire passer pour un autre ? | Identifiants volés | Second facteur, détection de fraude |
| **T**ampering (altération) | Peut-on modifier les données en transit ou stockées ? | Modifier le montant dans la requête | HTTPS, validation serveur, montant relu côté serveur |
| **R**epudiation (répudiation) | Peut-on nier avoir fait l'action ? | « Ce n'est pas moi qui ai viré » | Journal d'audit horodaté et immuable |
| **I**nformation disclosure (fuite) | Des données peuvent-elles fuiter ? | Message d'erreur révélant le solde d'un autre | 404 générique, pas de détail interne |
| **D**enial of service (déni de service) | Peut-on rendre le service indisponible ? | Rafale de virements | Limitation de débit, pagination bornée |
| **E**levation of privilege (élévation de privilège) | Peut-on obtenir plus de droits ? | Un client appelle une route conseiller | `@PreAuthorize`, tests 403 |

# Scénarios d'attaque à tester toi-même

À partir de la phase 3, transforme chaque scénario en test automatisé.

| N° | Attaque | Comment la tester | Résultat attendu |
|---|---|---|---|
| 1 | Accès au compte d'autrui | Jeton de Claire, `GET /api/comptes/MB0000000003` | 404 + ligne d'audit |
| 2 | Affectation massive | Ajouter `"solde": 999999` au JSON d'ouverture | Champ ignoré, solde 0,00 |
| 3 | Injection SQL | Numéro `MB0000000001' OR '1'='1` | 400 (format invalide), aucune donnée |
| 4 | Jeton modifié | Changer un caractère de la charge du JWT | 401 |
| 5 | Jeton expiré | Attendre 5 minutes, rejouer | 401 |
| 6 | Élévation de privilège | Jeton CLIENT sur `POST /blocage` | 403 |
| 7 | Rejeu de virement | Rejouer la même requête et la même clé | Même réponse, un seul débit |
| 8 | Montant piégé | `0.001`, `-0`, `1e10`, `"abc"` | 400 |
| 9 | Pagination abusive | `size=1000000` | Taille plafonnée à 100 ou 400 |
| 10 | Erreur bavarde | Provoquer une erreur interne | 500 générique, aucune trace Java dans la réponse |

# Liste de contrôle par phase

- [ ] **P1** : validation des DTO, `BigDecimal`, contraintes SQL, gestionnaire d'erreurs sans fuite, aucun secret dans Git.
- [ ] **P2** : pas de donnée sensible dans le stockage du navigateur, échappement des affichages (React et Angular le font par défaut), CORS restreint.
- [ ] **P3** : Keycloak, routes fermées par défaut, contrôle de propriété, tests 401/403/404, second facteur pour conseiller et admin.
- [ ] **P4** : chaque service valide le jeton, délais d'attente sur tous les appels sortants.
- [ ] **P5** : journal d'audit complet et immuable, logs masqués avec traceId.
- [ ] **P7** : analyse des dépendances et SonarCloud dans la CI, secrets GitHub, HTTPS sur l'environnement de recette.
