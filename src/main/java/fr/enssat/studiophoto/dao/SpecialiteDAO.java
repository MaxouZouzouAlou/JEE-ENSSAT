package fr.enssat.studiophoto.dao;

import fr.enssat.studiophoto.model.Specialite;

import java.util.List;

/** Accès aux spécialités (table {@code specialite}) : liste fixe, en lecture seule. */
public interface SpecialiteDAO {

    /** Toutes les spécialités, triées par libellé. */
    List<Specialite> lister();
}
