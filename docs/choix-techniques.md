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
| Tests | JUnit 5, faux DAO écrits à la main (pas de bibliothèque de mock) | `junit-jupiter` (scope test) |
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
  passent des DAO en mémoire et une horloge fixe (`java.time.Clock`).
- *Contrepartie* : le code JDBC est plus long qu'avec JPA (ouverture des connexions,
  lecture des `ResultSet`), et les transactions sur plusieurs tables sont gérées à la main
  (`setAutoCommit(false)`, `commit`, `rollback`).

Le packaging en jar exécutable et la configuration de la connexion MySQL restent à faire
par le lot 5 (tâche 5.8).

## P0.2 – Organisation du projet

Packages `resource / service / dao / model / dto / exception` (voir `README.md`). Le
groupe Maven a été renommé `fr.enssat:studiophoto` (il restait `org.tutorial:HelloJersey`
de l'exemple du cours). La protection de la branche `main` se règle sur GitLab
(Settings → Repository → Protected branches).

## P0.6 – Contrat d'API

Écrit dans `src/main/resources/static/openapi.yaml` : il est donc servi à
`http://localhost:8080/openapi.yaml`, prêt pour la page Swagger UI du lot 2 (tâche 2.8).
Conventions principales :

- un objet lié est envoyé par son identifiant (`"photographe": {"id": 2}`) et renvoyé complet ;
- 400 pour des données invalides (y compris un identifiant inconnu *dans le corps*),
  404 pour un identifiant inconnu *dans le chemin*, 409 pour une action impossible dans
  l'état actuel (doublon, transition interdite, suppression d'un élément référencé,
  photographe inactif ou occupé) ;
- les routes « à confirmer » (désactivation d'un photographe) répondent 409 avec un champ
  supplémentaire et acceptent `?confirmer=true`.

## P0.7 – Format d'erreur commun

Toute erreur renvoie :

```json
{ "status": 409, "erreur": "Conflict", "message": "Cet email est déjà utilisé." }
```

| Exception métier (`exception`) | Code HTTP |
| --- | --- |
| `DonneesInvalidesException` | 400 |
| `RessourceIntrouvableException` | 404 |
| `ConflitException` (et `ConfirmationRequiseException`) | 409 |

Les services lèvent ces exceptions ; `ExceptionMetierMapper` (un `ExceptionMapper` JAX-RS,
enregistré par `Main`) les convertit. Le message est en français et le front l'affiche
tel quel (`api.js`). Les erreurs non métier (JSON mal formé, panne de base) seront mises
au même format par le lot 5 (tâche 5.7).

## Dépendances entre lots fournies en phase 0

- `SeanceDAO` / `SeanceDAOImpl` : contient déjà les méthodes utilisées par le lot 1
  (compter les séances d'un photographe, compter et annuler ses séances à venir) ; le lot 3
  y ajoute les siennes.
