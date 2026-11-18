package fr.enssat.studiophoto.dao;

import fr.enssat.studiophoto.model.Photographe;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

/**
 * Accès aux photographes (table {@code photographe}) et à leurs spécialités
 * (table de liaison {@code photographe_specialite}). Les photographes renvoyés
 * ont toujours leur liste de spécialités remplie.
 */
public interface PhotographeDAO {

    /**
     * Photographes triés par nom.
     *
     * @param actif {@code true} : actifs seulement, {@code false} : inactifs seulement, {@code null} : tous
     */
    List<Photographe> lister(Boolean actif);

    Optional<Photographe> trouverParId(long id);

    /** Recherche par email, sans tenir compte de la casse. */
    Optional<Photographe> trouverParEmail(String email);

    /** Insère le photographe et ses spécialités ; renvoie le photographe avec son identifiant. */
    Photographe creer(Photographe photographe);

    /** Met à jour nom et email, et remplace la liste des spécialités. Le statut actif n'est pas modifié. */
    void modifier(Photographe photographe);

    void changerActif(long id, boolean actif);

    /** Supprime le photographe (ses liaisons aux spécialités sont supprimées en cascade). */
    void supprimer(long id);

    /**
     * {@code true} si le photographe n'a aucune séance non annulée entre les deux dates (incluses).
     * Méthode ajoutée par le lot 3 (le nom désigne en fait une disponibilité).
     */
    boolean isActif(Long photographeId, Date debut, Date fin);
}
