## Objet

<!-- Que fait cette pull request, en une ou deux phrases ? -->

**Exigences / stories :** EF-.. / US-..

## Changements

- …

## Comment tester

1. …
2. Requête(s) de `minibank.http` à exécuter : …

## Grille d'auto-revue

- [ ] Les cas nominaux ET les cas d'erreur sont testés
- [ ] Montants en `BigDecimal`, comparés avec `compareTo`
- [ ] Écritures dans une méthode `@Transactional` publique du service
- [ ] Aucune entité JPA exposée par l'API (DTO uniquement)
- [ ] Aucune donnée sensible dans les logs ni dans les réponses d'erreur
- [ ] Documentation à jour (OpenAPI, diagrammes, README)
- [ ] La CI est verte
