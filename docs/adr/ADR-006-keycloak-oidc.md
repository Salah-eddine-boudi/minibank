# ADR-006 — Authentification déléguée à Keycloak (OIDC)

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 11/10/2026 | Salah Eddine Boudi |

## Contexte

Gérer soi-même les mots de passe (stockage, réinitialisation, second facteur, verrouillage après échecs) est complexe et risqué. Les banques utilisent des fournisseurs d'identité centralisés.

## Décision

L'authentification est déléguée à **Keycloak** via **OpenID Connect** (code d'autorisation + PKCE pour l'application web). Les services sont des *resource servers* qui valident les **JWT** (signature, expiration, émetteur) et lisent les rôles `CLIENT`, `CONSEILLER`, `ADMIN`.

## Conséquences

**Positives**

- MiniBank ne stocke aucun mot de passe.
- Second facteur, politique de mots de passe et verrouillage fournis par Keycloak.
- Standard ouvert, compétence transférable (Azure AD, Okta fonctionnent pareil).

**Négatives ou points de vigilance**

- Un composant de plus à configurer et à faire tourner.
- Il faut comprendre OAuth 2.0 et OIDC pour déboguer.

## Alternatives écartées

- Authentification maison avec Spring Security et une table d'utilisateurs : formateur, mais éloigné des pratiques bancaires.
- Service cloud (Auth0, Entra ID) : dépendance externe pour un projet local.
