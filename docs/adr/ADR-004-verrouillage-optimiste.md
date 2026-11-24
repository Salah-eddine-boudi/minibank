# ADR-004 — Verrouillage optimiste pour la concurrence

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 11/10/2026 | Salah Eddine Boudi |

## Contexte

Deux opérations peuvent modifier le même compte au même instant (deux virements depuis un compte joint). Sans protection, la seconde écrase la première (« mise à jour perdue ») et le solde devient faux.

## Décision

Les entités modifiables portent un champ **`@Version`**. Hibernate ajoute `WHERE version = ?` à chaque mise à jour ; si la version a changé, l'opération échoue (`ObjectOptimisticLockingFailureException`), la transaction est annulée et l'API renvoie **409 Conflict**.

## Conséquences

**Positives**

- Aucun verrou tenu pendant la transaction : bonnes performances quand les conflits sont rares.
- Comportement simple à tester (test de concurrence).

**Négatives ou points de vigilance**

- En cas de conflit, le client doit réessayer (ou le service retente automatiquement une fois).
- Si les conflits devenaient fréquents (compte très sollicité), il faudrait passer au verrouillage pessimiste (`SELECT … FOR UPDATE`).

## Alternatives écartées

- Verrouillage pessimiste : plus sûr sous forte contention, mais bloque les autres transactions et risque les interblocages.
- Niveau d'isolation SERIALIZABLE : très coûteux.
