package nl.gogognome.gogoaccount.component.directdebit;

import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.StringUtil;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

class SepaFileGenerator {

    private final DateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    private final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    private final IbanValidator ibanValidator = new IbanValidator();

    private final Document document;
    private final ConfigurationService configurationService;

    private AmountFormat amountFormat;

    SepaFileGenerator(Document document, ConfigurationService configurationService) {
        this.document = document;
        this.configurationService = configurationService;
    }

    void generate(DirectDebitSettings settings, List<Invoice> invoices, File fileToCreate,
                  Date collectionDate, Map<String, Party> idToParty,
                  Map<String, PartyDirectDebitSettings> idToPartyDirectDebitSettings)
            throws Exception {
        Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
        amountFormat = new AmountFormat(Locale.US, bookkeeping.getCurrency()); // must use US locale. Therefore, amount format cannot be injected

        DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docBuilderFac.newDocumentBuilder();
        org.w3c.dom.Document doc = docBuilder.newDocument();
        Element root = doc.createElementNS("urn:iso:std:iso:20022:tech:xsd:pain.008.001.02", "Document");
        doc.appendChild(root);

        Element cstmrDrctDbtInitn = addElement(doc, root, "CstmrDrctDbtInitn");

        Element groupHeader = addElement(doc, cstmrDrctDbtInitn, "GrpHdr");
        addElement(doc, groupHeader, "MsgId", settings.getSequenceNumber());
        addElement(doc, groupHeader, "CreDtTm", dateTimeFormat.format(new Date()));
        addElement(doc, groupHeader, "NbOfTxs", Integer.toString(invoices.size()));
        Amount totalAmount = invoices.stream().map(Invoice::getAmountToBePaid).reduce(Amount.ZERO, (a, b) -> a.add(b));
        String formattedAmount = amountFormat.formatAmountWithoutCurrency(totalAmount.toBigInteger());
        addElement(doc, groupHeader, "CtrlSum", formattedAmount);
        addElement(doc, groupHeader, "InitgPty/Nm", bookkeeping.getOrganizationName());

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

        addElement(doc, paymentInformationIdentification, "ReqdColltnDt", dateFormat.format(collectionDate));

        Element creditor = addElement(doc, paymentInformationIdentification, "Cdtr");
        addElement(doc, creditor, "Nm", bookkeeping.getOrganizationName());

        Element postalAddress = addElement(doc, creditor, "PstlAdr");
        addElement(doc, postalAddress, "Ctry", bookkeeping.getOrganizationCountry());
        if (!StringUtil.isNullOrEmpty(bookkeeping.getOrganizationAddress())) {
            addElement(doc, postalAddress, "AdrLine", bookkeeping.getOrganizationAddress());
        }
        String zipCodeAndCity = (StringUtil.nullToEmptyString(bookkeeping.getOrganizationZipCode()) + ' '
                + StringUtil.nullToEmptyString(bookkeeping.getOrganizationCity())).trim();
        if (!StringUtil.isNullOrEmpty(zipCodeAndCity)) {
            addElement(doc, postalAddress, "AdrLine", zipCodeAndCity);
        }

        Element creditorAccount = addElement(doc, paymentInformationIdentification, "CdtrAcct");
        Element id = addElement(doc, creditorAccount, "Id");
        addElement(doc, id, "IBAN", settings.getIban());
        addElement(doc, creditorAccount, "Ccy", bookkeeping.getCurrency().getCurrencyCode());

        addElement(doc, paymentInformationIdentification, "CdtrAgt/FinInstnId/BIC", settings.getBic());

        addElement(doc, paymentInformationIdentification, "ChrgBr", "SLEV");

        for (Invoice invoice : invoices) {
            Party party = idToParty.get(invoice.getPartyId());
            PartyDirectDebitSettings partyDirectDebitSettings = idToPartyDirectDebitSettings.get(invoice.getPartyId());
            if (partyDirectDebitSettings == null) {
                throw new ServiceException("Party " + party.toString() + " has no direct debit settings. " +
                        "Those settings are required to generate a SEPA file");
            }
            addInvoiceElements(doc, paymentInformationIdentification, invoice, party, partyDirectDebitSettings, settings,
                    invoice.getDescription());
        }

        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
        try (FileOutputStream outputStream = new FileOutputStream(fileToCreate)) {
            StreamResult streamResult = new StreamResult(outputStream);
            transformer.transform(new DOMSource(doc), streamResult);
        }
    }

    private void addInvoiceElements(org.w3c.dom.Document doc, Element parent, Invoice invoice, Party party,
                                    PartyDirectDebitSettings partyDirectDebitSettings, DirectDebitSettings settings,
                                    String description) throws ServiceException {
        Element ddTransactionInformation = addElement(doc, parent, "DrctDbtTxInf");
        addElement(doc, ddTransactionInformation, "PmtId/EndToEndId", toAlphaNumerical(invoice.getId(), 35));

        if (!invoice.getAmountToBePaid().isPositive()) {
            throw new ServiceException("Amount to be paid must be positive.");
        }
        Element instructedAmount = addElement(doc, ddTransactionInformation, "InstdAmt",
                amountFormat.formatAmountWithoutCurrency(invoice.getAmountToBePaid().toBigInteger()));
        instructedAmount.setAttribute("Ccy", configurationService.getBookkeeping(document).getCurrency().getCurrencyCode());

        Element directDebitTransaction = addElement(doc, ddTransactionInformation, "DrctDbtTx");
        Element mandateRelatedInformation = addElement(doc, directDebitTransaction, "MndtRltdInf");
        addElement(doc, mandateRelatedInformation, "MndtId", toAlphaNumerical(party.getId(), 35));
        if (partyDirectDebitSettings.getMandateDate() == null) {
            throw new IllegalArgumentException("Party " + party.toString() + " has no mandate date specified, " +
                    "which is required to generate a SEPA file");
        }
        addElement(doc, mandateRelatedInformation, "DtOfSgntr", dateFormat.format(partyDirectDebitSettings.getMandateDate()));
        addElement(doc, mandateRelatedInformation, "AmdmntInd", "false");

        Element other = addElement(doc, directDebitTransaction, "CdtrSchmeId/Id/PrvtId/Othr");
        addElement(doc, other, "Id", toAlphaNumerical(settings.SepaDirectDebitContractNumber(), 35));
        addElement(doc, other, "SchmeNm/Prtry", "SEPA");

        addElement(doc, ddTransactionInformation, "DbtrAgt/FinInstnId/Othr/Id", "NOTPROVIDED");

        Element debtor = addElement(doc, ddTransactionInformation, "Dbtr");
        addElement(doc, debtor, "Nm", partyDirectDebitSettings.getName());
        Element postalAddress = addElement(doc, debtor, "PstlAdr");
        addElement(doc, postalAddress, "Ctry", partyDirectDebitSettings.getCountry());
        if (!StringUtil.isNullOrEmpty(partyDirectDebitSettings.getAddress())) {
            addElement(doc, postalAddress, "AdrLine", partyDirectDebitSettings.getAddress());
        }
        String zipCodeAndCity = (StringUtil.nullToEmptyString(party.getZipCode()) + ' '
                + StringUtil.nullToEmptyString(partyDirectDebitSettings.getCity())).trim();
        if (!StringUtil.isNullOrEmpty(zipCodeAndCity)) {
            addElement(doc, postalAddress, "AdrLine", zipCodeAndCity);
        }

        String iban = partyDirectDebitSettings.getIban();
        ibanValidator.validate(iban);
        addElement(doc, ddTransactionInformation, "DbtrAcct/Id/IBAN", iban);
        addElement(doc, ddTransactionInformation, "Purp/Cd", "OTHR");
        addElement(doc, ddTransactionInformation, "RmtInf/Ustrd", toAlphaNumerical(description, 140));
    }

    private Element addElement(org.w3c.dom.Document doc, Element element, String childElementName) {
        return addElement(doc, element, childElementName, null);
    }

    private Element addElement(org.w3c.dom.Document doc, Element element, String childElementName, Object textContent) {
        String[] childElementNames = childElementName.split("/");
        Element child = null;
        for (String name : childElementNames) {
            child = doc.createElement(name);
            element.appendChild(child);
            element = child;
        }
        if (child == null) {
            throw new IllegalArgumentException("Child element name must not be empty");
        }

        if (textContent != null) {
            child.setTextContent(textContent.toString());
        }
        return child;
    }

    private String toAlphaNumerical(String text, int maxLength) {
        StringBuilder sb = new StringBuilder(maxLength);
        for (char c : text.toCharArray()) {
            if ("abcdefghijklmnopqrstuvwxyz0123456789 ABCDEFGHIJKLMNOPQRSTUVWXYZ/-?:().,'+".indexOf(c) != -1) {
                sb.append(c);
            }
            if (sb.length() == maxLength) {
                break;
            }
        }
        return sb.toString();
    }

}
