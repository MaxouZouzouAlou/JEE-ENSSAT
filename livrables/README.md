# Livrables – groupe JEEPhoto

Projet de développement Java web IAI2 2026-2027 – Système de gestion d'un studio de photographie.
Groupe : Maxime (lot 1), Kyllian (lot 2), Pierre (lot 3), Pierrick (lot 4), Mathis (lot 5).

| Livrable demandé | Fichier |
| --- | --- |
| Application web sous forme de jar | [`studiophoto.jar`](studiophoto.jar) (exécutable, toutes les bibliothèques incluses) |
| Code source | dépôt https://gitlab.enssat.fr/maubry/jeephoto, branche `main` |
| Scripts de création de la base | [`sql/01_schema.sql`](sql/01_schema.sql) (tables), [`sql/02_donnees_test.sql`](sql/02_donnees_test.sql) (données de test) |
| Documentation de l'API (OpenAPI) | [`openapi.yaml`](openapi.yaml), aussi affichée par Swagger UI sur http://localhost:8080/swagger/ |
| Procédure d'installation | ci-dessous (version détaillée dans le [`README.md`](../README.md) du dépôt) |
| Rapport technique | [`rapport_technique.pdf`](rapport_technique.pdf) (source : [`rapport_technique.md`](rapport_technique.md)) |

## Procédure d'installation

**Prérequis** : Java 25, un client MySQL, et l'accès au serveur MySQL du groupe (`vps817240.ovh.net`, port 3306,
utilisateur `IAI2_team04`, schéma `IAI2_team04_schema`).

1. **Créer les tables et charger les données de test** (recrée les tables : les données existantes sont perdues).
   Depuis ce dossier `livrables` :

   ```bash
   cat sql/01_schema.sql sql/02_donnees_test.sql | mysql -h vps817240.ovh.net -P 3306 -u IAI2_team04 -p IAI2_team04_schema
   ```

2. **Lancer l'application** :

   ```bash
   java -jar studiophoto.jar
   ```

   Le jar contient la configuration de connexion du groupe. Pour une autre base, donner les paramètres au lancement :

   ```bash
   java -Ddatabase.url=jdbc:mysql://<hôte>:3306/<schéma> -Ddatabase.login=<utilisateur> -Ddatabase.password=<mot de passe> -jar studiophoto.jar
   ```

   (ou les variables d'environnement `DATABASE_URL`, `DATABASE_LOGIN`, `DATABASE_PASSWORD`).

3. **Attendre** le message « Application démarrée », puis ouvrir :

   | Quoi | Adresse |
   | --- | --- |
   | Site | http://localhost:8080/ |
   | API REST | http://localhost:8080/api/… |
   | Documentation Swagger | http://localhost:8080/swagger/ |

   Au premier affichage, choisir un profil : **Gestionnaire** donne accès à toutes les rubriques.

Pour recompiler depuis les sources (Java 25 et Maven) : `mvn clean package` à la racine du dépôt produit
`target/studiophoto.jar` et lance les tests unitaires (`mvn test` pour les tests seuls).
