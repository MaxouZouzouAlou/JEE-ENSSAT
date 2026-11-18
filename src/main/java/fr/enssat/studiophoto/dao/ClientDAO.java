package fr.enssat.studiophoto.dao;

import fr.enssat.studiophoto.model.Client;

import java.util.List;
import java.util.Optional;

/** Accès à la table {@code client} (particuliers et entreprises). */
public interface ClientDAO {

    /** Tous les clients, triés par identifiant. */
    List<Client> findAll();

    Optional<Client> findById(long id);

    /** Insère le client et renseigne son identifiant généré. */
    Client create(Client client);

    /** Met à jour le client ; renvoie {@code false} si aucun client ne porte cet identifiant. */
    boolean update(Client client);

    /** Supprime le client ; renvoie {@code false} si aucun client ne porte cet identifiant. */
    boolean delete(long id);

    /** Nombre de séances (tous statuts confondus) rattachées à ce client. */
    int countSeances(long clientId);
}
