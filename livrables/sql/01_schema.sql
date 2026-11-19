-- =====================================================================
-- Studio photo - Schéma de la base de données (MySQL 8)
-- Recrée toutes les tables : à exécuter sur le schéma fourni.
-- =====================================================================

SET NAMES utf8mb4;

-- Suppression dans l'ordre inverse des dépendances (script ré-exécutable)
DROP TABLE IF EXISTS facture;
DROP TABLE IF EXISTS seance;
DROP TABLE IF EXISTS type_seance;
DROP TABLE IF EXISTS client;
DROP TABLE IF EXISTS photographe_specialite;
DROP TABLE IF EXISTS photographe;
DROP TABLE IF EXISTS specialite;

-- ---------------------------------------------------------------------
-- Photographes et spécialités (relation plusieurs-à-plusieurs)
-- ---------------------------------------------------------------------
CREATE TABLE specialite (
    id      BIGINT       NOT NULL AUTO_INCREMENT,
    libelle VARCHAR(50)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_specialite_libelle UNIQUE (libelle)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE photographe (
    id     BIGINT        NOT NULL AUTO_INCREMENT,
    nom    VARCHAR(100)  NOT NULL,
    email  VARCHAR(150)  NOT NULL,
    actif  BOOLEAN       NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id),
    CONSTRAINT uk_photographe_email UNIQUE (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE photographe_specialite (
    photographe_id BIGINT NOT NULL,
    specialite_id  BIGINT NOT NULL,
    PRIMARY KEY (photographe_id, specialite_id),
    CONSTRAINT fk_ps_photographe FOREIGN KEY (photographe_id)
        REFERENCES photographe (id) ON DELETE CASCADE,
    CONSTRAINT fk_ps_specialite  FOREIGN KEY (specialite_id)
        REFERENCES specialite (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- Clients : héritage en table unique, la colonne "type" distingue
-- les particuliers (nom, prénom) des entreprises (raison sociale, SIREN/TVA)
-- ---------------------------------------------------------------------
CREATE TABLE client (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    type            ENUM('PARTICULIER', 'ENTREPRISE') NOT NULL,
    adresse_postale VARCHAR(255)  NOT NULL,
    email           VARCHAR(150)  NOT NULL,
    nom             VARCHAR(100)  NULL,   -- particulier
    prenom          VARCHAR(100)  NULL,   -- particulier
    raison_sociale  VARCHAR(150)  NULL,   -- entreprise
    siren_tva       VARCHAR(20)   NULL,   -- entreprise : SIREN ou n° de TVA
    PRIMARY KEY (id),
    CONSTRAINT ck_client_type CHECK (
        (type = 'PARTICULIER' AND nom IS NOT NULL AND prenom IS NOT NULL)
        OR (type = 'ENTREPRISE' AND raison_sociale IS NOT NULL AND siren_tva IS NOT NULL)
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- Types de séance et tarif (sert au calcul du montant HT de la facture)
-- ---------------------------------------------------------------------
CREATE TABLE type_seance (
    id               BIGINT         NOT NULL AUTO_INCREMENT,
    libelle          VARCHAR(100)   NOT NULL,
    tarif_horaire_ht DECIMAL(10, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_type_seance_libelle UNIQUE (libelle),
    CONSTRAINT ck_type_seance_tarif CHECK (tarif_horaire_ht >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- Séances photo
-- ---------------------------------------------------------------------
CREATE TABLE seance (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    date_seance    DATE          NOT NULL,
    heure_debut    TIME          NOT NULL,
    duree_minutes  INT           NOT NULL,
    lieu           VARCHAR(255)  NOT NULL,
    statut         ENUM('PLANIFIEE', 'CONFIRMEE', 'TERMINEE', 'ANNULEE') NOT NULL DEFAULT 'PLANIFIEE',
    type_seance_id BIGINT        NOT NULL,
    photographe_id BIGINT        NOT NULL,
    client_id      BIGINT        NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT ck_seance_duree CHECK (duree_minutes > 0),
    CONSTRAINT fk_seance_type        FOREIGN KEY (type_seance_id) REFERENCES type_seance (id) ON DELETE RESTRICT,
    CONSTRAINT fk_seance_photographe FOREIGN KEY (photographe_id) REFERENCES photographe (id) ON DELETE RESTRICT,
    CONSTRAINT fk_seance_client      FOREIGN KEY (client_id)      REFERENCES client (id)      ON DELETE RESTRICT,
    -- accélère la recherche de chevauchements et le planning d'un photographe
    INDEX idx_seance_photographe_date (photographe_id, date_seance)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

-- ---------------------------------------------------------------------
-- Factures : une seule facture par séance (seance_id UNIQUE, relation 1-1).
-- Les coordonnées du client et le détail de la prestation sont copiés
-- à la génération, pour qu'une facture émise ne change plus.
-- ---------------------------------------------------------------------
CREATE TABLE facture (
    id                       BIGINT         NOT NULL AUTO_INCREMENT,
    numero                   VARCHAR(20)    NOT NULL,              -- ex. FAC-2026-0001
    date_emission            DATE           NOT NULL,
    montant_ht               DECIMAL(10, 2) NOT NULL,
    taux_tva                 DECIMAL(5, 2)  NOT NULL,              -- en %, ex. 20.00
    montant_ttc              DECIMAL(10, 2) NOT NULL,
    statut_paiement          ENUM('EN_ATTENTE', 'PAYEE', 'ANNULEE') NOT NULL DEFAULT 'EN_ATTENTE',
    seance_id                BIGINT         NOT NULL,
    -- copie des coordonnées du client
    client_designation       VARCHAR(200)   NOT NULL,              -- "Prénom Nom" ou raison sociale
    client_adresse           VARCHAR(255)   NOT NULL,
    client_email             VARCHAR(150)   NOT NULL,
    client_siren_tva         VARCHAR(20)    NULL,
    -- copie du détail de la prestation
    prestation_date          DATE           NOT NULL,
    prestation_heure         TIME           NOT NULL,
    prestation_duree_minutes INT            NOT NULL,
    prestation_type          VARCHAR(100)   NOT NULL,
    photographe_nom          VARCHAR(100)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_facture_numero UNIQUE (numero),
    CONSTRAINT uk_facture_seance UNIQUE (seance_id),
    CONSTRAINT ck_facture_montants CHECK (montant_ht >= 0 AND montant_ttc >= montant_ht),
    CONSTRAINT fk_facture_seance FOREIGN KEY (seance_id) REFERENCES seance (id) ON DELETE RESTRICT
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
