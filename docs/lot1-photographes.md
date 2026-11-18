# Lot 1 – Photographes et spécialités

Notes pour la section du rapport consacrée au lot 1.

## Ce qui a été fait

| Tâche | Fichiers |
| --- | --- |
| 1.1 CRUD avec validation | `service/PhotographeService`, `dao/PhotographeDAO` + `PhotographeDAOImpl` |
| 1.2 Spécialités | `dao/SpecialiteDAO(Impl)`, table `photographe_specialite` |
| 1.3 Activer / désactiver | `PhotographeService.changerStatut`, méthodes du lot 1 dans `dao/SeanceDAO`, `exception/ConfirmationRequiseException(Mapper)` |
| 1.4 Règle de suppression | `PhotographeService.supprimer` |
| 1.5 Ressources REST | `resource/PhotographeResource`, `resource/SpecialiteResource` |
| 1.6 Tests unitaires | `src/test/.../service/PhotographeServiceTest` (24 tests) + faux DAO |
| 1.7 Pages front | `static/photographes.html`, `static/js/photographes.js` |

## Routes

| Méthode et route | Succès | Erreurs |
| --- | --- | --- |
| `GET /api/photographes[?actif=true\|false]` | 200 | – |
| `GET /api/photographes/{id}` | 200 | 404 |
| `POST /api/photographes` | 201 | 400, 409 (email déjà utilisé) |
| `PUT /api/photographes/{id}` | 200 | 400, 404, 409 |
| `PATCH /api/photographes/{id}/statut[?confirmer=true]` | 200 | 400, 404, 409 (séances à venir) |
| `DELETE /api/photographes/{id}` | 204 | 404, 409 (séances existantes) |
| `GET /api/specialites` | 200 | – |

## Règles métier et choix

**Validation.** Nom obligatoire (100 caractères max). Email obligatoire, au format
`texte@domaine.ext`, unique sans tenir compte de la casse : il est enregistré en
minuscules pour éviter deux comptes `Alice@…` et `alice@…`. L'unicité est vérifiée par le
service (message clair) et garantie par la contrainte `UNIQUE` de la base (cas de deux
requêtes simultanées, traduit aussi en 409). Une spécialité inconnue donne 400 ; les
doublons sont ignorés. Un photographe est toujours créé actif, et `PUT` ne change pas son
statut : le statut a sa propre route, avec ses propres règles.

**Désactivation en deux temps.** Si le photographe a des séances *à venir* (PLANIFIEE ou
CONFIRMEE, dont le début est dans le futur), une première requête répond 409 avec leur
nombre (`nombreSeances`) sans rien modifier. Le front affiche ce nombre dans une boîte de
confirmation, puis renvoie la requête avec `?confirmer=true` : le photographe est
désactivé et ces séances passent en ANNULEE. Les séances passées (même restées PLANIFIEE
ou CONFIRMEE), terminées ou déjà annulées ne sont pas touchées : elles appartiennent à
l'historique et le lot 3 les fait passer à TERMINEE. Une séance déjà commencée n'est plus
« à venir ». Le photographe est désactivé *avant* l'annulation des séances, pour qu'aucune
nouvelle séance ne puisse lui être affectée entre les deux.

Choix : le serveur décide (et compte) lui-même, au lieu de laisser le front vérifier les
séances puis désactiver. La règle ne peut donc pas être contournée par un autre client de
l'API, et la réponse 409 reste dans le format d'erreur commun.

**Suppression refusée dès qu'il existe une séance** (409, avec proposition de
désactivation). Supprimer un photographe qui a des séances obligerait soit à supprimer
ces séances (perte du planning passé, factures orphelines alors que la facture doit rester
consultable), soit à laisser des séances sans photographe. La désactivation conserve tout
l'historique et empêche les nouvelles affectations : c'est l'équivalent métier d'une
suppression. La base protège aussi la règle (`ON DELETE RESTRICT` sur `seance.photographe_id`).
Un photographe sans aucune séance (créé par erreur) peut, lui, être supprimé.

**Testabilité.** Le service reçoit ses DAO et une `Clock` par le constructeur ; les tests
utilisent des DAO en mémoire écrits à la main (le faux `SeanceDAO` hérite de la classe du
lot 3 et ne redéfinit que les trois méthodes utilisées) et une horloge fixe, ce qui permet de tester
précisément la limite passé / futur (séance ce matin, cet après-midi, pile maintenant).

## Interface

Liste avec filtre (tous / actifs / inactifs), formulaire de création et de modification
avec cases à cocher pour les spécialités, boutons Modifier / Désactiver ou Activer /
Supprimer sur chaque ligne. Les confirmations utilisent l'élément HTML `<dialog>`. Si la
suppression est refusée, l'interface propose directement la désactivation.
