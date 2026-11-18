package fr.enssat.studiophoto.model;

import java.time.LocalDateTime;

/**
 * Statut d'une séance, stocké dans la colonne {@code seance.statut}.
 *
 * <p>Transitions autorisées (à coder par le lot 3) :
 * PLANIFIEE vers CONFIRMEE ou ANNULEE ; CONFIRMEE vers TERMINEE (si la date est passée)
 * ou ANNULEE ; TERMINEE et ANNULEE sont définitifs.</p>
 */
public enum StatutSeance {
    PLANIFIEE("PLANIFIEE"),
    CONFIRMEE("CONFIRMEE"),
    TERMINEE("TERMINEE"),
    ANNULEE("ANNULEE");

    private String libelle;

    // Constructeur sans argument : assigne automatiquement le nom de l'énumération
    StatutSeance() {
        this.libelle = this.name();
    }

    // Constructeur avec argument (au cas où vous auriez besoin d'un libellé personnalisé)
    StatutSeance(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public boolean peutPasserA(StatutSeance statutCible, LocalDateTime finSeance) {
        if (statutCible == null) {
            return false;
        }

        return switch (this) {
            case PLANIFIEE -> statutCible == CONFIRMEE || statutCible == ANNULEE;
            case CONFIRMEE -> statutCible == ANNULEE
                    || (statutCible == TERMINEE && finSeance != null && !LocalDateTime.now().isBefore(finSeance));
            case TERMINEE, ANNULEE -> false;
        };
    }
}