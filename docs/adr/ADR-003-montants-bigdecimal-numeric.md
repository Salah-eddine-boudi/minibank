# ADR-003 — Montants en BigDecimal et NUMERIC(19,2)

| Statut | Date | Décideur |
|---|---|---|
| Acceptée | 11/10/2026 | Salah Eddine Boudi |

## Contexte

Les types à virgule flottante (`double`, `float`) ne représentent pas exactement les nombres décimaux : `0.1 + 0.2` vaut `0.30000000000000004`. Sur des milliers d'opérations, ces écarts deviennent des centimes faux, inacceptables pour une banque.

## Décision

Tous les montants sont des **`BigDecimal`** en Java, créés à partir de chaînes ou de `BigDecimal.valueOf`, comparés avec `compareTo`, arrondis explicitement en **HALF_EVEN** quand un calcul produit plus de 2 décimales. En base : **`NUMERIC(19,2)`**. En JSON : nombres décimaux.

## Conséquences

**Positives**

- Calculs exacts au centime.
- Arrondi bancaire explicite et testable.

**Négatives ou points de vigilance**

- Code plus verbeux (`a.add(b)` au lieu de `a + b`).
- Piège : `new BigDecimal("2.0").equals(new BigDecimal("2.00"))` est faux ; toujours utiliser `compareTo`.

## Alternatives écartées

- Montants en centimes dans un `long` : exact et rapide, mais moins lisible et source d'erreurs de conversion à l'affichage.
- `double` : rejeté (inexact).
