/* =====================================================================
   Clients - Lot 2
   Liste des clients (recherche, filtre par type), formulaire de création /
   modification avec bascule particulier / entreprise, suppression.
   Les outils communs (api, afficherSucces, echapperHtml...) sont dans js/api.js.
   ===================================================================== */

/** Clients chargés depuis l'API, filtrés ensuite côté navigateur. */
let clients = [];
/** Identifiant du client en cours de modification, null pour une création. */
let idEnEdition = null;

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('bouton-nouveau').addEventListener('click', () => ouvrirFormulaire(null));
    document.getElementById('bouton-annuler').addEventListener('click', fermerFormulaire);
    document.getElementById('formulaire').addEventListener('submit', enregistrer);
    document.querySelectorAll('input[name="type"]').forEach(radio =>
        radio.addEventListener('change', () => basculerType(radio.value)));
    document.getElementById('recherche').addEventListener('input', afficherListe);
    document.getElementById('filtre-type').addEventListener('change', afficherListe);

    // Boutons Modifier / Supprimer de chaque ligne (un seul écouteur pour tout le tableau)
    document.getElementById('liste-clients').addEventListener('click', e => {
        const bouton = e.target.closest('button[data-action]');
        if (!bouton) return;
        const client = clients.find(c => c.id === Number(bouton.dataset.id));
        if (bouton.dataset.action === 'modifier') ouvrirFormulaire(client);
        if (bouton.dataset.action === 'supprimer') supprimer(client);
    });

    chargerClients();
});

/* ---------------------------------------------------------------------
   Liste
   --------------------------------------------------------------------- */

async function chargerClients() {
    try {
        clients = await api.get('/clients');
    } catch (e) {
        clients = [];
    }
    afficherListe();
}

/** "Julie Bernard" pour un particulier, raison sociale pour une entreprise. */
function designation(client) {
    return client.type === 'ENTREPRISE' ? client.raisonSociale : client.prenom + ' ' + client.nom;
}

/** Texte en minuscules et sans accents, pour une recherche tolérante. */
function normaliser(texte) {
    return (texte || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

function afficherListe() {
    const recherche = normaliser(document.getElementById('recherche').value.trim());
    const type = document.getElementById('filtre-type').value;

    const visibles = clients.filter(c =>
        (!type || c.type === type)
        && (!recherche || normaliser([designation(c), c.email, c.adressePostale, c.sirenTva].join(' ')).includes(recherche)));

    const corps = document.getElementById('liste-clients');
    if (visibles.length === 0) {
        const texte = clients.length === 0 ? 'Aucun client enregistré.' : 'Aucun client ne correspond à la recherche.';
        corps.innerHTML = '<tr><td colspan="5" class="vide">' + texte + '</td></tr>';
    } else {
        corps.innerHTML = visibles.map(ligneClient).join('');
    }

    document.getElementById('compteur').textContent =
        visibles.length + ' client' + (visibles.length > 1 ? 's' : '') + ' affiché' + (visibles.length > 1 ? 's' : '')
        + (visibles.length !== clients.length ? ' sur ' + clients.length : '') + '.';
}

function ligneClient(client) {
    const estEntreprise = client.type === 'ENTREPRISE';
    const detail = estEntreprise ? '<br><small>' + echapperHtml(client.sirenTva) + '</small>' : '';
    return '<tr>'
        + '<td><strong>' + echapperHtml(designation(client)) + '</strong>' + detail + '</td>'
        + '<td><span class="badge">' + (estEntreprise ? 'Entreprise' : 'Particulier') + '</span></td>'
        + '<td class="masquable"><a href="mailto:' + echapperHtml(client.email) + '">' + echapperHtml(client.email) + '</a></td>'
        + '<td class="masquable">' + echapperHtml(client.adressePostale) + '</td>'
        + '<td class="actions-ligne">'
        + '<button type="button" class="secondaire" data-action="modifier" data-id="' + client.id + '">Modifier</button> '
        + '<button type="button" class="danger" data-action="supprimer" data-id="' + client.id + '">Supprimer</button>'
        + '</td>'
        + '</tr>';
}

/* ---------------------------------------------------------------------
   Formulaire
   --------------------------------------------------------------------- */

/** Ouvre le formulaire vide (client = null) ou pré-rempli pour une modification. */
function ouvrirFormulaire(client) {
    const formulaire = document.getElementById('formulaire');
    formulaire.reset();
    effacerMessages();
    idEnEdition = client ? client.id : null;

    const type = client ? client.type : 'PARTICULIER';
    formulaire.querySelector('input[name="type"][value="' + type + '"]').checked = true;
    basculerType(type);

    if (client) {
        for (const champ of ['nom', 'prenom', 'raisonSociale', 'sirenTva', 'adressePostale', 'email']) {
            formulaire.elements[champ].value = client[champ] || '';
        }
    }
    document.getElementById('titre-formulaire').textContent =
        client ? 'Modifier ' + designation(client) : 'Nouveau client';

    formulaire.hidden = false;
    formulaire.scrollIntoView({ behavior: 'smooth', block: 'start' });
    formulaire.querySelector('fieldset:not([disabled]) input').focus();
}

function fermerFormulaire() {
    document.getElementById('formulaire').hidden = true;
    idEnEdition = null;
}

/** Affiche les champs du type choisi ; le bloc de l'autre type est désactivé (ni validé, ni envoyé). */
function basculerType(type) {
    document.getElementById('champs-particulier').disabled = type !== 'PARTICULIER';
    document.getElementById('champs-entreprise').disabled = type !== 'ENTREPRISE';
}

/** Construit le JSON attendu par l'API : le champ "type" et uniquement les champs de ce type. */
function lireFormulaire() {
    const champs = document.getElementById('formulaire').elements;
    const type = champs.type.value;
    const client = {
        type: type,
        adressePostale: champs.adressePostale.value.trim(),
        email: champs.email.value.trim()
    };
    if (type === 'PARTICULIER') {
        client.nom = champs.nom.value.trim();
        client.prenom = champs.prenom.value.trim();
    } else {
        client.raisonSociale = champs.raisonSociale.value.trim();
        client.sirenTva = champs.sirenTva.value.trim();
    }
    return client;
}

async function enregistrer(e) {
    e.preventDefault();
    const formulaire = document.getElementById('formulaire');
    effacerMessages();

    // Contrôle rapide dans le navigateur ; le serveur refait toutes les vérifications
    if (!formulaire.checkValidity()) {
        formulaire.reportValidity();
        return;
    }

    const bouton = document.getElementById('bouton-enregistrer');
    bouton.disabled = true;
    try {
        const client = lireFormulaire();
        const enregistre = idEnEdition === null
            ? await api.post('/clients', client)
            : await api.put('/clients/' + idEnEdition, client);
        afficherSucces('Client « ' + designation(enregistre) + ' » ' + (idEnEdition === null ? 'créé.' : 'modifié.'));
        fermerFormulaire();
        await chargerClients();
    } catch (erreur) {
        // message déjà affiché par api.js ; le formulaire reste ouvert pour corriger
    } finally {
        bouton.disabled = false;
    }
}

/* ---------------------------------------------------------------------
   Suppression
   --------------------------------------------------------------------- */

async function supprimer(client) {
    if (!confirm('Supprimer le client « ' + designation(client) + ' » ?')) {
        return;
    }
    effacerMessages();
    try {
        await api.delete('/clients/' + client.id);
        afficherSucces('Client « ' + designation(client) + ' » supprimé.');
        if (idEnEdition === client.id) fermerFormulaire();
        await chargerClients();
    } catch (erreur) {
        // 409 si le client a des séances : le message de l'API est déjà affiché
    }
}
