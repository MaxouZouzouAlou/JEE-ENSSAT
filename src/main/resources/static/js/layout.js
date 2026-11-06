/* =====================================================================
   Studio photo - en-tête et menu communs.
   Chaque page contient <header id="entete"></header> : ce script le
   remplit et met en évidence la rubrique courante.
   Pour ajouter une rubrique au menu, il suffit de compléter RUBRIQUES.
   ===================================================================== */

const RUBRIQUES = [
    { page: 'photographes.html', libelle: 'Photographes' },
    { page: 'clients.html', libelle: 'Clients' },
    { page: 'seances.html', libelle: 'Séances' },
    { page: 'factures.html', libelle: 'Factures' },
    { page: 'statistiques.html', libelle: 'Statistiques' }
];

document.addEventListener('DOMContentLoaded', () => {
    const entete = document.getElementById('entete');
    if (!entete) return;

    const pageCourante = window.location.pathname.split('/').pop() || 'index.html';
    const rubriqueActive = document.body.dataset.rubrique || pageCourante;

    const liens = RUBRIQUES.map(r => {
        const actif = r.page === rubriqueActive ? ' class="actif" aria-current="page"' : '';
        return '<a href="' + r.page + '"' + actif + '>' + r.libelle + '</a>';
    }).join('');

    entete.innerHTML =
        '<a href="index.html"><strong>Studio photo</strong></a>' +
        '<nav aria-label="Rubriques">' + liens + '</nav>';
});
