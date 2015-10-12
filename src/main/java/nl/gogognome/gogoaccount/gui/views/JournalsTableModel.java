package nl.gogognome.gogoaccount.gui.views;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.event.TableModelListener;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentListener;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class implements a table model for a table containing journals.
 */
public class JournalsTableModel extends AbstractListTableModel<JournalEntry> implements DocumentListener {

    private final Logger logger = LoggerFactory.getLogger(JournalsTableModel.class);
    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

	private final static ColumnDefinition DATE =
		new ColumnDefinition("gen.date", Date.class, 200);

	private final static ColumnDefinition ID =
		new ColumnDefinition("gen.id", String.class, 200);

	private final static ColumnDefinition DESCRIPTION =
		new ColumnDefinition("gen.description", String.class, 500);

	private final static ColumnDefinition INVOICE =
		new ColumnDefinition("gen.invoice", String.class, 200);

	private final static List<ColumnDefinition> COLUMNS =
		Arrays.asList(DATE, ID, DESCRIPTION, INVOICE);

    /** Contains the <code>TableModelListener</code>s of this <code>TableModel</code>. */
    private ArrayList<TableModelListener> journalsTableModelListeners = new ArrayList<TableModelListener>();

    private Document document;

    /**
     * Constructor.
     * @param document the database from which to take the data
     */
    public JournalsTableModel(Document document) {
    	super(COLUMNS, document.getJournalEntries());
        this.document = document;
        document.addListener(this);
    }

    @Override
	public void documentChanged(Document document) {
        replaceRows(this.document.getJournalEntries());
    }

	@Override
	public Object getValueAt(int row, int col) {
        JournalEntry journalEntry = getRow(row);
        Object result = null;
        ColumnDefinition colDef = COLUMNS.get(col);
        if (DATE == colDef) {
            result = journalEntry.getDate();
        } else if (ID == colDef) {
            result = journalEntry.getId();
        } else if (DESCRIPTION == colDef) {
            result = journalEntry.getDescription();
        } else if (INVOICE == colDef) {
            if (journalEntry.getIdOfCreatedInvoice() != null) {
                try {
                    Invoice invoice = invoiceService.getInvoice(document, journalEntry.getIdOfCreatedInvoice());
                    Party party = partyService.getParty(document, invoice.getPayingPartyId());
                    result = invoice.getId() + " (" + party.getId() + " - " + party.getName() + ")";
                } catch (ServiceException e) {
                    logger.warn("Ignroed exception: " + e.getMessage(), e);
                    result = "???";
                }
            }
        }
        return result;
    }

}
