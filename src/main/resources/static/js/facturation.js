/* =====================================================================
   Facturation : génération de la facture d'une séance (utilisable depuis n'importe quelle page)

   Pour mettre le bouton "Générer la facture" sur la fiche d'une séance :
     1. inclure js/facturation.js dans seances.html
     2. charger la liste avec chargerFacturesParSeance(), puis mettre htmlBoutonFacture(seance, liste) dans le
        html de la fiche (vide si la séance n'est pas TERMINEE, "Voir la facture" si elle est déjà facturée)
     3. appeler activerBoutonsFacture(conteneur) une seule fois sur le conteneur de la fiche
     4. après un passage à TERMINEE, appeler proposerFacture(seance) pour proposer la facture tout de suite

   Dépend de js/api.js (api, afficherSucces, echapperHtml).
   ===================================================================== */

// séance -> id de sa facture, pour savoir lesquelles sont déjà facturées (une seule requête pour toute la liste)
async function chargerFacturesParSeance() {
    const parSeance = new Map();
    try {
        const factures = await api.get('/factures');
        factures.forEach(f => parSeance.set(f.seanceId, f.id));
    } catch (e) {
        // erreur déjà affichée par api.js ; sans la liste on propose le bouton, le serveur refusera si besoin
    }
    return parSeance;
}

// séance TERMINEE seulement : "Voir la facture" si elle en a déjà une, sinon "Générer la facture"
function htmlBoutonFacture(seance, facturesParSeance) {
    if (!seance || seance.statut !== 'TERMINEE') {
        return '';
    }
    const factureId = facturesParSeance && facturesParSeance.get(seance.id);
    if (factureId) {
        return '<a class="bouton-lien secondaire" href="factures.html?id=' + Number(factureId) + '">Voir la facture</a>';
    }
    return '<button type="button" data-action="generer-facture" data-seance-id="'
        + echapperHtml(seance.id) + '">Générer la facture</button>';
}

// branche les boutons (délégation d'évènement, sa marche aussi pour ceux ajoutés après)
function activerBoutonsFacture(racine) {
    racine.addEventListener('click', e => {
        const bouton = e.target.closest('button[data-action="generer-facture"]');
        if (bouton) {
            genererFacture(Number(bouton.dataset.seanceId), bouton);
        }
    });
}

// POST puis redirection vers la facture. Si ça échoue (pas terminée, déjà facturée...)
// api.js affiche l'erreur et on reste sur la page
async function genererFacture(seanceId, bouton) {
    if (bouton) bouton.disabled = true;
    try {
        const facture = await api.post('/seances/' + seanceId + '/facture');
        afficherSucces('Facture ' + facture.numero + ' générée.');
        window.location.href = 'factures.html?id=' + facture.id;
    } catch (e) {
        if (bouton) bouton.disabled = false;
    }
}

// à appeler juste après le passage d'une séance à TERMINEE : propose de générer la facture.
// Renvoie true si la secrétaire accepte (genererFacture ouvre alors la facture), false sinon
// (elle pourra toujours utiliser le bouton "Générer la facture" plus tard).
function proposerFacture(seance) {
    if (!seance || seance.statut !== 'TERMINEE') {
        return false;
    }
    if (!window.confirm('La séance est terminée. Générer la facture maintenant ?')) {
        return false;
    }
    genererFacture(Number(seance.id), null);
    return true;
}
