# Studio photo

Application web de gestion d'un studio de photographie (projet Java web IAI2, ENSSAT) :
photographes, clients, séances, facturation et statistiques.

- **Back-end** : Java 25, Jakarta REST (Jersey) sur serveur Grizzly embarqué, JSON-B, JDBC + MySQL.
- **Front-end** : HTML / CSS / JavaScript sans framework (`src/main/resources/static`).

## Démarrage rapide

```bash
mvn compile exec:java      # puis http://localhost:8080/
mvn test                   # tests unitaires
```

Création de la base, données de test et autres commandes : voir [readmecommande.md](readmecommande.md).

## Documentation

| Quoi | Où |
| --- | --- |
| Contrat de l'API (OpenAPI) | [`src/main/resources/static/openapi.yaml`](src/main/resources/static/openapi.yaml) |
| Choix techniques (phase 0) | [`docs/choix-techniques.md`](docs/choix-techniques.md) |
| Lot 1 – photographes : règles et choix | [`docs/lot1-photographes.md`](docs/lot1-photographes.md) |
| Schéma SQL et données de test | [`sql/`](sql/) |

## Organisation du code

```
fr.enssat.studiophoto
├── resource    ressources REST (@Path) : lisent la requête, appellent le service
├── service     règles métier
├── dao         accès MySQL en JDBC (interface + implémentation)
├── model       objets métier (sérialisés en JSON)
├── dto         objets d'échange qui ne sont pas des objets métier
└── exception   exceptions métier et conversion en réponses HTTP
```

Une branche par lot (`lot-1-photographes`, …), fusionnée dans `main` par merge request.
