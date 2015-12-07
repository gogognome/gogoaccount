package nl.gogognome.gogoaccount.gui.views;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class TransactionsJournalsTableModel extends AbstractListTableModel<Transaction> {

    private final Logger logger = LoggerFactory.getLogger(TransactionsJournalsTableModel.class);
    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

	private final Document document;

    private final static ColumnDefinition DATE =
        new ColumnDefinition("importBankStatementView.date", Date.class, 70);

    private final static ColumnDefinition TO_ACCOUNT =
        new ColumnDefinition("importBankStatementView.toAccount", String.class, 100);

    private final static ColumnDefinition FROM_ACCOUNT =
        new ColumnDefinition("importBankStatementView.fromAccount", String.class, 100);

    private final static ColumnDefinition AMOUNT =
        new ColumnDefinition("importBankStatementView.amount", String.class, 100);

    private final static ColumnDefinition DESCRIPTION1 =
        new ColumnDefinition("importBankStatementView.description1", String.class, 200);

    private final static ColumnDefinition ID =
        new ColumnDefinition("importBankStatementView.id", String.class, 50);

    private final static ColumnDefinition DESCRIPTION2 =
        new ColumnDefinition("importBankStatementView.description2", String.class, 200);

    private final static ColumnDefinition INVOICE =
        new ColumnDefinition("importBankStatementView.invoice", String.class, 200);

    private final static List<ColumnDefinition> COLUMN_DEFINITIONS = Arrays.asList(
        DATE, TO_ACCOUNT, FROM_ACCOUNT, AMOUNT, DESCRIPTION1, ID, DESCRIPTION2, INVOICE
    );

    public TransactionsJournalsTableModel(List<Transaction> transactions, Document document) {
        super(COLUMN_DEFINITIONS, transactions);
        this.document = document;
    }

    /** {@inheritDoc} */
    @Override
	public Object getValueAt(int rowIndex, int columnIndex) {
        ColumnDefinition colDef = getColumnDefinition(columnIndex);
        if (DATE.equals(colDef)) {
            return getRow(rowIndex).getImportedTransaction().getDate();
        } else if (FROM_ACCOUNT.equals(colDef)) {
            return getRow(rowIndex).getImportedTransaction().getFromAccount();
        } else if (TO_ACCOUNT.equals(colDef)) {
            return getRow(rowIndex).getImportedTransaction().getToAccount();
        } else if (AMOUNT.equals(colDef)) {
        	AmountFormat af = Factory.getInstance(AmountFormat.class);
            return af.formatAmount(getRow(rowIndex).getImportedTransaction().getAmount().toBigInteger());
        } else if (DESCRIPTION1.equals(colDef)) {
            return getRow(rowIndex).getImportedTransaction().getDescription();
        } else if (ID.equals(colDef)) {
            return getRow(rowIndex).getJournalEntry() != null ? getRow(rowIndex).getJournalEntry().getId() : null;
        } else if (DESCRIPTION2.equals(colDef)) {
            return getRow(rowIndex).getJournalEntry() != null ? getRow(rowIndex).getJournalEntry().getDescription() : null;
        } else if (INVOICE.equals(colDef)) {
        	JournalEntry journalEntry = getRow(rowIndex).getJournalEntry();
            if (journalEntry != null && journalEntry.getIdOfCreatedInvoice() != null) {
                try {
                    Invoice invoice = invoiceService.getInvoice(document, journalEntry.getIdOfCreatedInvoice());
                    Party party = partyService.getParty(document, invoice.getConcerningPartyId());
                    return invoice.getId() + " (" + party.getName() + ")";
                } catch (ServiceException e) {
                    logger.warn("Ignored exception: " + e.getMessage(), e);
                    return "???";
                }
            }
        }
        return null;
    }

}