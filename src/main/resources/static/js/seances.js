/* =====================================================================
   Séances - Lots 2 et 3
   Script propre à seances.html : seul ce fichier et seances.html sont à
   modifier pour cette rubrique. Les outils communs (api, afficherErreur,
   formaterDate, badgeStatut, echapperHtml...) sont dans js/api.js.
   ===================================================================== */

document.addEventListener('DOMContentLoaded', () => {
    chargerSeances();
});

async function chargerSeances() {
    const corpsTableau = document.getElementById('liste-seances');
    try {
        const seances = await api.get('/seance');
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
                <td><a class="bouton-lien" href="update_seance.html?id=${encodeURIComponent(seance.id)}">Modifier</a></td>
            </tr>
        `).join('');
    } catch (e) {
        // api.get affiche déjà le message d'erreur dans #messages.
    }
}
