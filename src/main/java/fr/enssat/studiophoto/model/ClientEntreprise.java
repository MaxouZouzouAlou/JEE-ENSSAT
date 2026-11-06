package fr.enssat.studiophoto.model;

/** Client entreprise, caractérisé par une raison sociale et un numéro SIREN/TVA. */
public class ClientEntreprise extends Client {

    private String raisonSociale;
    private String sirenTva;

    public ClientEntreprise() {
    }

    public ClientEntreprise(Long id, String adressePostale, String email, String raisonSociale, String sirenTva) {
        super(id, adressePostale, email);
        this.raisonSociale = raisonSociale;
        this.sirenTva = sirenTva;
    }

    @Override
    public TypeClient getType() {
        return TypeClient.ENTREPRISE;
    }

    @Override
    public String designation() {
        return raisonSociale;
    }

    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }

    public String getSirenTva() { return sirenTva; }
    public void setSirenTva(String sirenTva) { this.sirenTva = sirenTva; }

    @Override
    public String toString() {
        return "ClientEntreprise{id=" + getId() + ", " + raisonSociale + "}";
    }
}
