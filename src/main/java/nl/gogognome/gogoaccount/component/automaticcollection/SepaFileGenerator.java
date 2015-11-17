package nl.gogognome.gogoaccount.component.automaticcollection;

import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Currency;
import java.util.Date;
import java.util.List;
import java.util.Locale;

class SepaFileGenerator {


    public void generate(Document document, AutomaticCollectionSettings settings, List<Invoice> invoices, File fileToCreate,
                         Date collectionDate)
            throws Exception {
        ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);

        DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFac.newDocumentBuilder();
        org.w3c.dom.Document doc = docBuilder.newDocument();
        Element root = doc.createElementNS("urn:iso:std:iso:20022:tech:xsd:pain.008.001.02", "Document");
        doc.appendChild(root);

        Element cstmrDrctDbtInitn = addElement(doc, root, "CstmrDrctDbtInitn");

        Element groupHeader = addElement(doc, cstmrDrctDbtInitn, "GrpHdr");
        addElement(doc, groupHeader, "MsgId", settings.getSequenceNumber());
        DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        addElement(doc, groupHeader, "CreDtTm", dateTimeFormat.format(new Date()));
        addElement(doc, groupHeader, "NbOfTxs", Integer.toString(invoices.size()));
        Amount totalAmount = invoices.stream().map(i -> i.getAmountToBePaid()).reduce(Amount.getZero(Currency.getInstance("EUR")), (a, b) -> a.add(b));
        AmountFormat amountFormat = new AmountFormat(Locale.US);
        String formattedAmount = amountFormat.formatAmountWithoutCurrency(totalAmount);
        addElement(doc, groupHeader, "CtrlSum", formattedAmount);

        Element paymentInformationIdentification = addElement(doc, cstmrDrctDbtInitn, "PmtInf");
        addElement(doc, paymentInformationIdentification, "PmtInfId", settings.getSequenceNumber());
        addElement(doc, paymentInformationIdentification, "PmtMtd", "DD");
        addElement(doc, paymentInformationIdentification, "BtchBookg", "true");
        addElement(doc, paymentInformationIdentification, "NbOfTxs", invoices.size());
        addElement(doc, paymentInformationIdentification, "CtrlSum", formattedAmount);
        Element paymentTypeInformation = addElement(doc, paymentInformationIdentification, "PmtTpInf");
        Element serviceLevel = addElement(doc, paymentTypeInformation, "SvcLvl");
        addElement(doc, serviceLevel, "Cd", "SEPA");

        Element localInstrument = addElement(doc, paymentTypeInformation, "LclInstrm");
        addElement(doc, localInstrument, "Cd", "CORE");

        addElement(doc, paymentTypeInformation, "SeqTp", "FRST");

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        addElement(doc, paymentInformationIdentification, "ReqdColltnDt", dateFormat.format(collectionDate));

        Element creditor = addElement(doc, paymentInformationIdentification, "Cdtr");
        Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
        addElement(doc, creditor, "Nm", bookkeeping.getOrganizationName());
        Element postalAddress = addElement(doc, creditor, "PstlAdr");

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        try (FileOutputStream outputStream = new FileOutputStream(fileToCreate)) {
            StreamResult streamResult = new StreamResult(outputStream);
            transformer.transform(new DOMSource(doc), streamResult);
        }
    }

    private Element addElement(org.w3c.dom.Document doc, Element element, String childElementName) {
        Element child = doc.createElement(childElementName);
        element.appendChild(child);
        return child;
    }

    private void addElement(org.w3c.dom.Document doc, Element element, String childElementName, Object textContent) {
        Element child = doc.createElement(childElementName);
        element.appendChild(child);
        child.setTextContent(textContent.toString());
    }

}
