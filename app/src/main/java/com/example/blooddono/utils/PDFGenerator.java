package com.example.blooddono.utils;

import android.content.Context;
import android.graphics.Color;
import android.os.Environment;

import com.example.blooddono.models.Donation;
import com.example.blooddono.models.DonationDrive;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PDFGenerator {
    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font HEADER_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 12);
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

    public interface OnPDFGeneratedListener {
        void onSuccess(File pdfFile);
        void onError(Exception e);
    }

    public static void generateDriveReport(Context context, DonationDrive drive, List<Donation> donations,
                                           OnPDFGeneratedListener listener) {
        try {
            // Create PDF document
            Document document = new Document();

            // Create file in app's private documents directory
            File outputDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "reports");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            String fileName = "drive_report_" + drive.getId() + ".pdf";
            File outputFile = new File(outputDir, fileName);

            PdfWriter.getInstance(document, new FileOutputStream(outputFile));
            document.open();

            // Add drive details
            addDriveDetails(document, drive);

            // Add blood collection summary
            addBloodCollectionSummary(document, drive);

            // Add donations table
            if (!donations.isEmpty()) {
                addDonationsTable(document, donations);
            }

            document.close();
            listener.onSuccess(outputFile);

        } catch (DocumentException | IOException e) {
            listener.onError(e);
        }
    }

    private static void addDriveDetails(Document document, DonationDrive drive) throws DocumentException {
        // Title
        Paragraph title = new Paragraph("Donation Drive Report", TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        // Drive details section
        document.add(new Paragraph("Drive Details:", HEADER_FONT));
        document.add(new Paragraph("Name: " + drive.getName(), NORMAL_FONT));
        document.add(new Paragraph("Owner: " + drive.getOwnerName(), NORMAL_FONT));
        document.add(new Paragraph("Status: " + (drive.isActive() ? "Active" : "Completed"), NORMAL_FONT));
        document.add(new Paragraph("Start Date: " + dateFormat.format(new Date(drive.getStartDate())), NORMAL_FONT));
        document.add(new Paragraph("End Date: " + dateFormat.format(new Date(drive.getEndDate())), NORMAL_FONT));

        if (!drive.isActive() && drive.getCompletedAt() > 0) {
            document.add(new Paragraph("Completed On: " + dateFormat.format(new Date(drive.getCompletedAt())), NORMAL_FONT));
        }

        document.add(new Paragraph("Total Donations: " + drive.getTotalDonations(), NORMAL_FONT));
        document.add(Chunk.NEWLINE);
    }

    private static void addBloodCollectionSummary(Document document, DonationDrive drive) throws DocumentException {
        document.add(new Paragraph("Blood Collection Summary:", HEADER_FONT));

        // Create table for blood collection
        PdfPTable table = new PdfPTable(2); // 2 columns
        table.setWidthPercentage(100);

        // Add headers
        addTableHeader(table, new String[]{"Blood Type", "Amount Collected (mL)"});

        // Add data
        for (Map.Entry<String, Double> entry : drive.getTotalCollectedAmounts().entrySet()) {
            table.addCell(new Phrase(entry.getKey(), NORMAL_FONT));
            table.addCell(new Phrase(String.format("%.1f", entry.getValue()), NORMAL_FONT));
        }

        document.add(table);
        document.add(Chunk.NEWLINE);
    }

    private static void addDonationsTable(Document document, List<Donation> donations) throws DocumentException {
        document.add(new Paragraph("Detailed Donations:", HEADER_FONT));

        // Create table
        PdfPTable table = new PdfPTable(6); // 6 columns
        table.setWidthPercentage(100);

        // Add headers
        addTableHeader(table, new String[]{
                "Donor Name", "Donation Site", "Blood Types", "Status", "Date", "Collected Amounts"
        });

        // Add data
        for (Donation donation : donations) {
            table.addCell(new Phrase(donation.getDonorName(), NORMAL_FONT));
            table.addCell(new Phrase(donation.getSiteName(), NORMAL_FONT));
            table.addCell(new Phrase(String.join(", ", donation.getBloodTypes()), NORMAL_FONT));
            table.addCell(new Phrase(donation.getStatus().toUpperCase(), NORMAL_FONT));
            table.addCell(new Phrase(dateFormat.format(new Date(donation.getCreatedAt())), NORMAL_FONT));

            // Format collected amounts
            StringBuilder amounts = new StringBuilder();
            if (donation.getCollectedAmounts() != null) {
                for (Map.Entry<String, Double> entry : donation.getCollectedAmounts().entrySet()) {
                    amounts.append(entry.getKey())
                            .append(": ")
                            .append(String.format("%.1f", entry.getValue()))
                            .append(" mL\n");
                }
            }
            table.addCell(new Phrase(amounts.toString(), NORMAL_FONT));
        }

        document.add(table);
    }

    private static void addTableHeader(PdfPTable table, String[] headers) {
        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, HEADER_FONT));
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            cell.setPadding(5);
            table.addCell(cell);
        }
    }
}