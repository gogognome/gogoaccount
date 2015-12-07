package nl.gogognome.gogoaccount.services;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.AccountType;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentService;
import nl.gogognome.gogoaccount.component.importer.ImportBankStatementService;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.database.DocumentModificationFailedException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.StringUtil;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class reads the contents of a <code>Database</code> from an XML file.
 */
public class XMLFileReader {

    private final static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");

    private final ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
    private final ImportBankStatementService importBankStatementService = ObjectFactory.create(ImportBankStatementService.class);
    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final LedgerService ledgerService = ObjectFactory.create(LedgerService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

    private Document document;
    private String fileVersion;
    private final File file;

    public XMLFileReader(File file) {
        this.file = file;
    }

    /**
     * Creates a <tt>Database</tt> from a file.
     * @return a <tt>Database</tt> with the contents of the file.
     * @throws ServiceException if a problem occurs
     */
    public Document createDatabaseFromFile() throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            int highestPaymentId = 0;

            String path = file.getAbsolutePath();
            int indexOfExtension = path.lastIndexOf('.');
            if (indexOfExtension != -1) {
                path = path.substring(0, indexOfExtension);
            }
            DocumentService documentService = ObjectFactory.create(DocumentService.class);
            document = documentService.createNewDocument(path, "New bookkeeping");
            document.setFileName(file.getAbsolutePath());

            DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFac.newDocumentBuilder();
            org.w3c.dom.Document doc = docBuilder.parse(file);
            Element rootElement = doc.getDocumentElement();

            fileVersion = rootElement.getAttribute("fileversion");

            String description = rootElement.getAttribute("description");

            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            bookkeeping.setDescription(description);
            bookkeeping.setCurrency(Currency.getInstance(rootElement.getAttribute("currency")));
            bookkeeping.setStartOfPeriod(DATE_FORMAT.parse(rootElement.getAttribute("startdate")));
            configurationService.updateBookkeeping(document, bookkeeping);

            AmountFormat amountFormat = new AmountFormat(Locale.US, bookkeeping.getCurrency());

            // parse accounts
            if (fileVersion == null || fileVersion.equals("1.0")) {
                parseAccountsBeforeVersion2_2(rootElement);
            } else {
                parseAccountsForVersion2_2(rootElement);
            }

            parseAndAddParties(rootElement.getElementsByTagName("parties"));

            parseAndAddInvoices(rootElement.getElementsByTagName("invoices"), amountFormat);

            parseAndAddJournals(highestPaymentId, rootElement, amountFormat);

            parseAndAddImportedAccounts(rootElement.getElementsByTagName("importedaccounts"));

            return document;
        });
    }

    private void parseAccountsForVersion2_2(Element rootElement) throws ServiceException {
        NodeList nodes = rootElement.getElementsByTagName("accounts");
        for (int i=0; i<nodes.getLength(); i++)
        {
            Element elem = (Element)nodes.item(i);
            NodeList accountNodes = elem.getElementsByTagName("account");
            for (int j=0; j<accountNodes.getLength(); j++)
            {
                Element accountElem = (Element)accountNodes.item(j);
                String id = accountElem.getAttribute("id");
                String name = accountElem.getAttribute("name");
                AccountType type = AccountType.valueOf(accountElem.getAttribute("type"));
                configurationService.createAccount(document, new Account(id, name, type));
            }
        }
    }

    private void parseAccountsBeforeVersion2_2(Element rootElement) throws ServiceException {
        List<Account> assets = parseAccounts(rootElement.getElementsByTagName("assets"), AccountType.ASSET);
        for (Account account : assets) {
            configurationService.createAccount(document, account);
        }

        List<Account> liabilities = parseAccounts(rootElement.getElementsByTagName("liabilities"), AccountType.LIABILITY);
        for (Account account : liabilities) {
            configurationService.createAccount(document, account);
        }

        List<Account> expenses = parseAccounts(rootElement.getElementsByTagName("expenses"), AccountType.EXPENSE);
        for (Account account : expenses) {
            configurationService.createAccount(document, account);
        }

        List<Account> revenues = parseAccounts(rootElement.getElementsByTagName("revenues"), AccountType.REVENUE);
        for (Account account : revenues) {
            configurationService.createAccount(document, account);
        }
    }

    private void parseAndAddJournals(int highestPaymentId, Element rootElement, AmountFormat amountFormat)
            throws ParseException, DocumentModificationFailedException, ServiceException {
        NodeList journalsNodes = rootElement.getElementsByTagName("journals");
        for (int i=0; i<journalsNodes.getLength(); i++) {
            Element elem = (Element)journalsNodes.item(i);
            NodeList journalNodes = elem.getElementsByTagName("journal");
            for (int j=0; j<journalNodes.getLength(); j++) {
                Element journalElem = (Element)journalNodes.item(j);
                JournalEntry journalEntry = new JournalEntry();
                journalEntry.setId(journalElem.getAttribute("id"));
                String dateString = journalElem.getAttribute("date");
                journalEntry.setDate(DATE_FORMAT.parse(dateString));
                String idOfCreatedInvoice = journalElem.getAttribute("createdInvoice");
                if (idOfCreatedInvoice.length() == 0) {
                    idOfCreatedInvoice = null;
                }
                journalEntry.setIdOfCreatedInvoice(idOfCreatedInvoice);

                journalEntry.setDescription(journalElem.getAttribute("description"));
                NodeList itemNodes = journalElem.getElementsByTagName("item");
                ArrayList<JournalEntryDetail> journalEntryDetails = new ArrayList<>();
                for (int k=0; k<itemNodes.getLength(); k++) {
                    Element itemElem = (Element)itemNodes.item(k);
                    String accountId = itemElem.getAttribute("id");
                    String amountString = itemElem.getAttribute("amount");
                    Amount amount = new Amount(amountFormat.parse(amountString));
                    String side = itemElem.getAttribute("side");
                    String invoiceId = itemElem.getAttribute("invoice");
                    if (invoiceId.length() == 0) {
                        invoiceId = null;
                    }
                    String paymentId = itemElem.getAttribute("payment");
                    if (paymentId.length() == 0) {
                        paymentId = null;
                    } else {
                        if (paymentId.matches("p\\d+")) {
                            int pid = Integer.parseInt(paymentId.substring(1));
                            highestPaymentId = Math.max(highestPaymentId, pid);
                        }
                    }

                    JournalEntryDetail journalEntryDetail = new JournalEntryDetail();
                    journalEntryDetail.setAmount(amount);
                    journalEntryDetail.setAccountId(accountId);
                    journalEntryDetail.setDebet("debet".equals(side));
                    journalEntryDetail.setInvoiceId(invoiceId);
                    journalEntryDetail.setPaymentId(paymentId);
                    journalEntryDetails.add(journalEntryDetail);
                }

                ledgerService.addJournalEntry(document, journalEntry, journalEntryDetails, false);
            }
        }
    }

    private List<Account> parseAccounts(NodeList nodes, AccountType type) {
        ArrayList<Account> accounts = new ArrayList<>();
        for (int i=0; i<nodes.getLength(); i++)
        {
            Element elem = (Element)nodes.item(i);
            NodeList accountNodes = elem.getElementsByTagName("account");
            for (int j=0; j<accountNodes.getLength(); j++)
            {
                Element accountElem = (Element)accountNodes.item(j);
                String id = accountElem.getAttribute("id");
                String name = accountElem.getAttribute("name");
                accounts.add(new Account(id, name, type));
            }
        }
        return accounts;
    }

    /**
     * Parses parties.
     * @param nodes a node list containing parties.
     */
    private void parseAndAddParties(NodeList nodes) throws XMLParseException, ServiceException {
        for (int i=0; i<nodes.getLength(); i++) {
            Element elem = (Element)nodes.item(i);
            NodeList partyNodes = elem.getElementsByTagName("party");
            for (int j=0; j<partyNodes.getLength(); j++) {
                Element partyElem = (Element)partyNodes.item(j);
                Party party = new Party(partyElem.getAttribute("id"));
                party.setName(partyElem.getAttribute("name"));
                party.setAddress(emptyToNull(partyElem.getAttribute("address")));
                party.setZipCode(emptyToNull(partyElem.getAttribute("zip")));
                party.setCity(emptyToNull(partyElem.getAttribute("city")));
                party.setType(emptyToNull(partyElem.getAttribute("type")));
                party.setRemarks(emptyToNull(partyElem.getAttribute("remarks")));

                String birthDateString = partyElem.getAttribute("birthdate");
                if (birthDateString.length() > 0) {
                    try {
                        party.setBirthDate(DATE_FORMAT.parse(birthDateString));
                    } catch (java.text.ParseException e) {
                        throw new XMLParseException("Invalid birth date: \"" + birthDateString + "\"");
                    }
                }
                partyService.createParty(document, party);
            }
        }
    }

    private void parseAndAddInvoices(NodeList nodes, AmountFormat amountFormat) throws XMLParseException, ServiceException {
        for (int i=0; i<nodes.getLength(); i++) {
            Element elem = (Element)nodes.item(i);
            NodeList invoiceNodes = elem.getElementsByTagName("invoice");
            for (int j=0; j<invoiceNodes.getLength(); j++) {
                Element invoiceElem = (Element)invoiceNodes.item(j);
                String id = invoiceElem.getAttribute("id");
                Amount amountToBePaid;
                try {
                    amountToBePaid = new Amount(amountFormat.parse(invoiceElem.getAttribute("amountToBePaid")));
                } catch (ParseException e) {
                    throw new XMLParseException("Invalid amount: " + invoiceElem.getAttribute("amountToBePaid"));
                }

                String concerningPartyId = invoiceElem.getAttribute("concerningParty");
                if (!partyService.existsParty(document, concerningPartyId)) {
                    throw new XMLParseException("No (valid) concerning party specified for the invoice \"" + id + "\"");
                }

                String payingPartyId = invoiceElem.getAttribute("payingParty");
                if (payingPartyId != null && !partyService.existsParty(document, payingPartyId)) {
                    throw new XMLParseException("No (valid) paying party specified for the invoice \"" + id + "\"");
                }

                NodeList lineNodes = invoiceElem.getElementsByTagName("line");
                int numNodes = lineNodes.getLength();
                List<String> descriptions = new ArrayList<>(numNodes);
                List<Amount> amounts = new ArrayList<>(numNodes);
                for (int l=0; l<numNodes; l++) {
                    Element lineElem = (Element)lineNodes.item(l);
                    descriptions.add(lineElem.getAttribute("description"));
                    String amountString = lineElem.getAttribute("amount");
                    if (amountString != null && amountString.length() > 0) {
                        try {
                            amounts.add(new Amount(amountFormat.parse(amountString)));
                        } catch (ParseException e) {
                            throw new XMLParseException("Invalid amount: " + amountString);
                        }
                    } else {
                        amounts.add(null);
                    }
                }

                Date issueDate;
                try {
                    issueDate = DATE_FORMAT.parse(invoiceElem.getAttribute("issueDate"));
                } catch (ParseException e2) {
                    throw new XMLParseException("Invalid date: " + invoiceElem.getAttribute("issueDate"));
                }

                NodeList paymentNodes = invoiceElem.getElementsByTagName("payment");
                numNodes = paymentNodes.getLength();
                List<Payment> payments = new ArrayList<>(numNodes);
                for (int p=0; p<numNodes; p++) {
                    Element paymentElem = (Element)paymentNodes.item(p);
                    String paymentId;
                    paymentId = paymentElem.getAttribute("id");
                    if (StringUtil.isNullOrEmpty(paymentId)) {
                        paymentId = "p" + id + "-" + p;
                    }

                    Payment payment = new Payment(paymentId);
                    payment.setInvoiceId(id);
                    try {
                        payment.setDate(DATE_FORMAT.parse(paymentElem.getAttribute("date")));
                    } catch (ParseException e1) {
                        throw new XMLParseException("Invalid date: " + paymentElem.getAttribute("date"));
                    }
                    try {
                        payment.setAmount(new Amount(amountFormat.parse(paymentElem.getAttribute("amount"))));
                    } catch (ParseException e) {
                        throw new XMLParseException("Invalid amount: " + paymentElem.getAttribute("amount"));
                    }
                    payment.setDescription(paymentElem.getAttribute("description"));
                    payments.add(payment);
                }

                Invoice invoice = new Invoice(id);
                invoice.setPayingPartyId(payingPartyId);
                invoice.setConcerningPartyId(concerningPartyId);
                invoice.setAmountToBePaid(amountToBePaid);
                invoice.setIssueDate(issueDate);
                invoiceService.createInvoice(document, invoice);
                invoiceService.createDetails(document, invoice, descriptions, amounts);
                invoiceService.createPayments(document, payments);
            }
        }
    }

    private static String emptyToNull(String s) {
        return s == null || !s.isEmpty() ? s : null;
    }

    private void parseAndAddImportedAccounts(NodeList nodes) throws ServiceException {
        if (StringUtil.isNullOrEmpty(fileVersion)) {
            return;
        }

        for (int i=0; i<nodes.getLength(); i++) {
            Element elem = (Element)nodes.item(i);
            NodeList mappingNodes = elem.getElementsByTagName("mapping");
            for (int j=0; j<mappingNodes.getLength(); j++) {
                Element element = (Element) mappingNodes.item(j);
                String importedAccount = element.getAttribute("importedaccount");
                String accountId = element.getAttribute("account");
                importBankStatementService.setAccountForImportedAccount(document, importedAccount, null, accountId);
            }
        }
    }

}
