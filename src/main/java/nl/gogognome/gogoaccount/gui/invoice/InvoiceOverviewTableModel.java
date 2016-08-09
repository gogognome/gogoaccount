package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.invoice.InvoiceOverview;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.text.AmountFormat;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static java.util.Collections.emptyList;

public class InvoiceOverviewTableModel extends AbstractListTableModel<InvoiceOverview> {

    private final static ColumnDefinition ID =
            new ColumnDefinition("gen.id", String.class, 40);

    private final static ColumnDefinition DESCRIPTION =
            new ColumnDefinition("gen.description", String.class, 200);

    private final static ColumnDefinition ISSUE_DATE=
            new ColumnDefinition("gen.issueDate", Date.class, 100);

    private final static ColumnDefinition PARTY =
            new ColumnDefinition("gen.party", String.class, 200);

    private final static ColumnDefinition AMOUNT_TO_BE_PAID =
            new ColumnDefinition.Builder("gen.amountToBePaid", String.class, 100)
                    .add(new RightAlignedRenderer()).build();

    private final static ColumnDefinition AMOUNT_PAID =
            new ColumnDefinition.Builder("gen.amountPaid", String.class, 100)
                    .add(new RightAlignedRenderer()).build();

    private final static List<ColumnDefinition> COLUMN_DEFINITIONS =
            Arrays.asList(ID, DESCRIPTION, ISSUE_DATE, PARTY, AMOUNT_TO_BE_PAID, AMOUNT_PAID);

    private AmountFormat amountFormat;

    public InvoiceOverviewTableModel(AmountFormat amountFormat) {
        super(COLUMN_DEFINITIONS, emptyList());
        this.amountFormat = amountFormat;
    }

    public void replaceRows(List<InvoiceOverview> invoiceOverviews) {
        super.replaceRows(invoiceOverviews);
    }

    @Override
    public Object getValueAt(int row, int col) {
        Object result = null;
        InvoiceOverview invoiceOverview = getRow(row);

        ColumnDefinition colDef = COLUMN_DEFINITIONS.get(col);
        if (ID == colDef) {
            result = invoiceOverview.getInvoiceId();
        } else if (DESCRIPTION == colDef) {
            result = invoiceOverview.getDescription();
        } else if (ISSUE_DATE == colDef) {
            result = invoiceOverview.getIssueDate();
        } else if (PARTY == colDef) {
            result = invoiceOverview.getPartyId() + " - " + invoiceOverview.getPartyName();
        } else if (AMOUNT_TO_BE_PAID == colDef) {
            result = amountFormat.formatAmount(invoiceOverview.getAmountToBePaid().toBigInteger());
        } else if (AMOUNT_PAID == colDef) {
            result = amountFormat.formatAmount(invoiceOverview.getAmountPaid().toBigInteger());
        }

        return result;
    }

}
