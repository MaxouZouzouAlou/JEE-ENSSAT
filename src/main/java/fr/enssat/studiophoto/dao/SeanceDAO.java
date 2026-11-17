package fr.enssat.studiophoto.dao;

import java.time.LocalDateTime;

/**
 * Accès aux séances (table {@code seance}). Interface fournie dès la phase 0 :
 * le lot 3 la complète (création, modification, planning, chevauchements...).
 *
 * <p>Une séance « à venir » est une séance PLANIFIEE ou CONFIRMEE dont le début
 * (date + heure) est strictement après l'instant {@code maintenant} donné.
 * L'instant est passé en paramètre pour que les services restent testables.</p>
 */
public interface SeanceDAO {

    /** Nombre total de séances d'un photographe, tous statuts et toutes dates confondus. */
    int compterParPhotographe(long photographeId);

    /** Nombre de séances à venir (PLANIFIEE ou CONFIRMEE, début après {@code maintenant}) d'un photographe. */
    int compterAVenirParPhotographe(long photographeId, LocalDateTime maintenant);

    /**
     * Passe en ANNULEE les séances à venir d'un photographe (utilisé par le lot 1 à la désactivation).
     * Les séances passées, terminées ou déjà annulées ne sont pas modifiées.
     *
     * @return le nombre de séances annulées
     */
    int annulerAVenirParPhotographe(long photographeId, LocalDateTime maintenant);
}
