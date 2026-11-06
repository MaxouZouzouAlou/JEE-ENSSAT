package fr.enssat.studiophoto.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Facture d'une séance TERMINEE (une seule par séance). Table {@code facture}.
 *
 * <p>Les coordonnées du client et le détail de la prestation sont copiés au moment
 * de la génération : une facture émise ne change plus, même si le client est modifié.</p>
 */
public class Facture {

    private Long id;
    /** Numéro unique, ex. FAC-2026-0001. */
    private String numero;
    private LocalDate dateEmission;
    private BigDecimal montantHT;
    /** Taux de TVA en pourcentage, ex. 20.00. */
    private BigDecimal tauxTVA;
    private BigDecimal montantTTC;
    private StatutPaiement statutPaiement = StatutPaiement.EN_ATTENTE;
    private Long seanceId;

    // Coordonnées du client au moment de la facturation
    private String clientDesignation;
    private String clientAdresse;
    private String clientEmail;
    private String clientSirenTva;

    // Détail de la prestation
    private LocalDate prestationDate;
    private LocalTime prestationHeure;
    /** Durée en minutes. */
    private int prestationDuree;
    private String prestationType;
    private String photographeNom;

    public Facture() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }

    public LocalDate getDateEmission() { return dateEmission; }
    public void setDateEmission(LocalDate dateEmission) { this.dateEmission = dateEmission; }

    public BigDecimal getMontantHT() { return montantHT; }
    public void setMontantHT(BigDecimal montantHT) { this.montantHT = montantHT; }

    public BigDecimal getTauxTVA() { return tauxTVA; }
    public void setTauxTVA(BigDecimal tauxTVA) { this.tauxTVA = tauxTVA; }

    public BigDecimal getMontantTTC() { return montantTTC; }
    public void setMontantTTC(BigDecimal montantTTC) { this.montantTTC = montantTTC; }

    public StatutPaiement getStatutPaiement() { return statutPaiement; }
    public void setStatutPaiement(StatutPaiement statutPaiement) { this.statutPaiement = statutPaiement; }

    public Long getSeanceId() { return seanceId; }
    public void setSeanceId(Long seanceId) { this.seanceId = seanceId; }

    public String getClientDesignation() { return clientDesignation; }
    public void setClientDesignation(String clientDesignation) { this.clientDesignation = clientDesignation; }

    public String getClientAdresse() { return clientAdresse; }
    public void setClientAdresse(String clientAdresse) { this.clientAdresse = clientAdresse; }

    public String getClientEmail() { return clientEmail; }
    public void setClientEmail(String clientEmail) { this.clientEmail = clientEmail; }

    public String getClientSirenTva() { return clientSirenTva; }
    public void setClientSirenTva(String clientSirenTva) { this.clientSirenTva = clientSirenTva; }

    public LocalDate getPrestationDate() { return prestationDate; }
    public void setPrestationDate(LocalDate prestationDate) { this.prestationDate = prestationDate; }

    public LocalTime getPrestationHeure() { return prestationHeure; }
    public void setPrestationHeure(LocalTime prestationHeure) { this.prestationHeure = prestationHeure; }

    public int getPrestationDuree() { return prestationDuree; }
    public void setPrestationDuree(int prestationDuree) { this.prestationDuree = prestationDuree; }

    public String getPrestationType() { return prestationType; }
    public void setPrestationType(String prestationType) { this.prestationType = prestationType; }

    public String getPhotographeNom() { return photographeNom; }
    public void setPhotographeNom(String photographeNom) { this.photographeNom = photographeNom; }

    @Override
    public String toString() {
        return "Facture{numero='" + numero + "', TTC=" + montantTTC + ", statut=" + statutPaiement + "}";
    }
}
