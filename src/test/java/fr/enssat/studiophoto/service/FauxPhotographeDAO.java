package fr.enssat.studiophoto.service;

import fr.enssat.studiophoto.dao.PhotographeDAO;
import fr.enssat.studiophoto.model.Photographe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/** Faux DAO en mémoire : stocke des copies pour que le test ne modifie pas les données par erreur. */
class FauxPhotographeDAO implements PhotographeDAO {

    final Map<Long, Photographe> photographes = new LinkedHashMap<>();
    private long prochainId = 1;

    /** Ajoute directement un photographe (préparation des tests), sans passer par le service. */
    Photographe ajouter(String nom, String email, boolean actif) {
        Photographe p = new Photographe(prochainId++, nom, email, actif);
        photographes.put(p.getId(), copie(p));
        return p;
    }

    @Override
    public List<Photographe> lister(Boolean actif) {
        return photographes.values().stream()
                .filter(p -> actif == null || p.isActif() == actif)
                .sorted(Comparator.comparing(Photographe::getNom))
                .map(FauxPhotographeDAO::copie)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<Photographe> trouverParId(long id) {
        return Optional.ofNullable(photographes.get(id)).map(FauxPhotographeDAO::copie);
    }

    @Override
    public Optional<Photographe> trouverParEmail(String email) {
        return photographes.values().stream()
                .filter(p -> p.getEmail().equalsIgnoreCase(email))
                .findFirst().map(FauxPhotographeDAO::copie);
    }

    @Override
    public Photographe creer(Photographe photographe) {
        photographe.setId(prochainId++);
        photographes.put(photographe.getId(), copie(photographe));
        return copie(photographe);
    }

    @Override
    public void modifier(Photographe photographe) {
        Photographe existant = photographes.get(photographe.getId());
        Photographe nouveau = copie(photographe);
        nouveau.setActif(existant.isActif());
        photographes.put(nouveau.getId(), nouveau);
    }

    @Override
    public void changerActif(long id, boolean actif) {
        photographes.get(id).setActif(actif);
    }

    @Override
    public void supprimer(long id) {
        photographes.remove(id);
    }

    static Photographe copie(Photographe p) {
        Photographe c = new Photographe(p.getId(), p.getNom(), p.getEmail(), p.isActif());
        c.setSpecialites(new ArrayList<>(p.getSpecialites()));
        return c;
    }
}
