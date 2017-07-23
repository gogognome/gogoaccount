package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.businessobjects.Report.LedgerLine;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.ListTableModel;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * This class implements a model for a <code>JTable</code> that shows an overview
 * of an account at a specific date.
 */
public class AccountOverviewTableModel extends ListTableModel<AccountOverviewTableModel.LineInfo> {

    private static final long serialVersionUID = 1L;

    private final PartyService partyService;

    private final static ColumnDefinition<AccountOverviewTableModel.LineInfo> DATE =
            new ColumnDefinition<>("gen.date", Date.class, 75);

    private final static ColumnDefinition<AccountOverviewTableModel.LineInfo> ID =
            new ColumnDefinition<>("gen.id", String.class, 100);

    private final static ColumnDefinition<AccountOverviewTableModel.LineInfo> DESCRIPTION =
            new ColumnDefinition<>("gen.description", String.class, 200);

    private final static ColumnDefinition<AccountOverviewTableModel.LineInfo> DEBET =
        ColumnDefinition.<AccountOverviewTableModel.LineInfo>builder("gen.debet", String.class, 100)
    		.add(new RightAlignedRenderer()).build();

    private final static ColumnDefinition<AccountOverviewTableModel.LineInfo> CREDIT =
        ColumnDefinition.<AccountOverviewTableModel.LineInfo>builder("gen.credit", String.class, 100)
    		.add(new RightAlignedRenderer()).build();

    private final static ColumnDefinition<AccountOverviewTableModel.LineInfo> INVOICE =
            new ColumnDefinition<>("gen.invoice", String.class, 200);

    private final static List<ColumnDefinition<AccountOverviewTableModel.LineInfo>> COLUMN_DEFINITIONS = asList(
        DATE, ID, DESCRIPTION, DEBET, CREDIT, INVOICE
    );

    private Document document;
    private Report report;
    private Account account;

    /** This class contains the information to be shown in a single row of the table. */
    class LineInfo {
    	Date date;
    	String id;
        String description;
        Amount debet;
        Amount credit;
        String invoice;
    }

    public AccountOverviewTableModel(PartyService partyService) {
        super(COLUMN_DEFINITIONS, Collections.<LineInfo>emptyList());
        this.partyService = partyService;
    }

    public void setAccountAndDate(Document document, Report report, Account account) throws ServiceException {
    	this.report = report;
        this.account = account;
        this.document = document;
        clear();
        initializeValues();
    }

    private void initializeValues() throws ServiceException {
        if (document != null && account != null && report != null) {
        	for (LedgerLine line : report.getLedgerLinesForAccount(account)) {
	            LineInfo lineInfo = new LineInfo();
	            lineInfo.date = line.date;
	            lineInfo.id = line.id;
	            lineInfo.description = line.description;
	            lineInfo.debet = line.debetAmount;
	            lineInfo.credit = line.creditAmount;
                lineInfo.invoice = createInvoiceText(line.invoice);
	            addRow(lineInfo);
	        }
        }
    }

	private String createInvoiceText(Invoice invoice) throws ServiceException {
    	StringBuilder sb = new StringBuilder(100);
        if (invoice != null) {
            sb.append(invoice.getId());
            sb.append(" (").append(partyService.getParty(document, invoice.getPartyId()).getName()).append(")");
        }
		return sb.toString();
	}

    @Override
	public Object getValueAt(int row, int column) {
    	ColumnDefinition col = getColumnDefinition(column);
        LineInfo lineInfo = getRow(row);
        if (DATE.equals(col)) {
        	return lineInfo.date;
        } else if (ID.equals(col)) {
        	return lineInfo.id;
        } else if (ID.equals(col)) {
        	return lineInfo.id;
        } else if (DESCRIPTION.equals(col)) {
        	return lineInfo.description;
        } else if (DEBET.equals(col)) {
        	if (lineInfo.debet != null) {
        		return Factory.getInstance(AmountFormat.class).formatAmount(lineInfo.debet.toBigInteger());
        	}
        } else if (CREDIT.equals(col)) {
        	if (lineInfo.credit != null) {
        		return Factory.getInstance(AmountFormat.class).formatAmount(lineInfo.credit.toBigInteger());
        	}
        } else if (INVOICE.equals(col)) {
        	return lineInfo.invoice;
        }
        return null;
    }
}
