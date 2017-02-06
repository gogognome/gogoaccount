package nl.gogognome.gogoaccount.gui.invoice;

import org.xhtmlrenderer.pdf.ITextRenderer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

public class PdfGenerator {

    public void writePdfToStream(String xhtml, String url, OutputStream outputStream) throws Exception {
        // render at 300 dpi (see http://stackoverflow.com/questions/20495092/flying-saucer-set-custom-dpi-for-output-pdf)
        ITextRenderer renderer = new ITextRenderer(4.1666f, 1);
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.parse(new ByteArrayInputStream(xhtml.getBytes(Charset.forName("UTF-8"))));
        renderer.setDocument(doc, url);
        renderer.layout();
        renderer.createPDF(outputStream);
    }

}
