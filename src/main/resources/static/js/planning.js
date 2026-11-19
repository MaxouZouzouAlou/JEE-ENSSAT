/* =====================================================================
   Planning - Lot 2
   Vue des séances par jour ou par semaine, avec filtre par photographe.
   L'état (vue, date, photographe) est gardé dans l'URL : la page peut
   être rechargée ou partagée, ex. planning.html?vue=jour&date=2026-11-18&photographe=2
   Les outils communs (api, badgeStatut, echapperHtml...) sont dans js/api.js.
   ===================================================================== */

/**
 * Route de liste des séances. Le contrat de la phase 0 (openapi.yaml) prévoit
 * GET /seances?debut=&fin=&photographeId= ; la route en place sur main est
 * GET /seance?dateDebut=&dateFin=&photographeId=. Seule cette fonction est à
 * adapter si la route change.
 */
function urlSeances(debut, fin, photographeId) {
    const parametres = new URLSearchParams({ dateDebut: debut, dateFin: fin });
    if (photographeId) parametres.set('photographeId', photographeId);
    return '/seances?' + parametres;
}

const JOURS = ['dimanche', 'lundi', 'mardi', 'mercredi', 'jeudi', 'vendredi', 'samedi'];
const MOIS = ['janvier', 'février', 'mars', 'avril', 'mai', 'juin', 'juillet',
    'août', 'septembre', 'octobre', 'novembre', 'décembre'];

const etat = {
    vue: 'semaine',      // 'jour' ou 'semaine'
    date: new Date(),    // jour affiché, ou un jour de la semaine affichée
    photographe: ''      // identifiant, '' = tous
};
/** Numéro de la dernière requête : ignore les réponses arrivées dans le désordre. */
let requeteCourante = 0;

document.addEventListener('DOMContentLoaded', () => {
    lireUrl();

    document.querySelectorAll('[data-vue]').forEach(bouton =>
        bouton.addEventListener('click', () => changer({ vue: bouton.dataset.vue })));
    document.getElementById('precedent').addEventListener('click', () => decaler(-1));
    document.getElementById('suivant').addEventListener('click', () => decaler(1));
    document.getElementById('aujourdhui').addEventListener('click', () => changer({ date: new Date() }));
    document.getElementById('date').addEventListener('change', e => {
        if (e.target.value) changer({ date: depuisIso(e.target.value) });
    });
    const filtre = document.getElementById('filtre-photographe');
    filtre.addEventListener('change', e => changer({ photographe: e.target.value }));
    const profil = lireProfil();
    if (profil && profil.role === 'PHOTOGRAPHE') {
        filtre.disabled = true;   // filtre figé sur le photographe du profil
        document.querySelector('h1').textContent = 'Mon planning';
        const creation = document.querySelector('a[href="create_seance.html"]');
        if (creation) creation.hidden = true;   // la création de séance est réservée au secrétariat
    }
    // Clic sur l'en-tête d'un jour de la semaine -> vue de ce jour
    document.getElementById('calendrier').addEventListener('click', e => {
        const bouton = e.target.closest('button[data-jour]');
        if (bouton) changer({ vue: 'jour', date: depuisIso(bouton.dataset.jour) });
    });
    window.addEventListener('popstate', () => { lireUrl(); afficher(); });

    chargerPhotographes();
    afficher();
});

/* ---------------------------------------------------------------------
   État et URL
   --------------------------------------------------------------------- */

function lireUrl() {
    const vue = lireParametre('vue');
    const date = lireParametre('date');
    etat.vue = vue === 'jour' ? 'jour' : 'semaine';
    etat.date = /^\d{4}-\d{2}-\d{2}$/.test(date || '') ? depuisIso(date) : new Date();
    etat.photographe = lireParametre('photographe') || '';
    // un photographe ne voit que son propre planning, quel que soit le paramètre de l'URL
    const profil = lireProfil();
    if (profil && profil.role === 'PHOTOGRAPHE') {
        etat.photographe = String(profil.photographeId);
    }
}

function changer(modifications) {
    Object.assign(etat, modifications);
    const parametres = new URLSearchParams({ vue: etat.vue, date: versIso(etat.date) });
    if (etat.photographe) parametres.set('photographe', etat.photographe);
    history.pushState(null, '', '?' + parametres);
    afficher();
}

/** Recule ou avance d'un jour ou d'une semaine selon la vue. */
function decaler(sens) {
    changer({ date: ajouterJours(etat.date, sens * (etat.vue === 'jour' ? 1 : 7)) });
}

/* ---------------------------------------------------------------------
   Dates (toujours en heure locale, au format AAAA-MM-JJ pour l'API)
   --------------------------------------------------------------------- */

function versIso(date) {
    return date.getFullYear() + '-' + String(date.getMonth() + 1).padStart(2, '0')
        + '-' + String(date.getDate()).padStart(2, '0');
}

function depuisIso(texte) {
    const [annee, mois, jour] = texte.split('-').map(Number);
    return new Date(annee, mois - 1, jour);
}

function ajouterJours(date, nombre) {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate() + nombre);
}

/** Lundi de la semaine contenant cette date. */
function lundi(date) {
    const decalage = (date.getDay() + 6) % 7; // lundi = 0 ... dimanche = 6
    return ajouterJours(date, -decalage);
}

/** Premier et dernier jour affichés. */
function periodeAffichee() {
    if (etat.vue === 'jour') {
        return { debut: etat.date, fin: etat.date };
    }
    const debut = lundi(etat.date);
    return { debut, fin: ajouterJours(debut, 6) };
}

function titrePeriode({ debut, fin }) {
    if (etat.vue === 'jour') {
        return capitaliser(JOURS[debut.getDay()]) + ' ' + debut.getDate() + ' ' + MOIS[debut.getMonth()] + ' ' + debut.getFullYear();
    }
    const memeMois = debut.getMonth() === fin.getMonth();
    const memeAnnee = debut.getFullYear() === fin.getFullYear();
    return 'Semaine du ' + debut.getDate()
        + (memeMois ? '' : ' ' + MOIS[debut.getMonth()])
        + (memeAnnee ? '' : ' ' + debut.getFullYear())
        + ' au ' + fin.getDate() + ' ' + MOIS[fin.getMonth()] + ' ' + fin.getFullYear();
}

function capitaliser(texte) {
    return texte.charAt(0).toUpperCase() + texte.slice(1);
}

/** '14:00:00' + 90 min -> '14h00 – 15h30' */
function plageHoraire(seance) {
    const [h, m] = seance.heureDebut.split(':').map(Number);
    const fin = h * 60 + m + seance.duree;
    const finTexte = String(Math.floor(fin / 60) % 24).padStart(2, '0') + 'h' + String(fin % 60).padStart(2, '0');
    return formaterHeure(seance.heureDebut) + ' – ' + finTexte;
}

/* ---------------------------------------------------------------------
   Photographes (liste du filtre)
   --------------------------------------------------------------------- */

async function chargerPhotographes() {
    let photographes = [];
    try {
        // Les options du formulaire de séance contiennent les photographes actifs
        const options = await api.get('/seances/options');
        photographes = options.photographes || [];
    } catch (e) {
        // le planning reste utilisable sans le filtre
    }
    ajouterPhotographes(photographes);
}

/**
 * Ajoute au filtre les photographes absents (ex. un photographe inactif qui a
 * des séances passées), en gardant la liste triée par nom.
 */
function ajouterPhotographes(photographes) {
    const select = document.getElementById('filtre-photographe');
    const connus = new Map([...select.options].filter(o => o.value).map(o => [o.value, o.textContent]));
    photographes.forEach(p => { if (p && p.id != null) connus.set(String(p.id), p.nom); });

    const tries = [...connus.entries()].sort((a, b) => a[1].localeCompare(b[1], 'fr'));
    select.innerHTML = '<option value="">Tous les photographes</option>'
        + tries.map(([id, nom]) => '<option value="' + echapperHtml(id) + '">' + echapperHtml(nom) + '</option>').join('');
    select.value = etat.photographe;
}

/* ---------------------------------------------------------------------
   Affichage
   --------------------------------------------------------------------- */

async function afficher() {
    document.querySelectorAll('[data-vue]').forEach(b =>
        b.setAttribute('aria-pressed', String(b.dataset.vue === etat.vue)));
    document.getElementById('date').value = versIso(etat.date);
    document.getElementById('filtre-photographe').value = etat.photographe;
    document.getElementById('precedent').setAttribute('aria-label', etat.vue === 'jour' ? 'Jour précédent' : 'Semaine précédente');
    document.getElementById('suivant').setAttribute('aria-label', etat.vue === 'jour' ? 'Jour suivant' : 'Semaine suivante');

    const periode = periodeAffichee();
    document.getElementById('periode').textContent = titrePeriode(periode);

    const numero = ++requeteCourante;
    const calendrier = document.getElementById('calendrier');
    // Le contenu affiché est celui de l'ancienne période tant que la réponse n'est pas arrivée
    calendrier.setAttribute('aria-busy', 'true');
    let seances;
    try {
        seances = await api.get(urlSeances(versIso(periode.debut), versIso(periode.fin), etat.photographe));
    } catch (e) {
        seances = [];
    }
    if (numero !== requeteCourante) return; // une navigation plus récente a eu lieu

    ajouterPhotographes(seances.map(s => s.photographe));
    seances.sort((a, b) => (a.date + a.heureDebut).localeCompare(b.date + b.heureDebut));

    calendrier.removeAttribute('aria-busy');
    calendrier.className = etat.vue === 'jour' ? 'vue-jour' : 'vue-semaine';
    calendrier.innerHTML = etat.vue === 'jour'
        ? colonneJour(periode.debut, seances, false)
        : '<div class="semaine">' + [0, 1, 2, 3, 4, 5, 6]
            .map(i => ajouterJours(periode.debut, i))
            .map(jour => colonneJour(jour, seances.filter(s => s.date === versIso(jour)), true))
            .join('') + '</div>';
}

function colonneJour(jour, seances, avecEntete) {
    const estAujourdhui = versIso(jour) === versIso(new Date());
    const entete = avecEntete
        ? '<div class="jour-entete">'
            + '<button type="button" data-jour="' + versIso(jour) + '" title="Voir cette journée">'
            + JOURS[jour.getDay()] + ' ' + jour.getDate() + '</button>'
            + '</div>'
        : '';
    const contenu = seances.length
        ? seances.map(carteSeance).join('')
        : '<p class="jour-vide">' + (avecEntete ? '—' : 'Aucune séance ce jour-là.') + '</p>';
    return '<section class="jour' + (estAujourdhui ? ' aujourdhui' : '') + '"'
        + (avecEntete ? ' aria-label="' + JOURS[jour.getDay()] + ' ' + jour.getDate() + ' ' + MOIS[jour.getMonth()] + '"' : '') + '>'
        + entete + '<div class="jour-contenu">' + contenu + '</div></section>';
}

function designationClient(client) {
    if (!client) return '';
    return client.type === 'ENTREPRISE' ? client.raisonSociale : client.prenom + ' ' + client.nom;
}

function carteSeance(seance) {
    const type = seance.typeSeance ? seance.typeSeance.libelle : '';
    const photographe = seance.photographe ? seance.photographe.nom : '';
    const titre = [type, designationClient(seance.client), photographe, seance.lieu].filter(Boolean).join(' · ');
    // un photographe consulte son planning sans pouvoir modifier : la séance n'est pas un lien
    const lectureSeule = estPhotographe();
    const ouverture = lectureSeule
        ? '<div class="seance ' + echapperHtml(seance.statut) + '"'
        : '<a class="seance ' + echapperHtml(seance.statut) + '" href="update_seance.html?id=' + encodeURIComponent(seance.id) + '"';
    return ouverture
        + ' title="' + echapperHtml(titre) + '">'
        + '<div class="horaire">' + plageHoraire(seance) + '</div>'
        + '<div class="statut">' + badgeStatut(seance.statut) + '</div>'
        + '<div class="infos">'
        + '<div><strong>' + echapperHtml(type) + '</strong> – ' + echapperHtml(designationClient(seance.client)) + '</div>'
        + '<div class="detail">Photographe : ' + echapperHtml(photographe) + '</div>'
        + '<div class="detail">Lieu : ' + echapperHtml(seance.lieu) + '</div>'
        + '</div>'
        + (lectureSeule ? '</div>' : '</a>');
}

function estPhotographe() {
    const profil = lireProfil();
    return Boolean(profil && profil.role === 'PHOTOGRAPHE');
}
