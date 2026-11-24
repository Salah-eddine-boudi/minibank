# ADR-001 — Java 25 et Spring Boot 4 pour les services

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 11/10/2026 | Salah Eddine Boudi |

## Contexte

Il faut choisir le langage et le framework des services métier. Le projet vise un poste de développeur dans une équipe Services financiers, où Java et Spring dominent. Le poste de développement dispose de Java 25.

## Décision

Les services métier (Comptes, Virements, Notifications, Audit) sont écrits en **Java 25 (LTS)** avec **Spring Boot 4.1**, construits avec le **Maven Wrapper**.

## Conséquences

**Positives**

- Écosystème très riche (Spring Data, Spring Security, Spring Kafka, Actuator).
- Version LTS : maintenue plusieurs années, acceptée par les clients bancaires.
- Compétence directement valorisable en entretien.

**Négatives ou points de vigilance**

- Spring Boot 4 est récent : certains tutoriels en ligne décrivent encore Spring Boot 3 (packages de test, Jackson). Toujours vérifier la documentation officielle de la version utilisée.
- Démarrage plus lourd qu'un framework minimaliste.

## Alternatives écartées

- Kotlin + Spring Boot : moins répandu chez les clients visés.
- Quarkus : excellent, mais moins demandé dans les offres ciblées.
- Node.js : déjà pratiqué sur un autre projet ; moins pertinent pour le poste visé.
