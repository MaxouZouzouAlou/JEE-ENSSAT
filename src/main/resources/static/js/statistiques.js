/* =====================================================================
   Statistiques - Lot 5
   Classements des photographes et chiffre d'affaires d'une période.
   Les outils communs (api, echapperHtml, formaterDuree, formaterMontant...)
   sont dans js/api.js ; api.js affiche lui-même les messages d'erreur.
   ===================================================================== */

document.addEventListener('DOMContentLoaded', () => {
    chargerPhotographesParSeances();
});

async function chargerPhotographesParSeances()
{
    const contenu = document.getElementById('contenu');
    contenu.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th scope="col">Photographe</th>
                    <th scope="col">Nombre de séances réalisées</th>
                </tr>
            </thead>
            <tbody id="liste-photographes">
                <tr><td colspan="2">Chargement des photographes...</td></tr>
            </tbody>
        </table>
    `;

    const corpsTableau = document.getElementById('liste-photographes');
    try {
        const resultats = await api.get('/statistiques/photographes_par_seances');
        if (resultats.length === 0) {
            corpsTableau.innerHTML = '<tr><td colspan="2">Aucune séance terminée pour le moment.</td></tr>';
            return;
        }
        corpsTableau.innerHTML = resultats.map(resultat => `
            <tr>
                <td>${echapperHtml(resultat[0])}</td>
                <td>${echapperHtml(resultat[1])}</td>
            </tr>
        `).join('');
    } catch (e) {
        // le message d'erreur est déjà affiché par api.js
        corpsTableau.innerHTML = '<tr><td colspan="2">Classement indisponible.</td></tr>';
    }
}

async function chargerPhotographesParDuree()
{
    const contenu = document.getElementById('contenu');
    contenu.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th scope="col">Photographe</th>
                    <th scope="col">Durée des séances réalisées</th>
                </tr>
            </thead>
            <tbody id="liste-photographes">
                <tr><td colspan="2">Chargement des photographes...</td></tr>
            </tbody>
        </table>
    `;

    const corpsTableau = document.getElementById('liste-photographes');
    try {
        const resultats = await api.get('/statistiques/photographes_par_duree');
        if (resultats.length === 0) {
            corpsTableau.innerHTML = '<tr><td colspan="2">Aucune séance terminée pour le moment.</td></tr>';
            return;
        }
        corpsTableau.innerHTML = resultats.map(resultat => `
            <tr>
                <td>${echapperHtml(resultat[0])}</td>
                <td>${echapperHtml(formaterDuree(resultat[1]))}</td>
            </tr>
        `).join('');
    } catch (e) {
        // le message d'erreur est déjà affiché par api.js
        corpsTableau.innerHTML = '<tr><td colspan="2">Classement indisponible.</td></tr>';
    }
}

function demanderPeriode()
{
    const contenu = document.getElementById('contenu');
    contenu.innerHTML = `
        <p>Renseignez la date de début de période :</p>
        <input type="date" id="dateDebut"/>
        <p>Renseignez la date de fin de période :</p>
        <input type="date" id="dateFin"/>
        <p><button type="button" onclick="chargerChiffreAffairesPourPeriode()">Calculer</button></p>
        <div id="chiffre"></div>
    `;
}

async function chargerChiffreAffairesPourPeriode()
{
    const dateDebut = document.getElementById('dateDebut');
    const dateFin = document.getElementById('dateFin');
    const chiffre = document.getElementById('chiffre');
    chiffre.innerHTML = `
        <p>Chargement du résultat...</p>
    `;

    try {
        const resultat = await api.post(
            '/statistiques/chiffre_affaires_pour_periode', {"dateDebut" : dateDebut.value, "dateFin" : dateFin.value}
        );
        // 0 est un résultat valide (aucune facture payée sur la période)
        chiffre.innerHTML = `
            <p>Chiffre d'affaires TTC des factures payées : <strong>${echapperHtml(formaterMontant(resultat))}</strong></p>
        `;
    } catch (e) {
        // période invalide ou erreur serveur : le message est déjà affiché par api.js
        chiffre.innerHTML = '';
    }
}

