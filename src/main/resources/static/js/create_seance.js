document.addEventListener('DOMContentLoaded', chargerFormulaireSeance);

async function chargerFormulaireSeance() {
    try {
        const options = await api.get('/seances/options');
        remplirOptions('typeSeanceId', options.typesSeance, type => type.libelle);
        remplirOptions('photographeId', options.photographes, photographe => photographe.nom);
        remplirOptions('clientId', options.clients, client => client.designation);

        if (!options.typesSeance.length || !options.photographes.length || !options.clients.length) {
            afficherErreur('La création nécessite au moins un type de séance, un photographe actif et un client.');
            return;
        }

        document.getElementById('clientId').addEventListener('change', evenement => {
            const client = options.clients.find(element => String(element.id) === evenement.target.value);
            document.getElementById('clientType').value = client ? client.type : '';
        });
        document.getElementById('formulaire-seance').hidden = false;
        document.getElementById('formulaire-seance').addEventListener('submit', creerSeance);
    } catch (e) {
        // api.get affiche déjà le message d'erreur dans #messages.
    }
}

function remplirOptions(idSelect, valeurs, libelle) {
    const select = document.getElementById(idSelect);
    const choixInitial = document.createElement('option');
    choixInitial.value = '';
    choixInitial.textContent = 'Sélectionner…';
    choixInitial.disabled = true;
    choixInitial.selected = true;
    select.append(choixInitial);

    valeurs.forEach(objet => {
        const option = document.createElement('option');
        option.value = objet.id;
        option.textContent = `${libelle(objet)} (ID ${objet.id})`;
        select.append(option);
    });
}

async function creerSeance(evenement) {
    evenement.preventDefault();
    const donnees = {
        date: document.getElementById('date').value,
        heureDebut: document.getElementById('heureDebut').value + ':00',
        duree: document.getElementById('duree').value,
        lieu: document.getElementById('lieu').value,
        typeSeanceId: document.getElementById('typeSeanceId').value,
        photographeId: document.getElementById('photographeId').value,
        clientId: document.getElementById('clientId').value,
        clientType: document.getElementById('clientType').value
    };

    try {
        await api.post('/seances', donnees);
        window.location.href = 'seances.html';
    } catch (e) {
        // api.post affiche déjà le message d'erreur dans #messages.
    }
}
