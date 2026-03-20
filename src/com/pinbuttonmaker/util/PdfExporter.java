package com.pinbuttonmaker.util;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import com.pinbuttonmaker.ui.components.PaperPreviewPanel;

public final class PdfExporter {
    private static final double POINTS_PER_INCH = 72.0;
    private static final double KAPPA = 0.5522847498307936;

    private PdfExporter() {
        // Utility class.
    }

    public static void exportA4Pdf(File outputFile, PaperPreviewPanel.ExportSnapshot snapshot) throws Exception {
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

        Object document = null;
        Object contentStream = null;

        try {
            Class<?> pdDocumentClass = Class.forName("org.apache.pdfbox.pdmodel.PDDocument");
            Class<?> pdPageClass = Class.forName("org.apache.pdfbox.pdmodel.PDPage");
            Class<?> pdRectangleClass = Class.forName("org.apache.pdfbox.pdmodel.common.PDRectangle");
            Class<?> pdPageContentStreamClass = Class.forName("org.apache.pdfbox.pdmodel.PDPageContentStream");

            document = pdDocumentClass.getConstructor().newInstance();

            Field a4Field = pdRectangleClass.getField("A4");
            Object a4Rectangle = a4Field.get(null);
            Object page = pdPageClass.getConstructor(pdRectangleClass).newInstance(a4Rectangle);

            Method addPageMethod = pdDocumentClass.getMethod("addPage", pdPageClass);
            addPageMethod.invoke(document, page);

            Constructor<?> streamCtor = pdPageContentStreamClass.getConstructor(pdDocumentClass, pdPageClass);
            contentStream = streamCtor.newInstance(document, page);

            float a4Width = (float) pdRectangleClass.getMethod("getWidth").invoke(a4Rectangle);
            float a4Height = (float) pdRectangleClass.getMethod("getHeight").invoke(a4Rectangle);

            double paperWidthPoints = snapshot.getPaperWidthInches() * POINTS_PER_INCH;
            double paperHeightPoints = snapshot.getPaperHeightInches() * POINTS_PER_INCH;

            double scale = Math.min(a4Width / paperWidthPoints, a4Height / paperHeightPoints);
            double contentWidth = paperWidthPoints * scale;
            double contentHeight = paperHeightPoints * scale;

            double offsetX = (a4Width - contentWidth) / 2.0;
            double offsetY = (a4Height - contentHeight) / 2.0;

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
                double diameterPoints = slot.getDiameterInches() * POINTS_PER_INCH * scale;
                double xPoints = offsetX + (slot.getXInches() * POINTS_PER_INCH * scale);
                double yTopPoints = offsetY + (slot.getYInches() * POINTS_PER_INCH * scale);
                double yPoints = (a4Height - yTopPoints) - diameterPoints;

                BufferedImage image = slot.getPreviewImage();
                if (image != null) {
                    Object imageXObject = createPdfImage(document, image);
                    saveGraphicsStateMethod.invoke(contentStream);
                    drawEllipsePath(contentStream, xPoints, yPoints, diameterPoints, diameterPoints);
                    closePathMethod.invoke(contentStream);
                    clipMethod.invoke(contentStream);
                    drawImageMethod.invoke(
                        contentStream,
                        imageXObject,
                        (float) xPoints,
                        (float) yPoints,
                        (float) diameterPoints,
                        (float) diameterPoints
                    );
                    restoreGraphicsStateMethod.invoke(contentStream);
                }

                if (snapshot.isShowCutLines()) {
                    setStrokingColorMethod.invoke(contentStream, 0.35f, 0.50f, 0.72f);
                    setLineWidthMethod.invoke(contentStream, 1.1f);
                    setLineDashPatternMethod.invoke(contentStream, new float[] {4.0f, 3.0f}, 0.0f);
                    drawEllipsePath(contentStream, xPoints, yPoints, diameterPoints, diameterPoints);
                    closePathMethod.invoke(contentStream);
                    strokeMethod.invoke(contentStream);
                }
            }

            setLineDashPatternMethod.invoke(contentStream, new float[] {}, 0.0f);
            savePdf(document, outputFile);
        } catch (ClassNotFoundException classNotFoundException) {
            throw new IllegalStateException(
                "Apache PDFBox is not available. Add pdfbox and pdfbox-io jars to the classpath before exporting."
            );
        } finally {
            closeQuietly(contentStream);
            closeQuietly(document);
        }
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

        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g2 = copy.createGraphics();
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
