# Commandes du projet Studio photo

Toutes les commandes se lancent depuis le dossier `jeephoto` (la racine du projet).

## 1. Prérequis

```bash
java -version      # Java 25 (version utilisée dans le pom.xml)
mvn -version       # Maven
mysql --version    # client MySQL (pour les scripts SQL)
```

## 2. Base de données

Connexion (MySQL 8.0.43) :

| Paramètre | Valeur |
| --- | --- |
| Hôte | `vps817240.ovh.net` |
| Port | `3306` |
| Utilisateur | `IAI2_team04` |
| Schéma | `IAI2_team04_schema` |
| Mot de passe | voir `src/main/resources/application.properties` |

L'option `-p` demande le mot de passe au clavier.

### Créer les tables

Supprime puis recrée toutes les tables (les données existantes sont perdues) :

```bash
mysql -h vps817240.ovh.net -P 3306 -u IAI2_team04 -p IAI2_team04_schema < sql/01_schema.sql
```

### Charger les données de test

À lancer juste après la création des tables :

```bash
mysql -h vps817240.ovh.net -P 3306 -u IAI2_team04 -p IAI2_team04_schema < sql/02_donnees_test.sql
```

### Remettre la base à zéro (tables + données de test)

```bash
cat sql/01_schema.sql sql/02_donnees_test.sql | mysql -h vps817240.ovh.net -P 3306 -u IAI2_team04 -p IAI2_team04_schema
```

### Ouvrir une console SQL

```bash
mysql -h vps817240.ovh.net -P 3306 -u IAI2_team04 -p IAI2_team04_schema
```

Requêtes utiles une fois connecté :

```sql
SHOW TABLES;
SELECT * FROM photographe;
SELECT id, date_seance, heure_debut, statut, photographe_id FROM seance ORDER BY date_seance;
SELECT numero, montant_ttc, statut_paiement FROM facture;
exit
```

## 3. Lancer l'application

### Tester la connexion à la base (sans démarrer le serveur)

```bash
mvn compile exec:java -Dexec.mainClass=fr.enssat.studiophoto.DBManager
```

Affiche le nom de la base si la connexion fonctionne.

### Démarrer le serveur

```bash
mvn compile exec:java
```

Attendre le message « Application démarrée. (Faites CTRL+C pour arrêter) », puis ouvrir :

| Quoi | Adresse |
| --- | --- |
| Front (page d'accueil) | http://localhost:8080/ |
| API REST | http://localhost:8080/api/... (ex. `/api/photographes`) |
| Documentation de l'API (Swagger UI) | http://localhost:8080/swagger/ |

La documentation est générée à partir de `src/main/resources/static/openapi.yaml` : à mettre à jour quand une route est ajoutée ou modifiée.

`CTRL+C` arrête le serveur.

Après une modification :

- code Java : arrêter le serveur (`CTRL+C`) puis relancer `mvn compile exec:java` ;
- HTML / CSS / JS : relancer aussi, car les fichiers sont recopiés dans `target/` à la compilation.

### Tester l'API en ligne de commande

```bash
curl http://localhost:8080/api/photographes
curl -X POST http://localhost:8080/api/clients -H "Content-Type: application/json" -d '{"type":"PARTICULIER","nom":"Bernard","prenom":"Julie","adressePostale":"12 rue des Lilas, 22300 Lannion","email":"julie@mail.example"}'
curl -X POST http://localhost:8080/api/seances -H "Content-Type: application/json" -d '{"date":"2026-11-18","heureDebut":"14:00:00","duree":"90","lieu":"Lannion","typeSeanceId":"1","photographeId":"1","clientId":"1","clientType":"PARTICULIER"}'
curl -X PATCH http://localhost:8080/api/seances/13/statut -H "Content-Type: application/json" -d '{"statut":"CONFIRMEE"}'
```

Pour créer une séance, remplacer les identifiants par ceux de types, photographes actifs et clients présents en base.

### Lancer le jar exécutable (livrable)

```bash
mvn clean package                      # produit target/studiophoto.jar (toutes les bibliothèques incluses)
java -jar target/studiophoto.jar       # puis http://localhost:8080/
```

Les paramètres de connexion peuvent être donnés au lancement, sans modifier `application.properties`
(utile pour ne pas versionner le mot de passe). Par ordre de priorité :

```bash
java -Ddatabase.password=... -jar target/studiophoto.jar      # 1. option -D
DATABASE_PASSWORD=... java -jar target/studiophoto.jar        # 2. variable d'environnement
                                                              # 3. sinon application.properties
```

Même principe pour `database.url` (`DATABASE_URL`) et `database.login` (`DATABASE_LOGIN`).

## 4. Compiler et tester

```bash
mvn clean compile   # recompiler depuis zéro
mvn test            # lancer les tests unitaires
mvn package         # construire le jar exécutable target/studiophoto.jar
```

## 5. Documentation du projet

| Quoi | Où |
| --- | --- |
| Contrat de l'API (OpenAPI) | `src/main/resources/static/openapi.yaml` |
| Choix techniques et format d'erreur (phase 0) | `docs/choix-techniques.md` |
| Lot 1 – photographes : règles et choix | `docs/lot1-photographes.md` |
| Rapport technique | `livrables/rapport_technique.md` |

## 6. Git (une branche par lot)

```bash
git pull                              # récupérer le travail des autres
git checkout -b lot-3-seances         # créer sa branche (une fois)
git add .
git commit -m "Lot 3 : création d'une séance"
git push -u origin lot-3-seances      # puis ouvrir une merge request sur GitLab
```

Pour récupérer `main` dans sa branche :

```bash
git checkout main && git pull
git checkout lot-3-seances && git merge main
```
