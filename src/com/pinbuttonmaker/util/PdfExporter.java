package com.pinbuttonmaker.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.imageio.ImageIO;

import com.pinbuttonmaker.ui.components.PaperPreviewPanel;

public final class PdfExporter {
    private static final double POINTS_PER_INCH = 72.0;
    private static final double KAPPA = 0.5522847498307936;
    private static final float CUT_LINE_WIDTH_POINTS = 2.0f;
    private static final int FALLBACK_RENDER_DPI = 300;

    private PdfExporter() {
        // Utility class.
    }

    public static void exportPdf(File outputFile, PaperPreviewPanel.ExportSnapshot snapshot) throws Exception {
        if (outputFile == null) {
            throw new IllegalArgumentException("Output file is required.");
        }
        if (snapshot == null) {
            throw new IllegalArgumentException("Export snapshot is required.");
        }

        File parent = outputFile.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Unable to create output directory.");
        }

        try {
            exportWithPdfBoxReflective(outputFile, snapshot);
        } catch (ClassNotFoundException | NoClassDefFoundError missingPdfBox) {
            exportWithInternalWriter(outputFile, snapshot);
        }
    }

    private static void exportWithPdfBoxReflective(File outputFile, PaperPreviewPanel.ExportSnapshot snapshot) throws Exception {
        Object document = null;
        Object contentStream = null;

        try {
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> pdPageClass = Class.forName("org.apache.pdfbox.pdmodel.PDPage");
            Class<?> pdRectangleClass = Class.forName("org.apache.pdfbox.pdmodel.common.PDRectangle");
            Class<?> pdPageContentStreamClass = Class.forName("org.apache.pdfbox.pdmodel.PDPageContentStream");

            document = pdDocumentClass.getConstructor().newInstance();

            float paperWidthPoints = (float) (snapshot.getPaperWidthInches() * POINTS_PER_INCH);
            float paperHeightPoints = (float) (snapshot.getPaperHeightInches() * POINTS_PER_INCH);
            Object paperRectangle = pdRectangleClass.getConstructor(float.class, float.class)
                .newInstance(paperWidthPoints, paperHeightPoints);
            Object page = pdPageClass.getConstructor(pdRectangleClass).newInstance(paperRectangle);

            Method addPageMethod = pdDocumentClass.getMethod("addPage", pdPageClass);
            addPageMethod.invoke(document, page);

            Constructor<?> streamCtor = pdPageContentStreamClass.getConstructor(pdDocumentClass, pdPageClass);
            contentStream = streamCtor.newInstance(document, page);

            double pdfPageHeightPoints = paperHeightPoints;

            Method setLineWidthMethod = pdPageContentStreamClass.getMethod("setLineWidth", float.class);
            Method setStrokingColorMethod = pdPageContentStreamClass.getMethod("setStrokingColor", float.class, float.class, float.class);
            Method setLineDashPatternMethod = pdPageContentStreamClass.getMethod("setLineDashPattern", float[].class, float.class);
            Method strokeMethod = pdPageContentStreamClass.getMethod("stroke");
            Method saveGraphicsStateMethod = pdPageContentStreamClass.getMethod("saveGraphicsState");
            Method restoreGraphicsStateMethod = pdPageContentStreamClass.getMethod("restoreGraphicsState");
            Method clipMethod = pdPageContentStreamClass.getMethod("clip");
            Method drawImageMethod = resolveDrawImageMethod(pdPageContentStreamClass);
            Method closePathMethod = pdPageContentStreamClass.getMethod("closePath");

            for (PaperPreviewPanel.PlacedSlot slot : snapshot.getSlots()) {
                double diameterPoints = slot.getDiameterInches() * POINTS_PER_INCH;
                double xPoints = slot.getXInches() * POINTS_PER_INCH;
                double yTopPoints = slot.getYInches() * POINTS_PER_INCH;
                double yPoints = pdfPageHeightPoints - yTopPoints - diameterPoints;
                double bleedDiameterPoints = slot.getBleedDiameterInches() * POINTS_PER_INCH;
                double bleedInsetPoints = (bleedDiameterPoints - diameterPoints) / 2.0;
                double bleedXPoints = xPoints - bleedInsetPoints;
                double bleedYPoints = yPoints - bleedInsetPoints;

                BufferedImage image = slot.getPreviewImage();
                if (image != null) {
                    Object imageXObject = createPdfImage(document, image);
                    saveGraphicsStateMethod.invoke(contentStream);
                    drawEllipsePath(contentStream, bleedXPoints, bleedYPoints, bleedDiameterPoints, bleedDiameterPoints);
                    closePathMethod.invoke(contentStream);
                    clipMethod.invoke(contentStream);
                    drawImageMethod.invoke(
                        contentStream,
                        imageXObject,
                        (float) bleedXPoints,
                        (float) bleedYPoints,
                        (float) bleedDiameterPoints,
                        (float) bleedDiameterPoints
                    );
                    restoreGraphicsStateMethod.invoke(contentStream);
                }

                if (snapshot.isShowCutLines() && image != null) {
                    setStrokingColorMethod.invoke(contentStream, 0.0f, 0.0f, 0.0f);
                    setLineWidthMethod.invoke(contentStream, CUT_LINE_WIDTH_POINTS);
                    setLineDashPatternMethod.invoke(contentStream, new float[] {}, 0.0f);
                    drawEllipsePath(
                        contentStream,
                        xPoints - (CUT_LINE_WIDTH_POINTS / 2.0),
                        yPoints - (CUT_LINE_WIDTH_POINTS / 2.0),
                        diameterPoints + CUT_LINE_WIDTH_POINTS,
                        diameterPoints + CUT_LINE_WIDTH_POINTS
                    );
                    closePathMethod.invoke(contentStream);
                    strokeMethod.invoke(contentStream);
                }
            }

            setLineDashPatternMethod.invoke(contentStream, new float[] {}, 0.0f);
            savePdf(document, outputFile);
        } finally {
            closeQuietly(contentStream);
            closeQuietly(document);
        }
    }

    private static void exportWithInternalWriter(File outputFile, PaperPreviewPanel.ExportSnapshot snapshot) throws Exception {
        BufferedImage rendered = renderSnapshotToImage(snapshot, FALLBACK_RENDER_DPI);
        byte[] jpegBytes = encodeJpeg(rendered);

        double paperWidthPoints = snapshot.getPaperWidthInches() * POINTS_PER_INCH;
        double paperHeightPoints = snapshot.getPaperHeightInches() * POINTS_PER_INCH;

        writeSingleImagePdf(
            outputFile,
            jpegBytes,
            rendered.getWidth(),
            rendered.getHeight(),
            paperWidthPoints,
            paperHeightPoints,
            paperWidthPoints,
            paperHeightPoints,
            0.0,
            0.0
        );
    }

    private static BufferedImage renderSnapshotToImage(PaperPreviewPanel.ExportSnapshot snapshot, int dpi) {
        int width = Math.max(1, (int) Math.round(snapshot.getPaperWidthInches() * dpi));
        int height = Math.max(1, (int) Math.round(snapshot.getPaperHeightInches() * dpi));

        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, width, height);

        float cutLineStrokePx = (float) ((CUT_LINE_WIDTH_POINTS / POINTS_PER_INCH) * dpi);
        Stroke cutLineStroke = new BasicStroke(cutLineStrokePx, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        for (PaperPreviewPanel.PlacedSlot slot : snapshot.getSlots()) {
            double diameterPx = slot.getDiameterInches() * dpi;
            double xPx = slot.getXInches() * dpi;
            double yPx = slot.getYInches() * dpi;
            double bleedDiameterPx = slot.getBleedDiameterInches() * dpi;
            double bleedInsetPx = (bleedDiameterPx - diameterPx) / 2.0;

            Ellipse2D circle = new Ellipse2D.Double(xPx, yPx, diameterPx, diameterPx);
            BufferedImage preview = slot.getPreviewImage();
            if (preview != null) {
                Ellipse2D bleedCircle = new Ellipse2D.Double(
                    xPx - bleedInsetPx,
                    yPx - bleedInsetPx,
                    bleedDiameterPx,
                    bleedDiameterPx
                );
                drawPreviewIntoCircle(g2, preview, bleedCircle);
            }

            if (snapshot.isShowCutLines() && preview != null) {
                Stroke oldStroke = g2.getStroke();
                g2.setStroke(cutLineStroke);
                g2.setColor(Color.BLACK);
                g2.draw(new Ellipse2D.Double(
                    xPx - (cutLineStrokePx / 2.0),
                    yPx - (cutLineStrokePx / 2.0),
                    diameterPx + cutLineStrokePx,
                    diameterPx + cutLineStrokePx
                ));
                g2.setStroke(oldStroke);
            }
        }

        g2.dispose();
        return canvas;
    }

    private static void drawPreviewIntoCircle(Graphics2D g2, BufferedImage preview, Ellipse2D circle) {
        Shape oldClip = g2.getClip();
        g2.clip(circle);

        double targetDiameter = circle.getWidth();
        double scale = Math.max(targetDiameter / Math.max(1, preview.getWidth()), targetDiameter / Math.max(1, preview.getHeight()));
        int drawWidth = Math.max(1, (int) Math.round(preview.getWidth() * scale));
        int drawHeight = Math.max(1, (int) Math.round(preview.getHeight() * scale));
        int drawX = (int) Math.round(circle.getX() + (targetDiameter - drawWidth) / 2.0);
        int drawY = (int) Math.round(circle.getY() + (targetDiameter - drawHeight) / 2.0);

        g2.drawImage(preview, drawX, drawY, drawWidth, drawHeight, null);
        g2.setClip(oldClip);
    }

    private static byte[] encodeJpeg(BufferedImage sourceImage) throws Exception {
        BufferedImage rgb = ensureOpaquePreview(sourceImage);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        boolean written = ImageIO.write(rgb, "jpg", output);
        if (!written) {
            throw new IllegalStateException("JPEG encoder is unavailable for fallback PDF export.");
        }
        return output.toByteArray();
    }

    private static void writeSingleImagePdf(
        File outputFile,
        byte[] jpegBytes,
        int imageWidth,
        int imageHeight,
        double pageWidthPoints,
        double pageHeightPoints,
        double drawWidthPoints,
        double drawHeightPoints,
        double offsetXPoints,
        double offsetYPoints
    ) throws Exception {
        ByteArrayOutputStream pdf = new ByteArrayOutputStream(Math.max(4096, jpegBytes.length + 2048));
        List<Integer> offsets = new ArrayList<>();

        writeAscii(pdf, "%PDF-1.4\n");
        writeAscii(pdf, "%\u00E2\u00E3\u00CF\u00D3\n");

        offsets.add(pdf.size());
        writeAscii(pdf, "1 0 obj\n");
        writeAscii(pdf, "<< /Type /Catalog /Pages 2 0 R >>\n");
        writeAscii(pdf, "endobj\n");

        offsets.add(pdf.size());
        writeAscii(pdf, "2 0 obj\n");
        writeAscii(pdf, "<< /Type /Pages /Count 1 /Kids [3 0 R] >>\n");
        writeAscii(pdf, "endobj\n");

        offsets.add(pdf.size());
        writeAscii(pdf, "3 0 obj\n");
        writeAscii(pdf, "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ");
        writeAscii(pdf, formatNumber(pageWidthPoints));
        writeAscii(pdf, " ");
        writeAscii(pdf, formatNumber(pageHeightPoints));
        writeAscii(pdf, "] ");
        writeAscii(pdf, "/Resources << /XObject << /Im0 4 0 R >> >> ");
        writeAscii(pdf, "/Contents 5 0 R >>\n");
        writeAscii(pdf, "endobj\n");

        offsets.add(pdf.size());
        writeAscii(pdf, "4 0 obj\n");
        writeAscii(pdf, "<< /Type /XObject /Subtype /Image ");
        writeAscii(pdf, "/Width " + imageWidth + " /Height " + imageHeight + " ");
        writeAscii(pdf, "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode ");
        writeAscii(pdf, "/Length " + jpegBytes.length + " >>\n");
        writeAscii(pdf, "stream\n");
        pdf.write(jpegBytes);
        writeAscii(pdf, "\nendstream\n");
        writeAscii(pdf, "endobj\n");

        String content = "q "
            + formatNumber(drawWidthPoints) + " 0 0 "
            + formatNumber(drawHeightPoints) + " "
            + formatNumber(offsetXPoints) + " "
            + formatNumber(offsetYPoints)
            + " cm /Im0 Do Q\n";
        byte[] contentBytes = content.getBytes(StandardCharsets.ISO_8859_1);

        offsets.add(pdf.size());
        writeAscii(pdf, "5 0 obj\n");
        writeAscii(pdf, "<< /Length " + contentBytes.length + " >>\n");
        writeAscii(pdf, "stream\n");
        pdf.write(contentBytes);
        writeAscii(pdf, "endstream\n");
        writeAscii(pdf, "endobj\n");

        int xrefOffset = pdf.size();
        int objectCount = offsets.size() + 1;
        writeAscii(pdf, "xref\n");
        writeAscii(pdf, "0 " + objectCount + "\n");
        writeAscii(pdf, "0000000000 65535 f \n");
        for (int offset : offsets) {
            writeAscii(pdf, String.format(Locale.US, "%010d 00000 n \n", offset));
        }

        writeAscii(pdf, "trailer\n");
        writeAscii(pdf, "<< /Size " + objectCount + " /Root 1 0 R >>\n");
        writeAscii(pdf, "startxref\n");
        writeAscii(pdf, xrefOffset + "\n");
        writeAscii(pdf, "%%EOF\n");

        try (FileOutputStream output = new FileOutputStream(outputFile)) {
            pdf.writeTo(output);
        }
    }

    private static void writeAscii(ByteArrayOutputStream output, String text) throws Exception {
        output.write(text.getBytes(StandardCharsets.ISO_8859_1));
    }

    private static String formatNumber(double value) {
        return String.format(Locale.US, "%.4f", value);
    }

    private static Method resolveDrawImageMethod(Class<?> pdPageContentStreamClass) throws Exception {
        Class<?> imageClass = Class.forName("org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject");
        return pdPageContentStreamClass.getMethod(
            "drawImage",
            imageClass,
            float.class,
            float.class,
            float.class,
            float.class
        );
    }

    private static Object createPdfImage(Object document, BufferedImage bufferedImage) throws Exception {
        BufferedImage safeImage = ensureOpaquePreview(bufferedImage);

        Class<?> losslessFactoryClass = Class.forName("org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory");
        Method createFromImageMethod = losslessFactoryClass.getMethod(
            "createFromImage",
            Class.forName("org.apache.pdfbox.pdmodel.PDDocument"),
            BufferedImage.class
        );
        return createFromImageMethod.invoke(null, document, safeImage);
    }

    private static BufferedImage ensureOpaquePreview(BufferedImage image) {
        if (image == null) {
            return null;
        }

        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = copy.createGraphics();
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, image.getWidth(), image.getHeight());
        g2.drawImage(image, 0, 0, null);
        g2.dispose();
        return copy;
    }

    private static void drawEllipsePath(Object contentStream, double x, double y, double width, double height) throws Exception {
        Method moveToMethod = contentStream.getClass().getMethod("moveTo", float.class, float.class);
        Method curveToMethod = contentStream.getClass().getMethod(
            "curveTo",
            float.class,
            float.class,
            float.class,
            float.class,
            float.class,
            float.class
        );

        double rx = width / 2.0;
        double ry = height / 2.0;
        double cx = x + rx;
        double cy = y + ry;

        double ox = rx * KAPPA;
        double oy = ry * KAPPA;

        moveToMethod.invoke(contentStream, (float) (cx + rx), (float) cy);

        curveToMethod.invoke(
            contentStream,
            (float) (cx + rx),
            (float) (cy + oy),
            (float) (cx + ox),
            (float) (cy + ry),
            (float) cx,
            (float) (cy + ry)
        );

        curveToMethod.invoke(
            contentStream,
            (float) (cx - ox),
            (float) (cy + ry),
            (float) (cx - rx),
            (float) (cy + oy),
            (float) (cx - rx),
            (float) cy
        );

        curveToMethod.invoke(
            contentStream,
            (float) (cx - rx),
            (float) (cy - oy),
            (float) (cx - ox),
            (float) (cy - ry),
            (float) cx,
            (float) (cy - ry)
        );

        curveToMethod.invoke(
            contentStream,
            (float) (cx + ox),
            (float) (cy - ry),
            (float) (cx + rx),
            (float) (cy - oy),
            (float) (cx + rx),
            (float) cy
        );
    }

    private static void savePdf(Object document, File outputFile) throws Exception {
        Method saveMethod = document.getClass().getMethod("save", File.class);
        saveMethod.invoke(document, outputFile);
    }

    private static void closeQuietly(Object closable) {
        if (closable == null) {
            return;
        }

        try {
            Method closeMethod = closable.getClass().getMethod("close");
            closeMethod.invoke(closable);
        } catch (Exception ignored) {
            // Ignore close exceptions.
        }
    }
}
