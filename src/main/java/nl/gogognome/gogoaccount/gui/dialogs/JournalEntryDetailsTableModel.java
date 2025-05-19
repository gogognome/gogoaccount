package nl.gogognome.gogoaccount.gui.dialogs;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntryDetail;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.ListTableModel;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JournalEntryDetailsTableModel extends ListTableModel<JournalEntryDetail> {

    private final Logger logger = LoggerFactory.getLogger(JournalEntryDetailsTableModel.class);

    // TODO: Get rid of these services.
    private final ConfigurationService configurationService;
    private final InvoiceService invoiceService;
    private final PartyService partyService;

	@Serial
	private static final long serialVersionUID = 1L;

	private final static ColumnDefinition<JournalEntryDetail> ACCOUNT =
		new ColumnDefinition<JournalEntryDetail>("gen.account", String.class, 300);

	private final static ColumnDefinition<JournalEntryDetail> DEBET =
		ColumnDefinition.<JournalEntryDetail>builder("gen.debit", String.class, 100)
			.add(new RightAlignedRenderer()).build();

	private final static ColumnDefinition<JournalEntryDetail> CREDIT =
		ColumnDefinition.<JournalEntryDetail>builder("gen.credit", String.class, 100)
			.add(new RightAlignedRenderer()).build();

	private final static ColumnDefinition<JournalEntryDetail> INVOICE =
		new ColumnDefinition<JournalEntryDetail>("gen.invoice", String.class, 300);

	private final static List<ColumnDefinition<JournalEntryDetail>> COLUMN_DEFINTIIONS =
		Arrays.asList(ACCOUNT, DEBET, CREDIT, INVOICE);

    private final Document document;

    public JournalEntryDetailsTableModel(Document document, ConfigurationService configurationService, InvoiceService invoiceService, PartyService partyService) {
    	super(COLUMN_DEFINTIIONS, Collections.emptyList());
        this.document = document;
        this.configurationService = configurationService;
        this.invoiceService = invoiceService;
        this.partyService = partyService;
    }

    // TODO: replace parameter by list of objects that contain all details to be shown so that no service calls are needed by this class.
    public void setJournalEntryDetails(List<JournalEntryDetail> journalEntryDetails) {
    	setRows(journalEntryDetails);
    }

    @Override
	public Object getValueAt(int row, int col) {
    	ColumnDefinition<?> colDef = COLUMN_DEFINTIIONS.get(col);
        AmountFormat af = Factory.getInstance(AmountFormat.class);
        String result = null;
        JournalEntryDetail journalEntryDetail = getRow(row);

        if (ACCOUNT == colDef) {
            try {
                Account account = configurationService.getAccount(document, journalEntryDetail.getAccountId());
                result = account.getId() + " " + account.getName();
            } catch (ServiceException e) {
                logger.warn("Ignored exception: " + e.getMessage(), e);
                result = "???";
            }
        } else if (DEBET == colDef) {
            result = journalEntryDetail.isDebet() ? af.formatAmountWithoutCurrency(journalEntryDetail.getAmount().toBigInteger()) : "" ;
        } else if (CREDIT == colDef) {
            result = journalEntryDetail.isCredit() ? af.formatAmountWithoutCurrency(journalEntryDetail.getAmount().toBigInteger()) : "" ;
        } else if (INVOICE == colDef) {
            if (journalEntryDetail.getInvoiceId() != null) {
                try {
                    Invoice invoice = invoiceService.getInvoice(document, journalEntryDetail.getInvoiceId());
                    Party party = partyService.getParty(document, invoice.getPartyId());
                    result = invoice != null ? invoice.getId() + " (" + party.getId() + " - " + party.getName() + ")" : "";
                } catch (ServiceException e) {
                    logger.warn("Ignored exception: " + e.getMessage(), e);
                    result = "???";
                }
            }
        }
        return result;
    }

}