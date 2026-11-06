-- =====================================================================
-- Studio photo - Données de test
-- À exécuter après 01_schema.sql.
-- Les dates sont calculées à partir du jour d'exécution (CURDATE()),
-- donc les séances "passées" et "à venir" restent cohérentes.
-- Toutes les personnes, entreprises et adresses sont fictives.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Spécialités (liste imposée par le sujet)
-- ---------------------------------------------------------------------
INSERT INTO specialite (id, libelle) VALUES
    (1, 'Reportage'),
    (2, 'Mariage et fêtes'),
    (3, 'Famille');

-- ---------------------------------------------------------------------
-- Photographes : 5 actifs, 1 inactif (David Moreau)
-- ---------------------------------------------------------------------
INSERT INTO photographe (id, nom, email, actif) VALUES
    (1, 'Alice Martin',  'alice.martin@studio.example',  TRUE),
    (2, 'Bruno Leroy',   'bruno.leroy@studio.example',   TRUE),
    (3, 'Chloé Dubois',  'chloe.dubois@studio.example',  TRUE),
    (4, 'David Moreau',  'david.moreau@studio.example',  FALSE),
    (5, 'Emma Petit',    'emma.petit@studio.example',    TRUE),
    (6, 'Fabien Roux',   'fabien.roux@studio.example',   TRUE);

INSERT INTO photographe_specialite (photographe_id, specialite_id) VALUES
    (1, 2), (1, 3),
    (2, 1),
    (3, 1), (3, 3),
    (4, 2),
    (5, 1), (5, 2), (5, 3),
    (6, 3);

-- ---------------------------------------------------------------------
-- Clients : 4 particuliers, 3 entreprises
-- ---------------------------------------------------------------------
INSERT INTO client (id, type, adresse_postale, email, nom, prenom, raison_sociale, siren_tva) VALUES
    (1, 'PARTICULIER', '12 rue des Lilas, 22300 Lannion',          'julie.bernard@mail.example',     'Bernard',   'Julie',   NULL, NULL),
    (2, 'PARTICULIER', '4 impasse des Mouettes, 22700 Perros-Guirec', 'thomas.lefebvre@mail.example', 'Lefebvre',  'Thomas',  NULL, NULL),
    (3, 'PARTICULIER', '27 avenue du Port, 22560 Trébeurden',      'camille.garnier@mail.example',   'Garnier',   'Camille', NULL, NULL),
    (4, 'PARTICULIER', '8 chemin du Moulin, 22300 Ploubezre',      'hugo.fontaine@mail.example',     'Fontaine',  'Hugo',    NULL, NULL),
    (5, 'ENTREPRISE',  '15 boulevard de la Mer, 22300 Lannion',    'contact@horizon-evenements.example', NULL, NULL, 'Agence Horizon Événements', '901234567'),
    (6, 'ENTREPRISE',  '3 rue de l''Industrie, 22300 Lannion',      'contact@ateliers-kerlann.example',   NULL, NULL, 'Les Ateliers Kerlann',      'FR12902345678'),
    (7, 'ENTREPRISE',  '41 quai d''Aiguillon, 22300 Lannion',       'accueil@tregor-conseil.example',     NULL, NULL, 'Cabinet Trégor Conseil',    '903456789');

-- ---------------------------------------------------------------------
-- Types de séance et tarif horaire HT (hypothèse de tarification)
-- ---------------------------------------------------------------------
INSERT INTO type_seance (id, libelle, tarif_horaire_ht) VALUES
    (1, 'Portrait',             80.00),
    (2, 'Mariage',             150.00),
    (3, 'Reportage entreprise', 120.00),
    (4, 'Famille',              90.00);

-- ---------------------------------------------------------------------
-- Séances : couvrent les 4 statuts, dans le passé et dans le futur
-- ---------------------------------------------------------------------
INSERT INTO seance (id, date_seance, heure_debut, duree_minutes, lieu, statut, type_seance_id, photographe_id, client_id) VALUES
    -- Séances TERMINEE (passées), facturées : alimentent les statistiques
    ( 1, CURDATE() - INTERVAL 60 DAY, '14:00', 480, 'Domaine des Hortensias, Perros-Guirec', 'TERMINEE', 2, 1, 1),
    ( 2, CURDATE() - INTERVAL 45 DAY, '10:00', 120, 'Studio du centre-ville, Lannion',        'TERMINEE', 1, 2, 2),
    ( 3, CURDATE() - INTERVAL 40 DAY, '09:00', 240, 'Locaux du client, Lannion',              'TERMINEE', 3, 2, 5),
    ( 4, CURDATE() - INTERVAL 30 DAY, '15:00', 180, 'Plage de Trestraou, Perros-Guirec',      'TERMINEE', 4, 3, 3),
    ( 5, CURDATE() - INTERVAL 25 DAY, '11:00', 360, 'Salle des fêtes, Trébeurden',            'TERMINEE', 2, 5, 4),
    ( 6, CURDATE() - INTERVAL 20 DAY, '09:30',  90, 'Studio du centre-ville, Lannion',        'TERMINEE', 1, 6, 1),
    ( 7, CURDATE() - INTERVAL 15 DAY, '14:00', 300, 'Locaux du client, Lannion',              'TERMINEE', 3, 5, 6),
    ( 8, CURDATE() - INTERVAL 12 DAY, '10:00', 120, 'Parc de Ker Uhel, Lannion',              'TERMINEE', 4, 4, 2),
    -- Séances TERMINEE sans facture : pour tester "Générer la facture"
    ( 9, CURDATE() - INTERVAL 10 DAY, '16:00',  60, 'Studio du centre-ville, Lannion',        'TERMINEE', 1, 1, 3),
    (10, CURDATE() - INTERVAL  5 DAY, '10:00', 120, 'Locaux du client, Lannion',              'TERMINEE', 3, 3, 7),
    -- Séance CONFIRMEE dont la date est passée : pour tester le passage à TERMINEE
    (11, CURDATE() - INTERVAL  2 DAY, '14:00', 120, 'Studio du centre-ville, Lannion',        'CONFIRMEE', 1, 6, 4),
    -- Séance ANNULEE passée
    (12, CURDATE() - INTERVAL  8 DAY, '10:00',  60, 'Studio du centre-ville, Lannion',        'ANNULEE',  1, 3, 2),
    -- Séances à venir
    -- n°13 : Alice est occupée de 10:00 à 12:00 -> une séance à 11:00 le même jour doit être refusée
    (13, CURDATE() + INTERVAL  3 DAY, '10:00', 120, 'Studio du centre-ville, Lannion',        'PLANIFIEE', 1, 1, 2),
    (14, CURDATE() + INTERVAL  5 DAY, '14:00', 480, 'Domaine des Hortensias, Perros-Guirec', 'CONFIRMEE', 2, 5, 3),
    (15, CURDATE() + INTERVAL  7 DAY, '09:00', 180, 'Locaux du client, Lannion',              'PLANIFIEE', 3, 2, 5),
    (16, CURDATE() + INTERVAL 10 DAY, '15:00', 120, 'Jardin public, Lannion',                 'PLANIFIEE', 4, 3, 1),
    -- n°17 : séance annulée lors de la désactivation de David Moreau
    (17, CURDATE() + INTERVAL 12 DAY, '10:00', 120, 'Parc de Ker Uhel, Lannion',              'ANNULEE',  4, 4, 4),
    -- n°18 : Fabien a une séance à venir -> le désactiver doit demander une confirmation
    (18, CURDATE() + INTERVAL 14 DAY, '13:00', 240, 'Plage de Trestraou, Perros-Guirec',      'PLANIFIEE', 4, 6, 3),
    (19, CURDATE() + INTERVAL 20 DAY, '11:00', 420, 'Domaine des Hortensias, Perros-Guirec', 'CONFIRMEE', 2, 1, 4);

-- ---------------------------------------------------------------------
-- Factures des séances 1 à 8 : montants calculés depuis le tarif
-- (HT = tarif horaire x durée, TVA 20 %), coordonnées copiées.
-- ---------------------------------------------------------------------
INSERT INTO facture (numero, date_emission, montant_ht, taux_tva, montant_ttc, statut_paiement, seance_id,
                     client_designation, client_adresse, client_email, client_siren_tva,
                     prestation_date, prestation_heure, prestation_duree_minutes, prestation_type, photographe_nom)
SELECT CONCAT('FAC-', YEAR(CURDATE()), '-', LPAD(f.num, 4, '0')),
       s.date_seance + INTERVAL 1 DAY,
       ROUND(t.tarif_horaire_ht * s.duree_minutes / 60, 2),
       20.00,
       ROUND(ROUND(t.tarif_horaire_ht * s.duree_minutes / 60, 2) * 1.20, 2),
       f.statut,
       s.id,
       COALESCE(c.raison_sociale, CONCAT(c.prenom, ' ', c.nom)),
       c.adresse_postale,
       c.email,
       c.siren_tva,
       s.date_seance,
       s.heure_debut,
       s.duree_minutes,
       t.libelle,
       p.nom
FROM (
          SELECT 1 AS num, 1 AS seance_id, 'PAYEE'      AS statut
    UNION ALL SELECT 2, 2, 'PAYEE'
    UNION ALL SELECT 3, 3, 'PAYEE'
    UNION ALL SELECT 4, 4, 'PAYEE'
    UNION ALL SELECT 5, 5, 'EN_ATTENTE'
    UNION ALL SELECT 6, 6, 'ANNULEE'
    UNION ALL SELECT 7, 7, 'EN_ATTENTE'
    UNION ALL SELECT 8, 8, 'PAYEE'
) f
JOIN seance      s ON s.id = f.seance_id
JOIN type_seance t ON t.id = s.type_seance_id
JOIN client      c ON c.id = s.client_id
JOIN photographe p ON p.id = s.photographe_id
ORDER BY f.num;
