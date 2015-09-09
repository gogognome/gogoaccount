package nl.gogognome.gogoaccount.gui.dialogs;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.businessobjects.JournalItem;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Table model for journal items.
 */
public class ItemsTableModel extends AbstractListTableModel<JournalItem> {

    private final Logger logger = LoggerFactory.getLogger(ItemsTableModel.class);
    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

	private static final long serialVersionUID = 1L;

	private final static ColumnDefinition ACCOUNT =
		new ColumnDefinition("gen.account", String.class, 300);

	private final static ColumnDefinition DEBET =
		new ColumnDefinition.Builder("gen.debet", String.class, 100)
			.add(new RightAlignedRenderer()).build();

	private final static ColumnDefinition CREDIT =
		new ColumnDefinition.Builder("gen.credit", String.class, 100)
			.add(new RightAlignedRenderer()).build();

	private final static ColumnDefinition INVOICE =
		new ColumnDefinition("gen.invoice", String.class, 300);

	private final static List<ColumnDefinition> COLUMN_DEFINTIIONS =
		Arrays.asList(ACCOUNT, DEBET, CREDIT, INVOICE);

    private final Document document;

    public ItemsTableModel(Document document) {
    	super(COLUMN_DEFINTIIONS, Collections.<JournalItem>emptyList());
        this.document = document;
    }

    public void setJournalItems(JournalItem[] itemsArray) {
    	replaceRows(Arrays.asList(itemsArray));
    }

    @Override
	public Object getValueAt(int row, int col) {
    	ColumnDefinition colDef = COLUMN_DEFINTIIONS.get(col);
        AmountFormat af = Factory.getInstance(AmountFormat.class);
        String result = null;
        JournalItem item = getRow(row);

        if (ACCOUNT == colDef) {
            result = item.getAccount().getId() + " " + item.getAccount().getName();
        } else if (DEBET == colDef) {
            result = item.isDebet() ? af.formatAmountWithoutCurrency(item.getAmount()) : "" ;
        } else if (CREDIT == colDef) {
            result = item.isCredit() ? af.formatAmountWithoutCurrency(item.getAmount()) : "" ;
        } else if (INVOICE == colDef) {
            if (item.getInvoiceId() != null) {
                try {
                    Invoice invoice = invoiceService.getInvoice(document, item.getInvoiceId());
                    Party party = partyService.getParty(document, invoice.getPayingPartyId());
                    result = invoice != null ? invoice.getId() + " (" + party.getId() + " - " + party.getName() + ")" : "";
                } catch (ServiceException e) {
                    logger.warn("Ignroed exception: " + e.getMessage(), e);
                    result = "???";
                }
            }
        }
        return result;
    }

}