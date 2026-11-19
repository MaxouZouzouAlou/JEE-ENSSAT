/* =====================================================================
   Studio photo - en-tête, menu communs et profil de l'utilisateur.

   Sur la page d'accueil, l'utilisateur indique ce qu'il est (gestionnaire,
   secrétaire ou photographe) ; seules les pages de son profil sont ensuite
   affichées dans le menu et accessibles. Ce n'est pas une authentification :
   le profil est un simple choix gardé dans le navigateur (localStorage).

   Profils, d'après le diagramme de cas d'utilisation :
     - Gestionnaire : toutes les rubriques ;
     - Secrétaire : clients, séances, planning, factures ;
     - Photographe : son planning uniquement.

   Chaque page contient <header id="entete"></header> : ce script le
   remplit et met en évidence la rubrique courante.
   Pour ajouter une rubrique, compléter RUBRIQUES et les PROFILS concernés.
   ===================================================================== */

const RUBRIQUES = [
    { page: 'photographes.html', libelle: 'Photographes' },
    { page: 'clients.html', libelle: 'Clients' },
    { page: 'seances.html', libelle: 'Séances' },
    { page: 'planning.html', libelle: 'Planning' },
    { page: 'factures.html', libelle: 'Factures' },
    { page: 'statistiques.html', libelle: 'Statistiques' }
];

/** Rubriques permises par profil, et page où l'on arrive après avoir choisi son profil. */
const PROFILS = {
    GESTIONNAIRE: {
        libelle: 'Gestionnaire',
        accueil: 'index.html',
        pages: ['photographes.html', 'clients.html', 'seances.html', 'planning.html', 'factures.html', 'statistiques.html']
    },
    SECRETAIRE: {
        libelle: 'Secrétaire',
        accueil: 'index.html',
        pages: ['clients.html', 'seances.html', 'planning.html', 'factures.html']
    },
    PHOTOGRAPHE: {
        libelle: 'Photographe',
        accueil: 'planning.html',
        pages: ['planning.html']
    }
};

/** Pages rattachées à une rubrique du menu (elles suivent ses droits). */
const RUBRIQUE_DE_PAGE = {
    'seance.html': 'seances.html',
    'create_seance.html': 'seances.html',
    'update_seance.html': 'seances.html'
};

const CLE_PROFIL = 'studiophoto.profil';

/* ---------------------------------------------------------------------
   Profil (gardé dans le navigateur)
   --------------------------------------------------------------------- */

/**
 * Profil choisi, ou null : { role: 'PHOTOGRAPHE', photographeId: 3, nom: 'Chloé Dubois' }.
 * photographeId et nom ne servent qu'au profil photographe.
 */
function lireProfil() {
    try {
        const profil = JSON.parse(window.localStorage.getItem(CLE_PROFIL));
        if (!profil || !PROFILS[profil.role]) return null;
        if (profil.role === 'PHOTOGRAPHE' && !profil.photographeId) return null;
        return profil;
    } catch (e) {
        return null; // stockage indisponible ou contenu illisible
    }
}

function enregistrerProfil(profil) {
    try {
        window.localStorage.setItem(CLE_PROFIL, JSON.stringify(profil));
    } catch (e) {
        // stockage indisponible (navigation privée stricte) : le profil ne sera pas retenu
    }
}

function effacerProfil() {
    try {
        window.localStorage.removeItem(CLE_PROFIL);
    } catch (e) {
        // rien à effacer
    }
}

/** La page est-elle permise pour ce profil ? (l'accueil l'est pour tous) */
function pagePermise(profil, page) {
    if (page === 'index.html') return true;
    const rubrique = RUBRIQUE_DE_PAGE[page] || page;
    return PROFILS[profil.role].pages.includes(rubrique);
}

function pageCourante() {
    return window.location.pathname.split('/').pop() || 'index.html';
}

/* ---------------------------------------------------------------------
   Contrôle d'accès, fait dès le chargement du script (avant l'affichage)
   --------------------------------------------------------------------- */

(function controlerAcces() {
    const page = pageCourante();
    const profil = lireProfil();
    if (!profil) {
        // sans profil, on commence par le choisir sur l'accueil
        if (page !== 'index.html') window.location.replace('index.html');
        return;
    }
    // le photographe n'a que son planning : il y va directement
    const destination = PROFILS[profil.role].accueil;
    if (page === 'index.html' ? destination !== 'index.html' : !pagePermise(profil, page)) {
        window.location.replace(destination);
    }
})();

/* ---------------------------------------------------------------------
   En-tête et menu
   --------------------------------------------------------------------- */

document.addEventListener('DOMContentLoaded', () => {
    const entete = document.getElementById('entete');
    if (!entete) return;

    const profil = lireProfil();
    const rubriqueActive = document.body.dataset.rubrique || pageCourante();
    const rubriques = profil ? RUBRIQUES.filter(r => PROFILS[profil.role].pages.includes(r.page)) : [];

    const liens = rubriques.map(r => {
        const actif = r.page === rubriqueActive ? ' class="actif" aria-current="page"' : '';
        return '<a href="' + r.page + '"' + actif + '>' + r.libelle + '</a>';
    }).join('');

    let blocProfil = '';
    if (profil) {
        const qui = PROFILS[profil.role].libelle + (profil.nom ? ' : ' + profil.nom : '');
        blocProfil = '<div class="profil"><span>' + echapperHtml(qui) + '</span>'
            + '<button type="button" class="secondaire" id="changer-profil">Changer de profil</button></div>';
    }

    entete.innerHTML =
        '<a href="' + (profil ? PROFILS[profil.role].accueil : 'index.html') + '"><strong>Studio photo</strong></a>'
        + '<nav aria-label="Rubriques">' + liens + '</nav>'
        + blocProfil;

    const bouton = document.getElementById('changer-profil');
    if (bouton) {
        bouton.addEventListener('click', () => {
            effacerProfil();
            window.location.href = 'index.html';
        });
    }
});
