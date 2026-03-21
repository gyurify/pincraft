import java.io.File;
import com.pinbuttonmaker.ui.components.PaperPreviewPanel;
import com.pinbuttonmaker.util.PdfExporter;

public class PdfSmokeTest {
    public static void main(String[] args) throws Exception {
        PaperPreviewPanel panel = new PaperPreviewPanel();
        panel.setPaperSize("A4", 8.27, 11.69);
        panel.setButtonLayout("2.25\"", 2.25, 0.36, 0.62, 0.30, 0.80);
        panel.setShowCutLines(true);
        PdfExporter.exportA4Pdf(new File("out/test-export.pdf"), panel.buildExportSnapshot());
        System.out.println("ok");
    }
}
