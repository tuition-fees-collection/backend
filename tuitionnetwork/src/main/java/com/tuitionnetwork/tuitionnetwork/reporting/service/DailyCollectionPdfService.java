package com.tuitionnetwork.tuitionnetwork.reporting.service;

import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionRecord;
import com.tuitionnetwork.tuitionnetwork.reporting.domain.DailyCollectionReport;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

@Service
public class DailyCollectionPdfService {

    public byte[] generatePdf(DailyCollectionReport report) {

        try (
                PDDocument document = new PDDocument();
                ByteArrayOutputStream output =
                        new ByteArrayOutputStream()
        ) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font regularFont =
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            PDType1Font boldFont =
                    new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

            try (PDPageContentStream content =
                         new PDPageContentStream(document, page)) {

                float y = 800;

                y = writeLine(
                        content,
                        boldFont,
                        18,
                        50,
                        y,
                        "Daily Collection Report"
                );

                y -= 10;

                y = writeLine(
                        content,
                        regularFont,
                        12,
                        50,
                        y,
                        "Date: " + report.date()
                );

                y = writeLine(
                        content,
                        regularFont,
                        12,
                        50,
                        y,
                        "Total Collected: "
                                + report.totalCollected()
                                + " EGP"
                );

                y = writeLine(
                        content,
                        regularFont,
                        12,
                        50,
                        y,
                        "Payment Count: "
                                + report.paymentCount()
                );

                y -= 15;

                y = writeLine(
                        content,
                        boldFont,
                        14,
                        50,
                        y,
                        "Breakdown by Fee Type"
                );

                for (Map.Entry<?, BigDecimal> entry :
                        report.amountByFeeType().entrySet()) {

                    y = writeLine(
                            content,
                            regularFont,
                            11,
                            60,
                            y,
                            entry.getKey()
                                    + ": "
                                    + entry.getValue()
                                    + " EGP"
                    );
                }

                y -= 15;

                y = writeLine(
                        content,
                        boldFont,
                        14,
                        50,
                        y,
                        "Breakdown by Payment Method"
                );

                for (Map.Entry<?, BigDecimal> entry :
                        report.amountByPaymentMethod().entrySet()) {

                    y = writeLine(
                            content,
                            regularFont,
                            11,
                            60,
                            y,
                            entry.getKey()
                                    + ": "
                                    + entry.getValue()
                                    + " EGP"
                    );
                }

                y -= 15;

                y = writeLine(
                        content,
                        boldFont,
                        14,
                        50,
                        y,
                        "Collections"
                );

                for (DailyCollectionRecord item : report.items()) {

                    String line =
                            item.studentId()
                                    + " | "
                                    + item.feeType()
                                    + " | "
                                    + item.amountApplied()
                                    + " EGP | "
                                    + item.paymentMethod()
                                    + " | Ref: "
                                    + item.paymentReference();

                    y = writeLine(
                            content,
                            regularFont,
                            9,
                            50,
                            y,
                            line
                    );
                }
            }

            document.save(output);

            return output.toByteArray();

        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to generate daily collection PDF",
                    ex
            );
        }
    }

    private float writeLine(
            PDPageContentStream content,
            PDType1Font font,
            float fontSize,
            float x,
            float y,
            String text
    ) throws IOException {

        content.beginText();
        content.setFont(font, fontSize);
        content.newLineAtOffset(x, y);
        content.showText(text);
        content.endText();

        return y - (fontSize + 6);
    }
}