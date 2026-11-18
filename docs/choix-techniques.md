# Phase 0 – Choix techniques

Notes pour le rapport (section « Architecture générale et justification de la stack »).

## P0.1 – Stack retenue

| Besoin | Choix | Fourni par |
| --- | --- | --- |
| API REST | Jakarta REST (JAX-RS) avec Jersey 4 | `jersey-container-grizzly2-http` |
| Serveur HTTP | Grizzly embarqué, lancé par `Main` | idem |
| JSON | JSON-B (Yasson) | `jersey-media-json-binding` |
| Accès aux données | JDBC + `PreparedStatement`, une classe DAO par table | JDK + pilote `mysql-connector-j` |
| Injection de dépendances | Constructeurs (pas de framework) ; HK2 de Jersey pour les ressources | `jersey-hk2` |
| PDF des factures | OpenPDF | `com.github.librepdf:openpdf` |
| Tests | JUnit 5 ; DAO simulés par Mockito (lots 2, 4) ou par des faux DAO écrits à la main (lot 1) | `junit-jupiter`, `mockito-junit-jupiter` (scope test) |
| Front | HTML / CSS / JS sans framework, `fetch` | – |

**Écart avec le plan initial.** Le plan prévoyait Payara Micro avec JPA et CDI. Le socle
déjà commité (premier commit de la phase 0) utilise Jersey + Grizzly + JDBC ; le groupe a
choisi de le garder :

- *Moins de magie, plus de maîtrise* : chaque requête SQL est écrite et lisible, ce qui
  facilite l'explication des règles métier (chevauchement de séances, statistiques) en
  soutenance ; pas de configuration `persistence.xml`, de mapping d'héritage ni de
  chargement paresseux à comprendre.
- *Démarrage léger* : un simple `main` démarre le serveur (`mvn compile exec:java`), sans
  serveur d'applications à installer.
- *Testabilité* : les services reçoivent leurs DAO par le constructeur ; les tests leur
  passent des DAO simulés et une horloge fixe (`java.time.Clock`).
- *Contrepartie* : le code JDBC est plus long qu'avec JPA (ouverture des connexions,
  lecture des `ResultSet`), et les transactions sur plusieurs tables sont gérées à la main
  (`setAutoCommit(false)`, `commit`, `rollback`).

Le packaging en jar exécutable et la configuration de la connexion MySQL restent à faire
par le lot 5 (tâche 5.8).

## P0.2 – Organisation du projet

Packages `resource / service / dao / model / dto / exception` (voir `dao/package-info.java`
et les autres `package-info.java`). La protection de la branche `main` se règle sur
GitLab (Settings → Repository → Protected branches).

## P0.6 – Contrat d'API

Écrit dans `src/main/resources/static/openapi.yaml`, servi par la page Swagger UI
(`http://localhost:8080/swagger/`, lot 2). Chaque lot y documente ses routes quand il les
code ; conventions :

- un objet lié est envoyé par son identifiant (`"specialites": [{"id": 2}]`) et renvoyé complet ;
- 400 pour des données invalides, 404 pour un identifiant inconnu, 409 pour une action
  impossible dans l'état actuel (doublon, transition interdite, suppression d'un élément
  référencé...) ;
- une action « à confirmer » (désactivation d'un photographe) répond 409 avec un champ
  supplémentaire et accepte `?confirmer=true`.

## P0.7 – Format d'erreur commun

Toute erreur renvoie :

```json
{ "status": 409, "erreur": "Conflict", "message": "Cet email est déjà utilisé." }
```

| Exception (`exception`) | Code HTTP | Converti par |
| --- | --- | --- |
| `DonneesInvalidesException` | 400 | `MetierExceptionMapper` |
| `RessourceIntrouvableException` | 404 | `MetierExceptionMapper` |
| `ConflitException` | 409 | `MetierExceptionMapper` |
| `ConfirmationRequiseException` (lot 1) | 409 + `nombreSeances` | `ConfirmationRequiseExceptionMapper` |
| `DaoException` | 500 | `DaoExceptionMapper` |
| erreurs JAX-RS (JSON illisible, route inconnue...) | 400, 404, 405, 415 | `WebApplicationExceptionMapper`, `ProcessingExceptionMapper` |

Les services lèvent les exceptions métier (sous-classes de `MetierException`) ; les mappers,
enregistrés par `Main`, les convertissent. Le message est en français et le front l'affiche
tel quel (`api.js`). Quand deux mappers conviennent, JAX-RS prend celui dont le type
d'exception est le plus proche : `ConfirmationRequiseExceptionMapper` passe donc avant
`MetierExceptionMapper` pour ajouter `nombreSeances`.

## Dépendances entre lots

- `SeanceDAO` (lot 3) contient aussi les méthodes utilisées par le lot 1 : compter les
  séances d'un photographe, compter et annuler ses séances à venir.
