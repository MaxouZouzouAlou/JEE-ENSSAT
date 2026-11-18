package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.SpecialiteDAO;
import fr.enssat.studiophoto.model.Specialite;

import java.util.List;

/** Faux DAO : les trois spécialités du script de données de test. */
class FauxSpecialiteDAO implements SpecialiteDAO {

    static final Specialite REPORTAGE = new Specialite(1L, "Reportage");
    static final Specialite MARIAGE = new Specialite(2L, "Mariage et fêtes");
    static final Specialite FAMILLE = new Specialite(3L, "Famille");

    @Override
    public List<Specialite> lister() {
        return List.of(FAMILLE, MARIAGE, REPORTAGE);
    }
}
