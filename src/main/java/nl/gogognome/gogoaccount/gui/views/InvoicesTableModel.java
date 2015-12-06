package nl.gogognome.gogoaccount.gui.views;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.DayOfYearComparator;
import nl.gogognome.lib.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The table model that shows information about the invoices.
 */
class InvoicesTableModel extends AbstractListTableModel<Invoice> {

    private final Logger logger = LoggerFactory.getLogger(InvoicesTableModel.class);
	private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
	private final PartyService partyService = ObjectFactory.create(PartyService.class);

	private final static ColumnDefinition ID =
		new ColumnDefinition("gen.id", String.class, 40);

	private final static ColumnDefinition NAME =
		new ColumnDefinition("gen.name", String.class, 200);

	private final static ColumnDefinition SALDO =
		new ColumnDefinition("gen.saldo", String.class, 200);

	private final static ColumnDefinition DATE =
		new ColumnDefinition.Builder("gen.date", Date.class, 100)
			.add(new DayOfYearComparator()).build();

	private final static List<ColumnDefinition> COLUMN_DEFINITIONS =
		Arrays.asList(ID, NAME, SALDO, DATE);

	private Document document;

	public InvoicesTableModel(Document document) {
		super(COLUMN_DEFINITIONS, Collections.<Invoice>emptyList());
		this.document = document;
	}

	@Override
	public Object getValueAt(int row, int col) {
        Object result = null;
        try {
            Invoice invoice = getRow(row);
            ColumnDefinition colDef = COLUMN_DEFINITIONS.get(col);

            if (ID == colDef) {
                result = invoice.getId();
            } else if (NAME == colDef) {
                result = partyService.getParty(document, invoice.getPayingPartyId()).getName();
            } else if (SALDO == colDef) {
                result = Factory.getInstance(AmountFormat.class).formatAmount(
                        invoiceService.getRemainingAmountToBePaid(document, invoice.getId(), new Date()).toBigInteger());
            } else if (DATE == colDef) {
                result = invoice.getIssueDate();
            }
        } catch (ServiceException e) {
            logger.warn("Ignored exception: " + e.getMessage(), e);
            result = "???";
        }
		return result;
	}

}
