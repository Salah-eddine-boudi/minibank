---
title: "Conception objet appliquée à MiniBank"
subtitle: "Encapsulation, héritage, polymorphisme, interfaces, UML et design patterns du cours Concept objet, transformés en code bancaire"
author: "Projet MiniBank — dossier 03 Besoins et conception"
date: "Novembre 2026"
---

::: casquette
**Architecte applicatif** pour le modèle et les patterns, **développeur** pour le code. Ce document fait le pont entre ton cours *Concept objet* (Java, UML, design patterns) et l'état E1 « Cœur bancaire ».
:::

# Les trois concepts fondamentaux dans un compte bancaire

## Encapsulation : personne ne touche au solde directement

Ton cours : *les données sont privées, les méthodes publiques sont la seule façon d'agir sur elles ; on ne crée un accesseur que si c'est nécessaire.* Dans une banque, c'est vital : si n'importe quel code peut écrire `compte.setSolde(…)`, aucune règle de gestion n'est garantie.

```java
// Compte.java — classe abstraite : on n'ouvre jamais un « compte » tout court
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)    // une seule table pour tous les types de comptes
@DiscriminatorColumn(name = "type_compte")               // colonne qui dit « COURANT » ou « LIVRET »
public abstract class Compte {

    @Id @GeneratedValue
    private Long id;                                      // privé : identifiant technique

    @Column(nullable = false, unique = true, length = 12)
    private String numero;                                // RG-01 : « MB » + 10 chiffres

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal solde = BigDecimal.ZERO;           // privé, et PAS de setSolde()

    @Enumerated(EnumType.STRING)
    private StatutCompte statut = StatutCompte.ACTIF;     // ACTIF, BLOQUE, CLOTURE

    @Version
    private long version;                                 // verrouillage optimiste (ADR-004)

    protected Compte() { }                                // exigé par JPA, invisible du reste du code

    protected Compte(String numero) {
        this.numero = numero;
    }

    /** Seule porte d'entrée pour ajouter de l'argent (RG-02, RG-06). */
    public void crediter(Montant montant) {
        if (statut == StatutCompte.CLOTURE) {
            throw new OperationRefuseeException("Compte clôturé : aucune opération possible (RG-06)");
        }
        solde = solde.add(montant.valeur());              // Montant garantit déjà > 0 et 2 décimales
    }

    /** Seule porte d'entrée pour retirer de l'argent (RG-03, RG-06). */
    public void debiter(Montant montant) {
        if (statut != StatutCompte.ACTIF) {
            throw new OperationRefuseeException("Compte " + statut + " : débit interdit (RG-06)");
        }
        if (!peutDebiter(montant)) {                      // la règle dépend du type de compte
            throw new SoldeInsuffisantException(numero, montant);
        }
        solde = solde.subtract(montant.valeur());
    }

    /** Polymorphisme : chaque type de compte décide à sa façon. */
    protected abstract boolean peutDebiter(Montant montant);

    public BigDecimal solde() { return solde; }           // lecture seule : un « getter » utile
    public String numero() { return numero; }
    protected BigDecimal soldeActuel() { return solde; }  // visible des sous-classes seulement
}
```

::: cle
- `private` sur `solde` + **aucun** `setSolde` : la seule façon de modifier le solde est `crediter` ou `debiter`, qui appliquent les règles.
- `protected` sur le constructeur et sur `soldeActuel()` : visibles des **sous-classes** (héritage), pas du reste du code. C'est le tableau *Private / Protected / Public* de ton cours.
- `Montant` est un petit objet (*value object*) qui garantit RG-02 dès sa création : impossible de fabriquer un montant négatif ou à trois décimales.
:::

## Héritage : un compte courant *est un* compte

Ton cours : *l'héritage se justifie seulement si la sous-classe a au moins un attribut, une méthode ou une association propre.* Ici, c'est le cas :

```java
@Entity
@DiscriminatorValue("COURANT")
public class CompteCourant extends Compte {

    @Column(precision = 19, scale = 2)
    private BigDecimal decouvertAutorise = BigDecimal.ZERO;   // propre au compte courant ; 0 en V1 (RG-03)

    protected CompteCourant() { }
    public CompteCourant(String numero) { super(numero); }    // appelle le constructeur de Compte

    @Override                                                  // le compilateur vérifie la redéfinition
    protected boolean peutDebiter(Montant montant) {
        // solde - montant >= -découvert autorisé
        return soldeActuel().subtract(montant.valeur()).compareTo(decouvertAutorise.negate()) >= 0;
    }
}

@Entity
@DiscriminatorValue("LIVRET")
public class LivretEpargne extends Compte {

    @Column(precision = 5, scale = 4)
    private BigDecimal tauxAnnuel = new BigDecimal("0.0200");  // propre au livret (RG-14)

    protected LivretEpargne() { }
    public LivretEpargne(String numero) { super(numero); }

    @Override
    protected boolean peutDebiter(Montant montant) {
        return soldeActuel().compareTo(montant.valeur()) >= 0; // jamais de découvert sur un livret
    }

    /** RG-14 : intérêts mensuels = solde × taux annuel ÷ 12, arrondi bancaire (RG-13). */
    public Montant interetsDuMois() {
        return Montant.de(soldeActuel().multiply(tauxAnnuel)
                .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_EVEN));
    }
}
```

## Polymorphisme : le même appel, des comportements différents

```java
List<Compte> comptesDeClaire = List.of(courant, livret);   // le type de la variable est Compte
for (Compte c : comptesDeClaire) {
    c.debiter(Montant.de("50.00"));   // Java appelle CompteCourant.peutDebiter OU LivretEpargne.peutDebiter
}
```

::: debutant
Ton cours le dit ainsi : *c'est le type de l'objet, et non le type de la variable, qui détermine la méthode exécutée.* La variable `c` est de type `Compte`, mais l'objet derrière est un `CompteCourant` ou un `LivretEpargne`, et c'est **sa** version de `peutDebiter` qui s'exécute. Le jour où tu ajoutes un `CompteJoint`, la boucle ne change pas : c'est le principe ouvert/fermé (SOLID).
:::

## Interface : un contrat pour évaluer le risque

Une interface est *un contrat que les classes qui l'implémentent doivent respecter* (cours). Le Service Virements ne doit pas savoir **comment** on calcule un score : il dépend d'un contrat.

```java
/** Contrat : n'importe quel évaluateur de risque doit savoir noter un virement. */
public interface EvaluateurRisque {
    ScoreRisque evaluer(DemandeVirement demande);
}

/** Implémentation réelle (état E6) : appelle le Service Fraude en Python par HTTP (flux F03). */
@Component
@Primary
class EvaluateurRisqueHttp implements EvaluateurRisque { /* RestClient + délai 300 ms */ }

/** Implémentation locale (états E1 à E5) : les règles RG-12 codées en Java. */
@Component
class EvaluateurRisqueRegles implements EvaluateurRisque { /* +40, +30, +20, +20 */ }
```

Le jour où le Service Fraude Python arrive, **on change d'implémentation sans toucher au Service Virements**. C'est le *pattern Stratégie* et le *principe d'inversion des dépendances*.

# L'UML à chaque étape du projet

Ton cours place chaque diagramme UML à une étape. Le kit d'octobre les contient déjà (dossier `schemas/uml`) :

| Étape (cours) | Diagramme | Fichier du kit | Ce qu'il apporte à MiniBank |
|---|---|---|---|
| Analyse des besoins | Cas d'utilisation | `02_cas_utilisation` | Qui fait quoi : Claire, Karim, Julie |
| Analyse du domaine | Classes (domaine) | `05_classes_domaine` | Client, Compte, Opération, Virement et leurs cardinalités |
| Analyse du domaine | Activités | `04_activite_virement` | L'enchaînement d'un virement (complété par le BPMN du dossier 04) |
| Analyse de l'application | Séquence système | `03_sequence_systeme_virement` | L'utilisateur face au système, vu de l'extérieur |
| Analyse de l'application | États-transitions | `06_etats_virement`, `07_etats_compte` | Les statuts possibles et les transitions autorisées |
| Conception | Classes (conception) | `08_classes_conception_service_comptes` | Contrôleurs, services, dépôts, entités |
| Conception | Composants, déploiement | `15_composants`, `16_deploiement` | Les briques et où elles tournent |

## Associations et cardinalités, avec l'exemple exact du cours

Ton cours illustre les cardinalités avec… un compte bancaire : *un compte est détenu par 0 ou 1 personne ; une personne détient 0 ou plusieurs comptes.* Dans MiniBank :

| Association | Cardinalités | Type | Pourquoi |
|---|---|---|---|
| Client — Compte | 1 client détient 0..3 comptes (RG-15) ; 1 compte a 1..2 titulaires (compte joint) | Association simple | Les deux existent indépendamment |
| Compte ◆— Opération | 1 compte compose 0..* opérations | **Composition** | Une opération n'a pas de sens sans son compte |
| Client ◇— Bénéficiaire | 1 client a 0..* bénéficiaires | **Agrégation** | Supprimer un bénéficiaire ne supprime pas le compte du destinataire |
| Client — Compte, via **Procuration** | Classe d'association | Classe d'association | Si le lien disparaît, la procuration n'existe plus (moyen mnémotechnique du cours) |

::: attention
En banque, on ne **supprime** jamais une opération (RG-08) : la composition dit que l'opération *disparaîtrait* avec le compte, mais en pratique un compte clôturé est **conservé** avec son historique. Le diagramme exprime la dépendance de vie ; la règle de gestion impose l'archivage.
:::

# Les design patterns du cours, retrouvés dans MiniBank

| Pattern (cours) | Où dans MiniBank | Pourquoi | Piège à connaître |
|---|---|---|---|
| **Singleton** | Les beans Spring (`@Service`, `@Component`) sont uniques par défaut | Une seule instance partagée, gérée par le conteneur | Ce n'est **pas** le Singleton classique à constructeur privé : c'est Spring qui garantit l'unicité. Un bean sans état est donc obligatoire |
| **Observateur** | Événements de domaine (`VirementExecute`) puis Kafka (E5) | Notifications, ETL, audit s'abonnent sans que Virements les connaisse | Kafka est un observateur **distribué** : l'abonné peut être arrêté, le message attend |
| **Stratégie** | `EvaluateurRisque` et ses implémentations | Changer d'algorithme sans toucher l'appelant | — |
| **Commande** | `DemandeVirement` + clé d'idempotence (RG-10) | La demande est un objet qu'on peut stocker, rejouer sans double débit | — |
| **Itérateur** | `Page<Operation>` de Spring Data, les `Stream` Java | Parcourir l'historique sans connaître la structure interne | Ne jamais charger tout l'historique d'un coup |
| **Fabrique** | `CompteFactory.ouvrir(TypeCompte, Client)` | Centraliser la création et la règle RG-15 (au plus 1 courant, 2 livrets) | — |
| **Adaptateur** | Les clients HTTP vers Fraude, Keycloak, GLPI | Traduire une API externe dans le vocabulaire de MiniBank | — |

# SOLID en une ligne chacun, avec MiniBank

| Principe | Exemple |
|---|---|
| **S** — une seule responsabilité | `VirementService` orchestre ; `Compte` applique ses règles ; le contrôleur ne fait que traduire HTTP |
| **O** — ouvert à l'extension, fermé à la modification | Ajouter `CompteJoint` sans modifier `VirementService` |
| **L** — substitution de Liskov | Partout où on attend un `Compte`, un `LivretEpargne` doit fonctionner sans surprise |
| **I** — interfaces ciblées | `EvaluateurRisque` n'expose qu'une méthode |
| **D** — dépendre des abstractions | `VirementService` dépend d'`EvaluateurRisque`, pas d'`EvaluateurRisqueHttp` |

# Les mêmes idées en Python et en TypeScript

Les concepts objet ne sont pas propres à Java. Tu les retrouveras dans les autres services :

```python
# Service Fraude (Python) — une interface « à la Python » avec Protocol
from typing import Protocol
from decimal import Decimal

class Regle(Protocol):                     # contrat : toute règle sait calculer des points
    def points(self, virement: "Virement") -> int: ...

class MontantEleve:                        # implémente le contrat sans « implements » explicite
    def points(self, virement) -> int:
        return 40 if virement.montant > Decimal("3000") else 0   # RG-12
```

```typescript
// Espace client (TypeScript) — l'encapsulation existe aussi côté front
export class Montant {
  private constructor(private readonly centimes: number) {}      // constructeur privé
  static depuisSaisie(texte: string): Montant {                  // fabrique qui valide RG-02
    const valeur = Math.round(Number(texte.replace(',', '.')) * 100);
    if (!Number.isFinite(valeur) || valeur <= 0) throw new Error('Montant invalide');
    return new Montant(valeur);
  }
}
```

# À faire et à éviter

::: faire
- Mettre les règles de gestion **dans les objets métier** (modèle riche), pas dans des services qui manipulent des données nues.
- Tester chaque règle (RG-xx) par un test unitaire sur l'objet, sans base de données.
- Utiliser `BigDecimal` et `RoundingMode.HALF_EVEN` pour tout montant (ADR-003).
:::

::: eviter
- Générer automatiquement tous les getters et setters : c'est l'inverse de l'encapsulation.
- Un héritage sans attribut ni méthode propre (exemple « Product » de ton cours).
- `double` ou `float` pour de l'argent : `0.1 + 0.2` ne vaut pas `0.3`.
:::
