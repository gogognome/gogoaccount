package nl.gogognome.gogoaccount.gui.invoice;

import com.google.common.base.Joiner;
import nl.gogognome.gogoaccount.component.invoice.InvoiceOverview;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.RightAlignedRenderer;
import nl.gogognome.lib.util.DayOfYearComparator;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class InvoiceTableModel extends AbstractListTableModel<InvoiceOverview> {

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

    private Map<String, List<String>> partyIdToTags;

    public InvoiceTableModel() {
        super(COLUMN_DEFINITIONS);
    }

    public void replaceRows(List<InvoiceOverview> invoices) {
        super.replaceRows(invoices);
        this.partyIdToTags = partyIdToTags;
    }

    @Override
    public Object getValueAt(int row, int col) {
        Object result = null;
        Party party = getRow(row);

        ColumnDefinition colDef = COLUMN_DEFINITIONS.get(col);
        if (ID == colDef) {
            result = party.getId();
        } else if (NAME == colDef) {
            result = party.getName();
        } else if (ADDRESS == colDef) {
            result = party.getAddress();
        } else if (ZIP_CODE == colDef) {
            result = party.getZipCode();
        } else if (CITY == colDef) {
            result = party.getCity();
        } else if (BIRTH_DATE == colDef) {
            result = party.getBirthDate();
        } else if (TAGS == colDef) {
            result = Joiner.on(", ").join(partyIdToTags.get(party.getId()));
        } else if (REMARKS == colDef) {
            String remarks = party.getRemarks();
            if (remarks != null && remarks.length() > 30) {
                int size = Math.max(20, remarks.lastIndexOf(' ', 30));
                remarks = remarks.substring(0, size) + "...";
            }
            result = remarks;
        }

        return result;
    }

}
