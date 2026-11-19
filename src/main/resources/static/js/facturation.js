/* =====================================================================
   Facturation : génération de la facture d'une séance (utilisable depuis n'importe quelle page)

   Pour mettre le bouton "Générer la facture" sur la fiche d'une séance :
     1. inclure js/facturation.js dans seances.html
     2. mettre htmlBoutonFacture(seance) dans le html de la fiche (vide si la séance n'est pas TERMINEE)
     3. appeler activerBoutonsFacture(conteneur) une seule fois sur le conteneur de la fiche
     4. après un passage à TERMINEE, appeler proposerFacture(seance) pour proposer la facture tout de suite

   Dépend de js/api.js (api, afficherSucces, echapperHtml).
   ===================================================================== */

// bouton "Générer la facture", seulement si la séance est TERMINEE
function htmlBoutonFacture(seance) {
    if (!seance || seance.statut !== 'TERMINEE') {
        return '';
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
