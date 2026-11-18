/* =====================================================================
   Factures : liste (recherche + filtre par statut), détail, PDF et changement de statut.
   Les outils communs (api, afficherSucces, echapperHtml...) sont dans js/api.js.
   ===================================================================== */

// factures chargées depuis l'API (filtrées ensuite côté navigateur)
let factures = [];
// id de la facture dont le détail est ouvert (null si aucune)
let idOuverte = null;

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('recherche').addEventListener('input', afficherListe);
    document.getElementById('filtre-statut').addEventListener('change', afficherListe);

    // un seul listener pour tous les boutons (liste + détail)
    document.querySelector('main').addEventListener('click', e => {
        const bouton = e.target.closest('button[data-action]');
        if (!bouton) return;
        const id = Number(bouton.dataset.id);
        if (bouton.dataset.action === 'detail') ouvrirDetail(id);
        if (bouton.dataset.action === 'fermer') fermerDetail();
        if (bouton.dataset.action === 'statut') changerStatut(id, bouton.dataset.statut);
    });

    chargerFactures().then(() => {
        // factures.html?id=12 ouvre directement cette facture
        const id = lireParametre('id');
        if (id) ouvrirDetail(Number(id));
    });
});

// ---------- liste ----------

async function chargerFactures() {
    try {
        factures = await api.get('/factures');
    } catch (e) {
        factures = [];
    }
    afficherListe();
}

// minuscules + sans accents pour la recherche
function normaliser(texte) {
    return (texte || '').toLowerCase().normalize('NFD').replace(/[̀-ͯ]/g, '');
}

function afficherListe() {
    const recherche = normaliser(document.getElementById('recherche').value.trim());
    const statut = document.getElementById('filtre-statut').value;

    const affichees = factures.filter(f =>
        (!statut || f.statutPaiement === statut)
        && normaliser([f.numero, f.clientDesignation, f.clientEmail].join(' ')).includes(recherche));

    const corps = document.getElementById('liste-factures');
    if (affichees.length === 0) {
        const message = factures.length === 0 ? 'Aucune facture pour le moment.' : 'Aucune facture ne correspond.';
        corps.innerHTML = '<tr><td colspan="7" class="vide">' + message + '</td></tr>';
    } else {
        corps.innerHTML = affichees.map(ligne).join('');
    }
    document.getElementById('compteur').textContent = factures.length === 0 ? ''
        : affichees.length + ' facture' + (affichees.length > 1 ? 's' : '') + ' sur ' + factures.length;
}

function ligne(f) {
    return '<tr' + (f.id === idOuverte ? ' class="selectionnee"' : '') + '>'
        + '<td class="numero"><strong>' + echapperHtml(f.numero) + '</strong></td>'
        + '<td>' + formaterDate(f.dateEmission) + '</td>'
        + '<td>' + echapperHtml(f.clientDesignation) + '</td>'
        + '<td class="masquable">' + echapperHtml(f.prestationType) + ' · ' + formaterDate(f.prestationDate) + '</td>'
        + '<td class="montant">' + formaterMontant(f.montantTTC) + '</td>'
        + '<td>' + badgeStatut(f.statutPaiement) + '</td>'
        + '<td class="actions-ligne">'
        + '<button type="button" class="secondaire" data-action="detail" data-id="' + Number(f.id) + '">Détail</button> '
        + '<a class="bouton secondaire" href="' + lienPdf(f) + '">PDF</a>'
        + '</td></tr>';
}

function lienPdf(f) {
    return API_BASE + '/factures/' + Number(f.id) + '/pdf';
}

// ---------- détail d'une facture ----------

async function ouvrirDetail(id) {
    let facture = factures.find(f => f.id === id);
    if (!facture) {
        // pas dans la liste (liste pas à jour ?) : on la demande à l'API
        try {
            facture = await api.get('/factures/' + id);
        } catch (e) {
            return;
        }
    }
    idOuverte = id;
    const zone = document.getElementById('detail');
    zone.innerHTML = htmlDetail(facture);
    zone.hidden = false;
    zone.scrollIntoView({ behavior: 'smooth', block: 'start' });
    history.replaceState(null, '', 'factures.html?id=' + id);
    afficherListe();
}

function fermerDetail() {
    idOuverte = null;
    document.getElementById('detail').hidden = true;
    history.replaceState(null, '', 'factures.html');
    afficherListe();
}

function htmlDetail(f) {
    const tva = Math.round((f.montantTTC - f.montantHT) * 100) / 100;
    const entreprise = f.clientSirenTva
        ? '<p>SIREN/TVA : ' + echapperHtml(f.clientSirenTva) + '</p>' : '';

    // seule une facture EN_ATTENTE peut encore changer de statut
    const actionsStatut = f.statutPaiement === 'EN_ATTENTE'
        ? '<button type="button" data-action="statut" data-id="' + Number(f.id) + '" data-statut="PAYEE">Marquer comme payée</button> '
        + '<button type="button" class="danger" data-action="statut" data-id="' + Number(f.id) + '" data-statut="ANNULEE">Annuler la facture</button> '
        : '';

    return '<div class="detail-tete"><h2>Facture ' + echapperHtml(f.numero) + '</h2>'
        + '<span>' + badgeStatut(f.statutPaiement) + '</span></div>'
        + '<p>Émise le ' + formaterDate(f.dateEmission) + '</p>'
        + '<div class="deux-colonnes">'
        + '<div><h3>Client</h3>'
        + '<p><strong>' + echapperHtml(f.clientDesignation) + '</strong></p>'
        + '<p>' + echapperHtml(f.clientAdresse) + '</p>'
        + '<p>' + echapperHtml(f.clientEmail) + '</p>' + entreprise + '</div>'
        + '<div><h3>Prestation</h3>'
        + '<p><strong>' + echapperHtml(f.prestationType) + '</strong></p>'
        + '<p>Le ' + formaterDate(f.prestationDate) + ' à ' + formaterHeure(f.prestationHeure) + '</p>'
        + '<p>Durée : ' + formaterDuree(f.prestationDuree) + '</p>'
        + '<p>Photographe : ' + echapperHtml(f.photographeNom) + '</p></div>'
        + '</div>'
        + '<table class="montants">'
        + '<tr><td>Total HT</td><td class="montant">' + formaterMontant(f.montantHT) + '</td></tr>'
        + '<tr><td>TVA ' + Number(f.tauxTVA) + ' %</td><td class="montant">' + formaterMontant(tva) + '</td></tr>'
        + '<tr class="total"><td>Total TTC</td><td class="montant">' + formaterMontant(f.montantTTC) + '</td></tr>'
        + '</table>'
        + '<div class="actions">' + actionsStatut
        + '<a class="bouton secondaire" href="' + lienPdf(f) + '">Télécharger le PDF</a> '
        + '<button type="button" class="secondaire" data-action="fermer">Fermer</button></div>';
}

// ---------- statut de paiement ----------

async function changerStatut(id, statut) {
    const facture = factures.find(f => f.id === id);
    if (!facture) return;
    // on confirme car c'est définitif (payer aussi : une facture payée ne ce rouvre pas)
    const action = statut === 'PAYEE' ? 'marquer comme payée' : 'annuler';
    if (!window.confirm('Voulez-vous vraiment ' + action + ' la facture ' + facture.numero
        + ' ?\nCe changement est définitif.')) {
        return;
    }
    try {
        const miseAJour = await api.patch('/factures/' + id + '/statut', { statut: statut });
        Object.assign(facture, miseAJour);
        afficherSucces('Facture ' + facture.numero + ' : ' + (statut === 'PAYEE' ? 'payée.' : 'annulée.'));
        ouvrirDetail(id);
    } catch (e) {
        // l'erreur est déjà affichée par api.js
    }
}
