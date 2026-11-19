/* =====================================================================
   Fiche d'une séance : détail + boutons de changement de statut.
   Le serveur reste la référence pour les transitions (PATCH /api/seances/{id}/statut),
   ici on grise seulement les boutons qui seraient refusés.
   Les outils communs sont dans js/api.js, la proposition de facture dans js/facturation.js.
   ===================================================================== */

// transitions permises depuis chaque statut (même table que StatutSeance.peutPasserA côté serveur)
const TRANSITIONS = {
    PLANIFIEE: ['CONFIRMEE', 'ANNULEE'],
    CONFIRMEE: ['TERMINEE', 'ANNULEE'],
    TERMINEE: [],
    ANNULEE: []
};

const BOUTONS = {
    CONFIRMEE: { libelle: 'Confirmer', classe: '' },
    TERMINEE: { libelle: 'Terminer', classe: '' },
    ANNULEE: { libelle: 'Annuler la séance', classe: 'danger' }
};

const MESSAGES = {
    CONFIRMEE: 'Séance confirmée.',
    TERMINEE: 'Séance terminée.',
    ANNULEE: 'Séance annulée.'
};

// séance affichée (null tant qu'elle n'est pas chargée) et factures déjà émises par séance
let seance = null;
let facturesParSeance = new Map();

document.addEventListener('DOMContentLoaded', () => {
    const fiche = document.getElementById('fiche');
    // bouton "Générer la facture" d'une séance déjà terminée (js/facturation.js)
    activerBoutonsFacture(fiche);
    fiche.addEventListener('click', e => {
        const bouton = e.target.closest('button[data-statut]');
        if (bouton) changerStatut(bouton.dataset.statut, bouton);
        const suppression = e.target.closest('button[data-action="supprimer-seance"]');
        if (suppression) supprimerSeance(suppression);
    });
    chargerSeance();
});

async function chargerSeance() {
    const id = lireParametre('id');
    if (!id || !/^\d+$/.test(id)) {
        afficherErreur('Identifiant de séance invalide.');
        return;
    }
    try {
        seance = await api.get('/seances/' + encodeURIComponent(id));
        if (!seance) {
            afficherErreur('Séance introuvable.');
            return;
        }
        facturesParSeance = await chargerFacturesParSeance();
        afficherFiche();
    } catch (e) {
        // api.get affiche déjà le message d'erreur dans #messages.
    }
}

function designationClient(client) {
    if (!client) return '';
    return client.type === 'ENTREPRISE'
        ? client.raisonSociale
        : [client.prenom, client.nom].filter(Boolean).join(' ');
}

// la séance est finie quand son heure de fin est passée (même règle que le serveur)
function seanceFinie(s) {
    const debut = new Date(s.date.substring(0, 10) + 'T' + s.heureDebut);
    return Date.now() >= debut.getTime() + s.duree * 60000;
}

function afficherFiche() {
    const fiche = document.getElementById('fiche');
    fiche.innerHTML =
        '<div class="fiche-tete"><h2>Séance n° ' + echapperHtml(seance.id) + '</h2>'
        + '<span>' + badgeStatut(seance.statut) + '</span></div>'
        + '<p>Le ' + echapperHtml(formaterDate(seance.date)) + ' à ' + echapperHtml(formaterHeure(seance.heureDebut))
        + ' · ' + echapperHtml(formaterDuree(seance.duree)) + '</p>'
        + '<p>Lieu : ' + echapperHtml(seance.lieu) + '</p>'
        + '<p>Type : ' + echapperHtml(seance.typeSeance && seance.typeSeance.libelle) + '</p>'
        + '<p>Photographe : ' + echapperHtml(seance.photographe && seance.photographe.nom) + '</p>'
        + '<p>Client : ' + echapperHtml(designationClient(seance.client)) + '</p>'
        + '<div class="actions">' + htmlActions() + htmlBoutonSupprimer() + '</div>';
    fiche.hidden = false;
}

function htmlActions() {
    const cibles = TRANSITIONS[seance.statut] || [];
    if (cibles.length === 0) {
        // séance TERMINEE : sa facture, ANNULEE : rien à faire
        return seance.statut === 'TERMINEE'
            ? htmlBoutonFacture(seance, facturesParSeance)
            : '<span class="aide">Une séance annulée ne peut plus être modifiée.</span>';
    }
    let html = cibles.map(cible => {
        const bouton = BOUTONS[cible];
        const bloque = cible === 'TERMINEE' && !seanceFinie(seance);
        return '<button type="button" class="' + bouton.classe + '" data-statut="' + cible + '"'
            + (bloque ? ' disabled title="La séance n\'est pas encore terminée"' : '') + '>'
            + bouton.libelle + '</button>';
    }).join(' ');
    if (cibles.includes('TERMINEE') && !seanceFinie(seance)) {
        html += ' <span class="aide">« Terminer » sera possible une fois la séance passée.</span>';
    }
    return html + ' <a class="bouton-lien secondaire" href="update_seance.html?id='
        + encodeURIComponent(seance.id) + '">Modifier</a>';
}

// une séance facturée ne peut pas être supprimée (le serveur refuse aussi, 409)
function htmlBoutonSupprimer() {
    if (facturesParSeance.get(seance.id)) {
        return '';
    }
    return ' <button type="button" class="danger" data-action="supprimer-seance">Supprimer</button>';
}

async function supprimerSeance(bouton) {
    if (!window.confirm('Supprimer définitivement la séance n° ' + seance.id + ' ?')) {
        return;
    }
    bouton.disabled = true;
    try {
        await api.delete('/seances/' + encodeURIComponent(seance.id));
        window.location.href = 'seances.html';
    } catch (e) {
        // l'erreur est déjà affichée par api.js
        bouton.disabled = false;
    }
}

async function changerStatut(cible, bouton) {
    // terminer et annuler sont définitifs, on demande avant
    if (cible !== 'CONFIRMEE') {
        const action = cible === 'TERMINEE' ? 'terminer' : 'annuler';
        if (!window.confirm('Voulez-vous vraiment ' + action + ' cette séance ?\nCe changement est définitif.')) {
            return;
        }
    }
    bouton.disabled = true;
    try {
        const miseAJour = await api.patch('/seances/' + encodeURIComponent(seance.id) + '/statut', { statut: cible });
        seance.statut = miseAJour.statut;
        afficherSucces(MESSAGES[cible]);
        afficherFiche();
        if (cible === 'TERMINEE') {
            proposerFacture(seance);
        }
    } catch (e) {
        // l'erreur est déjà affichée par api.js
        bouton.disabled = false;
    }
}
