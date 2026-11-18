package fr.enssat.studiophoto.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import fr.enssat.studiophoto.model.Facture;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Génération du PDF d'une facture avec OpenPDF.
 * On n'utilise que les champs de la facture, pas le client ni la séance d'origine.
 */
public class FacturePdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final Font TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22);
    private static final Font SOUS_TITRE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font TEXTE = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font EN_TETE_TABLEAU = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

    /** le PDF en bytes */
    public byte[] generer(Facture facture) {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        try {
            PdfWriter.getInstance(document, sortie);
            document.open();

            document.add(new Paragraph("FACTURE", TITRE));
            document.add(new Paragraph("N° " + facture.getNumero(), SOUS_TITRE));
            document.add(new Paragraph("Émise le " + date(facture.getDateEmission())
                    + " - Statut : " + libelleStatut(facture), TEXTE));
            document.add(espace());

            document.add(new Paragraph("Studio photo", SOUS_TITRE));
            document.add(new Paragraph("Facturé à :", SOUS_TITRE));
            document.add(new Paragraph(facture.getClientDesignation(), TEXTE));
            document.add(new Paragraph(facture.getClientAdresse(), TEXTE));
            document.add(new Paragraph(facture.getClientEmail(), TEXTE));
            if (facture.getClientSirenTva() != null) {
                document.add(new Paragraph("SIREN/TVA : " + facture.getClientSirenTva(), TEXTE));
            }
            document.add(espace());

            document.add(tableauPrestation(facture));
            document.add(espace());
            document.add(tableauMontants(facture));

            document.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("Génération du PDF de la facture " + facture.getNumero() + " impossible", e);
        }
        return sortie.toByteArray();
    }

    private PdfPTable tableauPrestation(Facture f) {
        PdfPTable tableau = new PdfPTable(new float[]{3f, 2f, 2f, 2f, 2f});
        tableau.setWidthPercentage(100);
        for (String titre : new String[]{"Prestation", "Date", "Heure", "Durée", "Photographe"}) {
            PdfPCell cellule = new PdfPCell(new Phrase(titre, EN_TETE_TABLEAU));
            cellule.setBackgroundColor(Color.DARK_GRAY);
            cellule.setPadding(5);
            tableau.addCell(cellule);
        }
        tableau.addCell(cellule(f.getPrestationType(), Element.ALIGN_LEFT));
        tableau.addCell(cellule(date(f.getPrestationDate()), Element.ALIGN_LEFT));
        tableau.addCell(cellule(f.getPrestationHeure().toString(), Element.ALIGN_LEFT));
        tableau.addCell(cellule(f.getPrestationDuree() + " min", Element.ALIGN_LEFT));
        tableau.addCell(cellule(f.getPhotographeNom(), Element.ALIGN_LEFT));
        return tableau;
    }

    private PdfPTable tableauMontants(Facture f) {
        PdfPTable tableau = new PdfPTable(new float[]{3f, 1.5f});
        tableau.setWidthPercentage(45);
        tableau.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tableau.addCell(cellule("Total HT", Element.ALIGN_LEFT));
        tableau.addCell(cellule(euros(f.getMontantHT()), Element.ALIGN_RIGHT));
        tableau.addCell(cellule("TVA " + f.getTauxTVA().stripTrailingZeros().toPlainString() + " %",
                Element.ALIGN_LEFT));
        tableau.addCell(cellule(euros(f.getMontantTTC().subtract(f.getMontantHT())), Element.ALIGN_RIGHT));
        tableau.addCell(new PdfPCell(new Phrase("Total TTC", TOTAL)));
        PdfPCell ttc = new PdfPCell(new Phrase(euros(f.getMontantTTC()), TOTAL));
        ttc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        tableau.addCell(ttc);
        return tableau;
    }

    private PdfPCell cellule(String texte, int alignement) {
        PdfPCell cellule = new PdfPCell(new Phrase(texte, TEXTE));
        cellule.setHorizontalAlignment(alignement);
        cellule.setPadding(5);
        return cellule;
    }

    private Paragraph espace() {
        return new Paragraph(" ", TEXTE);
    }

    private String date(LocalDate date) {
        return DATE.format(date);
    }

    // pas d'espace insécable, la police de base du PDF ne sait pas l'afficher
    private String euros(BigDecimal montant) {
        return montant.toPlainString().replace('.', ',') + " €";
    }

    private String libelleStatut(Facture f) {
        return switch (f.getStatutPaiement()) {
            case EN_ATTENTE -> "en attente de paiement";
            case PAYEE -> "payée";
            case ANNULEE -> "annulée";
        };
    }
}
