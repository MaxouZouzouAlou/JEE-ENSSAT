/* =====================================================================
   Accueil : choix du profil, puis cartes des rubriques permises.
   Les profils et leurs droits sont définis dans js/layout.js.
   ===================================================================== */

document.addEventListener('DOMContentLoaded', () => {
    const profil = lireProfil();
    if (profil) {
        afficherRubriques(profil);
    } else {
        afficherChoixProfil();
    }
});

/** Cartes des seules rubriques du profil. */
function afficherRubriques(profil) {
    document.querySelectorAll('#rubriques a.carte').forEach(carte => {
        carte.hidden = !pagePermise(profil, carte.getAttribute('href'));
    });
    document.getElementById('rubriques').hidden = false;
}

function afficherChoixProfil() {
    const formulaire = document.getElementById('choix-profil');
    const role = document.getElementById('role');
    formulaire.hidden = false;

    role.addEventListener('change', () => {
        const photographe = role.value === 'PHOTOGRAPHE';
        document.getElementById('bloc-photographe').hidden = !photographe;
        document.getElementById('photographe-profil').required = photographe;
        if (photographe) chargerPhotographes();
    });

    formulaire.addEventListener('submit', evenement => {
        evenement.preventDefault();
        if (!role.value) return;
        const profil = { role: role.value };
        if (role.value === 'PHOTOGRAPHE') {
            const choix = document.getElementById('photographe-profil');
            if (!choix.value) return;
            profil.photographeId = Number(choix.value);
            profil.nom = choix.options[choix.selectedIndex].textContent;
        }
        enregistrerProfil(profil);
        window.location.href = PROFILS[profil.role].accueil;
    });
}

let photographesCharges = false;

/** Liste des photographes actifs, chargée une seule fois. */
async function chargerPhotographes() {
    if (photographesCharges) return;
    const select = document.getElementById('photographe-profil');
    try {
        const photographes = await api.get('/photographes?actif=true');
        photographesCharges = true;
        select.innerHTML = '<option value="">Choisir votre nom…</option>'
            + photographes.map(p => '<option value="' + echapperHtml(p.id) + '">' + echapperHtml(p.nom) + '</option>').join('');
    } catch (e) {
        // message déjà affiché par api.js
        select.innerHTML = '<option value="">Liste indisponible</option>';
    }
}
