/* =====================================================================
   Studio photo - outils communs à toutes les pages.

   Appel de l'API (renvoie une promesse) :
       const photographes = await api.get('/photographes');
       await api.post('/clients', { type: 'PARTICULIER', nom: 'Bernard', ... });
       await api.patch('/seances/12/statut', { statut: 'CONFIRMEE' });
       await api.delete('/clients/3');

   En cas d'erreur (code HTTP 4xx/5xx ou serveur injoignable), le message
   est affiché automatiquement en haut de la page et la promesse est
   rejetée avec une ErreurApi : il suffit d'arrêter le traitement.

       try {
           await api.post('/seances', seance);
           afficherSucces('Séance créée.');
       } catch (e) {
           // le message d'erreur est déjà affiché
       }
   ===================================================================== */

const API_BASE = '/api';

/** Erreur renvoyée par l'API : contient le code HTTP et le message. */
class ErreurApi extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}

/**
 * Appel générique de l'API.
 * @param {string} chemin   chemin après /api, ex. '/seances?photographeId=2'
 * @param {object} options  { method, body } ; body est converti en JSON
 * @returns {Promise<any>}  le JSON de la réponse, ou null si la réponse est vide
 */
async function appelApi(chemin, options = {}) {
    const methode = options.method || 'GET';
    const entetes = { 'Accept': 'application/json' };
    let corps;
    if (options.body !== undefined) {
        entetes['Content-Type'] = 'application/json';
        corps = JSON.stringify(options.body);
    }

    let reponse;
    try {
        reponse = await fetch(API_BASE + chemin, { method: methode, headers: entetes, body: corps });
    } catch (e) {
        const message = 'Le serveur ne répond pas. Vérifiez que l\'application est démarrée.';
        afficherErreur(message);
        throw new ErreurApi(0, message);
    }

    if (!reponse.ok) {
        const message = await lireMessageErreur(reponse);
        afficherErreur(message);
        throw new ErreurApi(reponse.status, message);
    }

    if (reponse.status === 204) {
        return null;
    }
    const texte = await reponse.text();
    if (!texte) {
        return null;
    }
    const type = reponse.headers.get('Content-Type') || '';
    return type.includes('application/json') ? JSON.parse(texte) : texte;
}

/** Lit le message d'une réponse en erreur : format commun { status, erreur, message }. */
async function lireMessageErreur(reponse) {
    const parDefaut = {
        400: 'Données invalides.',
        404: 'Élément introuvable.',
        409: 'Action impossible dans l\'état actuel.',
        500: 'Erreur interne du serveur.'
    };
    try {
        const texte = await reponse.text();
        try {
            const json = JSON.parse(texte);
            if (json && json.message) {
                return json.message;
            }
        } catch (e) {
            if (texte && texte.length < 300) {
                return texte;
            }
        }
    } catch (e) {
        // corps illisible : on garde le message par défaut
    }
    return parDefaut[reponse.status] || ('Erreur ' + reponse.status + '.');
}

/** Raccourcis par méthode HTTP. */
const api = {
    get: (chemin) => appelApi(chemin),
    post: (chemin, donnees) => appelApi(chemin, { method: 'POST', body: donnees }),
    put: (chemin, donnees) => appelApi(chemin, { method: 'PUT', body: donnees }),
    patch: (chemin, donnees) => appelApi(chemin, { method: 'PATCH', body: donnees }),
    delete: (chemin) => appelApi(chemin, { method: 'DELETE' })
};

/* ---------------------------------------------------------------------
   Messages affichés en haut de la page (zone #messages)
   --------------------------------------------------------------------- */

function afficherErreur(message) {
    afficherMessage(message, 'erreur');
}

function afficherSucces(message) {
    afficherMessage(message, 'succes', 4000);
}

/** Affiche un message ; il disparaît après "duree" ms si une durée est donnée. */
function afficherMessage(message, type, duree) {
    let zone = document.getElementById('messages');
    if (!zone) {
        zone = document.createElement('div');
        zone.id = 'messages';
        (document.querySelector('main') || document.body).prepend(zone);
    }
    const bloc = document.createElement('div');
    bloc.className = 'message ' + type;
    bloc.setAttribute('role', type === 'erreur' ? 'alert' : 'status');

    const texte = document.createElement('span');
    texte.textContent = message;
    const fermer = document.createElement('button');
    fermer.type = 'button';
    fermer.setAttribute('aria-label', 'Fermer');
    fermer.textContent = '×';
    fermer.addEventListener('click', () => bloc.remove());

    bloc.append(texte, fermer);
    zone.append(bloc);
    if (duree) {
        setTimeout(() => bloc.remove(), duree);
    }
}

function effacerMessages() {
    const zone = document.getElementById('messages');
    if (zone) {
        zone.innerHTML = '';
    }
}

/* ---------------------------------------------------------------------
   Mise en forme des valeurs reçues de l'API
   --------------------------------------------------------------------- */

/** '2026-11-06' -> '06/11/2026' */
function formaterDate(dateIso) {
    if (!dateIso) return '';
    const [annee, mois, jour] = dateIso.substring(0, 10).split('-');
    return jour + '/' + mois + '/' + annee;
}

/** '14:30:00' -> '14h30' */
function formaterHeure(heureIso) {
    if (!heureIso) return '';
    const [h, m] = heureIso.split(':');
    return h + 'h' + m;
}

/** 90 -> '1 h 30' (durées en minutes) */
function formaterDuree(minutes) {
    const h = Math.floor(minutes / 60);
    const m = minutes % 60;
    if (h === 0) return m + ' min';
    return m === 0 ? h + ' h' : h + ' h ' + String(m).padStart(2, '0');
}

/** 1440 -> '1 440,00 €' */
function formaterMontant(montant) {
    if (montant === null || montant === undefined) return '';
    return Number(montant).toLocaleString('fr-FR', { style: 'currency', currency: 'EUR' });
}

/** Badge coloré pour un statut (PLANIFIEE, PAYEE, ACTIF...). Voir .badge dans style.css */
function badgeStatut(statut) {
    const libelles = {
        PLANIFIEE: 'Planifiée', CONFIRMEE: 'Confirmée', TERMINEE: 'Terminée', ANNULEE: 'Annulée',
        EN_ATTENTE: 'En attente', PAYEE: 'Payée', ACTIF: 'Actif', INACTIF: 'Inactif'
    };
    return '<span class="badge ' + echapperHtml(statut) + '">' + echapperHtml(libelles[statut] || statut) + '</span>';
}

/**
 * À utiliser pour toute donnée insérée avec innerHTML (noms, lieux, adresses...),
 * pour éviter qu'un texte saisi soit interprété comme du HTML.
 */
function echapperHtml(valeur) {
    if (valeur === null || valeur === undefined) return '';
    return String(valeur)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/** Lit un paramètre de l'URL de la page, ex. seance.html?id=12 -> lireParametre('id') = '12' */
function lireParametre(nom) {
    return new URLSearchParams(window.location.search).get(nom);
}
