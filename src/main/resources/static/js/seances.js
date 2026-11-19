/* =====================================================================
   Séances - Lots 2 et 3
   Script propre à seances.html : seul ce fichier et seances.html sont à
   modifier pour cette rubrique. Les outils communs (api, afficherErreur,
   formaterDate, badgeStatut, echapperHtml...) sont dans js/api.js.
   ===================================================================== */

document.addEventListener('DOMContentLoaded', () => {
    // bouton "Générer la facture" des séances terminées (js/facturation.js)
    activerBoutonsFacture(document.getElementById('liste-seances'));
    chargerSeances();
});

async function chargerSeances() {
    const corpsTableau = document.getElementById('liste-seances');
    try {
        const [seances, facturesParSeance] = await Promise.all([api.get('/seance'), chargerFacturesParSeance()]);
        if (seances.length === 0) {
            corpsTableau.innerHTML = '<tr><td colspan="7">Aucune séance à afficher.</td></tr>';
            return;
        }

        corpsTableau.innerHTML = seances.map(seance => `
            <tr>
                <td>${echapperHtml(seance.id)}</td>
                <td>${echapperHtml(formaterDate(seance.date))}</td>
                <td>${echapperHtml(formaterHeure(seance.heureDebut))}</td>
                <td>${echapperHtml(formaterDuree(seance.duree))}</td>
                <td>${echapperHtml(seance.lieu)}</td>
                <td>${badgeStatut(seance.statut)}</td>
                <td><a class="bouton-lien" href="seance.html?id=${encodeURIComponent(seance.id)}">Détail</a> <a class="bouton-lien secondaire" href="update_seance.html?id=${encodeURIComponent(seance.id)}">Modifier</a> ${htmlBoutonFacture(seance, facturesParSeance)}</td>
            </tr>
        `).join('');
    } catch (e) {
        // api.get affiche déjà le message d'erreur dans #messages.
    }
}
