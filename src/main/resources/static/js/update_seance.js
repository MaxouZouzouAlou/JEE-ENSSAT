document.addEventListener('DOMContentLoaded', chargerFormulaireSeance);

let seanceId;

async function chargerFormulaireSeance() {
    seanceId = lireParametre('id');
    if (!seanceId || !/^\d+$/.test(seanceId)) {
        afficherErreur('Identifiant de séance invalide.');
        return;
    }

    try {
        const seances = await api.get('/seances');
        const seance = seances.find(element => String(element.id) === seanceId);
        if (!seance) {
            afficherErreur('Séance introuvable.');
            return;
        }

        remplirOptions(seances, 'typeSeance', 'typeSeanceId', type => type.libelle);
        remplirOptions(seances, 'photographe', 'photographeId', photographe => photographe.nom);
        remplirOptions(seances, 'client', 'clientId', client =>
            client.type === 'ENTREPRISE'
                ? client.raisonSociale
                : [client.prenom, client.nom].filter(Boolean).join(' '));

        document.getElementById('date').value = seance.date.substring(0, 10);
        document.getElementById('heureDebut').value = seance.heureDebut.substring(0, 5);
        document.getElementById('duree').value = seance.duree;
        document.getElementById('lieu').value = seance.lieu;
        document.getElementById('statut').value = seance.statut;
        document.getElementById('typeSeanceId').value = seance.typeSeance.id;
        document.getElementById('photographeId').value = seance.photographe.id;
        document.getElementById('clientId').value = seance.client.id;
        document.getElementById('clientType').value = seance.client.type;
        document.getElementById('clientId').addEventListener('change', evenement => {
            const client = seances.find(element =>
                element.client && String(element.client.id) === evenement.target.value)?.client;
            if (client) {
                document.getElementById('clientType').value = client.type;
            }
        });

        document.getElementById('formulaire-seance').hidden = false;
        document.getElementById('formulaire-seance')
            .addEventListener('submit', enregistrerSeance);
    } catch (e) {
        // api.get affiche déjà le message d'erreur dans #messages.
    }
}

function remplirOptions(seances, propriete, idSelect, libelle) {
    const select = document.getElementById(idSelect);
    const valeurs = new Map();
    seances.forEach(seance => {
        const objet = seance[propriete];
        if (objet && objet.id !== null && objet.id !== undefined) {
            valeurs.set(String(objet.id), objet);
        }
    });

    valeurs.forEach(objet => {
        const option = document.createElement('option');
        option.value = objet.id;
        option.textContent = `${libelle(objet)} (ID ${objet.id})`;
        select.append(option);
    });
}

async function enregistrerSeance(evenement) {
    evenement.preventDefault();
    const client = document.getElementById('clientId');
    const donnees = {
        date: document.getElementById('date').value,
        heureDebut: document.getElementById('heureDebut').value + ':00',
        duree: document.getElementById('duree').value,
        lieu: document.getElementById('lieu').value,
        statut: document.getElementById('statut').value,
        typeSeanceId: document.getElementById('typeSeanceId').value,
        photographeId: document.getElementById('photographeId').value,
        clientId: client.value,
        clientType: document.getElementById('clientType').value
    };

    try {
        await api.put('/seances/' + encodeURIComponent(seanceId), donnees);
        // séance passée à TERMINEE : on propose la facture (si acceptée, la page de la facture s'ouvre)
        if (donnees.statut === 'TERMINEE' && proposerFacture({ id: seanceId, statut: 'TERMINEE' })) {
            return;
        }
        window.location.href = 'seances.html';
    } catch (e) {
        // api.put affiche déjà le message d'erreur dans #messages.
    }
}
