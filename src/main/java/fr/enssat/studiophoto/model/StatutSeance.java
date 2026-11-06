package fr.enssat.studiophoto.model;

/**
 * Statut d'une séance, stocké dans la colonne {@code seance.statut}.
 *
 * <p>Transitions autorisées (à coder par le lot 3) :
 * PLANIFIEE vers CONFIRMEE ou ANNULEE ; CONFIRMEE vers TERMINEE (si la date est passée)
 * ou ANNULEE ; TERMINEE et ANNULEE sont définitifs.</p>
 */
public enum StatutSeance {
    PLANIFIEE,
    CONFIRMEE,
    TERMINEE,
    ANNULEE
}
