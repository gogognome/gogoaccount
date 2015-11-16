package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.DateUtil;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class SepaFileGenerator {


    public void generate(Document document, AutomaticCollectionSettings settings, List<Invoice> invoices, File fileToCreate)
            throws Exception {
        ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);

        DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFac.newDocumentBuilder();
        org.w3c.dom.Document doc = docBuilder.newDocument();
        Element root = doc.createElementNS("urn:iso:std:iso:20022:tech:xsd:pain.008.001.02", "Document");
        doc.appendChild(root);

        Element cstmrDrctDbtInitn = doc.createElement("CstmrDrctDbtInitn");
        root.appendChild(cstmrDrctDbtInitn);

        Element groupHeader = doc.createElement("GrpHdr");
        cstmrDrctDbtInitn.appendChild(groupHeader);
        addElement(doc, groupHeader, "MsgId", Long.toString(settings.getSequenceNumber()));
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        addElement(doc, groupHeader, "CreDtTm", dateFormat.format(new Date()));
        addElement(doc, groupHeader, "NbOfTxs", Integer.toString(invoices.size()));
        Amount totalAmount = invoices.stream().map(i -> i.getAmountToBePaid()).reduce(Amount.getZero(Currency.getInstance("EUR")), (a, b) -> a.add(b));
        AmountFormat amountFormat = new AmountFormat(Locale.US);
        addElement(doc, groupHeader, "CtrlSum", amountFormat.formatAmountWithoutCurrency(totalAmount));

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        try (FileOutputStream outputStream = new FileOutputStream(fileToCreate)) {
            StreamResult streamResult = new StreamResult(outputStream);
            transformer.transform(new DOMSource(doc), streamResult);
        }
    }

    private void addElement(org.w3c.dom.Document doc, Element element, String childElementName, String textContent) {
        Element child = doc.createElement(childElementName);
        element.appendChild(child);
        child.setTextContent(textContent);
    }

}
