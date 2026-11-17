/* =====================================================================
   Photographes - Lot 1
   Script propre à photographes.html. Les outils communs (api, afficherErreur,
   afficherSucces, badgeStatut, echapperHtml...) sont dans js/api.js.

   API utilisée :
     GET    /api/specialites
     GET    /api/photographes[?actif=true|false]
     POST   /api/photographes                  { nom, email, specialites: [{id}] }
     PUT    /api/photographes/{id}             idem
     PATCH  /api/photographes/{id}/statut[?confirmer=true]   { actif }
     DELETE /api/photographes/{id}
   ===================================================================== */

/** Spécialités disponibles (liste fixe chargée une fois). */
let specialites = [];
/** Photographes affichés, pour retrouver une ligne à partir de son id. */
let photographes = [];
/** Identifiant du photographe en cours de modification, ou null pour une création. */
let idEnEdition = null;

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('filtre-statut').addEventListener('change', chargerPhotographes);
    document.getElementById('bouton-nouveau').addEventListener('click', () => ouvrirFormulaire(null));
    document.getElementById('bouton-annuler').addEventListener('click', fermerFormulaire);
    document.getElementById('formulaire').addEventListener('submit', enregistrer);
    document.getElementById('liste').addEventListener('click', actionSurLigne);

    chargerSpecialites();
    chargerPhotographes();
});

/* ---------------------------------------------------------------------
   Chargement et affichage
   --------------------------------------------------------------------- */

async function chargerSpecialites() {
    try {
        specialites = await api.get('/specialites');
    } catch (e) {
        specialites = [];
    }
    const zone = document.getElementById('cases-specialites');
    if (specialites.length === 0) {
        zone.innerHTML = '<span class="texte-doux">Aucune spécialité disponible.</span>';
        return;
    }
    zone.innerHTML = specialites.map(s =>
        '<label><input type="checkbox" name="specialite" value="' + s.id + '"> '
        + echapperHtml(s.libelle) + '</label>'
    ).join('');
}

async function chargerPhotographes() {
    const filtre = document.getElementById('filtre-statut').value;
    const chemin = filtre ? '/photographes?actif=' + filtre : '/photographes';
    try {
        photographes = await api.get(chemin);
    } catch (e) {
        photographes = [];
    }
    afficherListe();
}

function afficherListe() {
    const corps = document.getElementById('liste');
    if (photographes.length === 0) {
        corps.innerHTML = '<tr><td colspan="5" class="texte-doux">Aucun photographe.</td></tr>';
        return;
    }
    corps.innerHTML = photographes.map(p => {
        const libelles = p.specialites.map(s => echapperHtml(s.libelle)).join(', ');
        const boutonStatut = p.actif
            ? '<button type="button" class="secondaire" data-action="desactiver" data-id="' + p.id + '">Désactiver</button>'
            : '<button type="button" class="secondaire" data-action="activer" data-id="' + p.id + '">Activer</button>';
        return '<tr>'
            + '<td>' + echapperHtml(p.nom) + '</td>'
            + '<td>' + echapperHtml(p.email) + '</td>'
            + '<td>' + (libelles || '<span class="texte-doux">—</span>') + '</td>'
            + '<td>' + badgeStatut(p.actif ? 'ACTIF' : 'INACTIF') + '</td>'
            + '<td class="actions-ligne">'
            + '<button type="button" class="secondaire" data-action="modifier" data-id="' + p.id + '">Modifier</button>'
            + boutonStatut
            + '<button type="button" class="danger" data-action="supprimer" data-id="' + p.id + '">Supprimer</button>'
            + '</td>'
            + '</tr>';
    }).join('');
}

/** Clic sur un bouton d'une ligne du tableau. */
function actionSurLigne(evenement) {
    const bouton = evenement.target.closest('button[data-action]');
    if (!bouton) return;
    const photographe = photographes.find(p => p.id === Number(bouton.dataset.id));
    if (!photographe) return;

    switch (bouton.dataset.action) {
        case 'modifier': ouvrirFormulaire(photographe); break;
        case 'activer': activer(photographe); break;
        case 'desactiver': desactiver(photographe); break;
        case 'supprimer': supprimer(photographe); break;
    }
}

/* ---------------------------------------------------------------------
   Formulaire de création / modification
   --------------------------------------------------------------------- */

function ouvrirFormulaire(photographe) {
    effacerMessages();
    idEnEdition = photographe ? photographe.id : null;
    const choisies = photographe ? photographe.specialites.map(s => s.id) : [];

    document.getElementById('titre-formulaire').textContent =
        photographe ? 'Modifier ' + photographe.nom : 'Nouveau photographe';
    document.getElementById('nom').value = photographe ? photographe.nom : '';
    document.getElementById('email').value = photographe ? photographe.email : '';
    document.querySelectorAll('input[name="specialite"]').forEach(caseACocher => {
        caseACocher.checked = choisies.includes(Number(caseACocher.value));
    });

    const formulaire = document.getElementById('formulaire');
    formulaire.hidden = false;
    formulaire.scrollIntoView({ behavior: 'smooth', block: 'start' });
    document.getElementById('nom').focus();
}

function fermerFormulaire() {
    document.getElementById('formulaire').hidden = true;
    idEnEdition = null;
}

async function enregistrer(evenement) {
    evenement.preventDefault();
    effacerMessages();

    const donnees = {
        nom: document.getElementById('nom').value.trim(),
        email: document.getElementById('email').value.trim(),
        specialites: Array.from(document.querySelectorAll('input[name="specialite"]:checked'))
            .map(caseACocher => ({ id: Number(caseACocher.value) }))
    };

    const bouton = document.getElementById('bouton-enregistrer');
    bouton.disabled = true;
    try {
        if (idEnEdition === null) {
            const cree = await api.post('/photographes', donnees);
            afficherSucces(cree.nom + ' a été ajouté.');
        } else {
            const modifie = await api.put('/photographes/' + idEnEdition, donnees);
            afficherSucces(modifie.nom + ' a été modifié.');
        }
        fermerFormulaire();
        await chargerPhotographes();
    } catch (e) {
        // message d'erreur déjà affiché par api.js ; le formulaire reste ouvert pour corriger
    } finally {
        bouton.disabled = false;
    }
}

/* ---------------------------------------------------------------------
   Activation, désactivation, suppression
   --------------------------------------------------------------------- */

async function activer(photographe) {
    const ok = await demanderConfirmation('Réactiver ' + photographe.nom + ' ?',
        'Il pourra de nouveau être affecté à des séances.', 'Réactiver', false);
    if (!ok) return;
    effacerMessages();
    try {
        await api.patch('/photographes/' + photographe.id + '/statut', { actif: true });
        afficherSucces(photographe.nom + ' est de nouveau actif.');
        await chargerPhotographes();
    } catch (e) {
        // message déjà affiché
    }
}

/**
 * Désactivation en deux temps : si le photographe a des séances à venir, l'API répond 409
 * avec leur nombre ; on demande alors une seconde confirmation avant de renvoyer la
 * requête avec ?confirmer=true (les séances à venir sont annulées).
 * @param {boolean} dejaConfirme true si l'utilisateur a déjà accepté la désactivation
 *                               (proposée après un refus de suppression)
 */
async function desactiver(photographe, dejaConfirme = false) {
    if (!dejaConfirme) {
        const ok = await demanderConfirmation('Désactiver ' + photographe.nom + ' ?',
            'Il ne pourra plus être affecté à de nouvelles séances. Son historique est conservé.',
            'Désactiver', true);
        if (!ok) return;
    }
    effacerMessages();

    let reponse = await envoyerDesactivation(photographe.id, false);
    if (reponse && reponse.nombreSeances) {
        const n = reponse.nombreSeances;
        const texte = photographe.nom + ' a ' + n + (n > 1
            ? ' séances à venir. Elles seront annulées.'
            : ' séance à venir. Elle sera annulée.');
        const confirme = await demanderConfirmation('Séances à venir', texte,
            'Désactiver et annuler ' + (n > 1 ? 'les séances' : 'la séance'), true);
        if (!confirme) return;
        reponse = await envoyerDesactivation(photographe.id, true);
    }
    if (!reponse || !reponse.resultat) return;

    const annulees = reponse.resultat.seancesAnnulees;
    afficherSucces(photographe.nom + ' est désactivé'
        + (annulees > 0 ? ' ; ' + annulees + ' séance(s) à venir annulée(s).' : '.'));
    await chargerPhotographes();
}

/**
 * PATCH .../statut { actif: false }. Appel direct à fetch (et non api.patch) car un 409 avec
 * "nombreSeances" n'est pas une erreur à afficher : c'est une demande de confirmation.
 * @returns {Promise<{resultat}|{nombreSeances}|null>} null si une erreur a été affichée
 */
async function envoyerDesactivation(id, confirmer) {
    let reponse;
    try {
        reponse = await fetch(API_BASE + '/photographes/' + id + '/statut' + (confirmer ? '?confirmer=true' : ''), {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: JSON.stringify({ actif: false })
        });
    } catch (e) {
        afficherErreur('Le serveur ne répond pas. Vérifiez que l\'application est démarrée.');
        return null;
    }

    if (reponse.ok) {
        return { resultat: await reponse.json() };
    }
    if (reponse.status === 409 && !confirmer) {
        const corps = await reponse.clone().json().catch(() => null);
        if (corps && typeof corps.nombreSeances === 'number') {
            return { nombreSeances: corps.nombreSeances };
        }
    }
    afficherErreur(await lireMessageErreur(reponse));
    return null;
}

async function supprimer(photographe) {
    const ok = await demanderConfirmation('Supprimer ' + photographe.nom + ' ?',
        'La suppression est définitive. Elle est refusée si le photographe a déjà des séances : '
        + 'dans ce cas, désactivez-le.', 'Supprimer', true);
    if (!ok) return;
    effacerMessages();
    try {
        await api.delete('/photographes/' + photographe.id);
        afficherSucces(photographe.nom + ' a été supprimé.');
        await chargerPhotographes();
    } catch (e) {
        // 409 : le photographe a des séances. Le message est déjà affiché ; on propose la désactivation.
        if (e.status === 409 && photographe.actif) {
            const desactiverPlutot = await demanderConfirmation('Suppression impossible',
                e.message + ' Voulez-vous le désactiver à la place ?', 'Désactiver', true);
            if (desactiverPlutot) {
                effacerMessages();
                await desactiver(photographe, true);
            }
        }
    }
}

/* ---------------------------------------------------------------------
   Boîte de confirmation
   --------------------------------------------------------------------- */

/**
 * Affiche la boîte de confirmation et attend la réponse.
 * @returns {Promise<boolean>} true si l'utilisateur a cliqué sur le bouton de confirmation
 */
function demanderConfirmation(titre, texte, libelleOk, dangereux) {
    const boite = document.getElementById('confirmation');
    if (typeof boite.showModal !== 'function') {
        return Promise.resolve(window.confirm(titre + '\n\n' + texte));
    }
    document.getElementById('confirmation-titre').textContent = titre;
    document.getElementById('confirmation-texte').textContent = texte;
    const boutonOk = document.getElementById('confirmation-ok');
    boutonOk.textContent = libelleOk || 'Confirmer';
    boutonOk.className = dangereux ? 'danger' : '';

    return new Promise(resoudre => {
        boite.returnValue = '';
        boite.addEventListener('close', () => resoudre(boite.returnValue === 'ok'), { once: true });
        boite.showModal();
    });
}
