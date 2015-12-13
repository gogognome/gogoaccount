package nl.gogognome.gogoaccount.gui.views;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.util.DayOfYearComparator;

/**
 * The table model containing information about parties.
 */
public class PartiesTableModel extends AbstractListTableModel<Party> {

    private final static ColumnDefinition ID =
        new ColumnDefinition("gen.id", String.class, 40);

    private final static ColumnDefinition NAME =
        new ColumnDefinition("gen.name", String.class, 200);

    private final static ColumnDefinition ADDRESS =
        new ColumnDefinition("gen.address", String.class, 200);

    private final static ColumnDefinition ZIP_CODE =
        new ColumnDefinition("gen.zipCode", String.class, 80);

    private final static ColumnDefinition CITY =
        new ColumnDefinition("gen.city", String.class, 100);

    private final static ColumnDefinition BIRTH_DATE =
        new ColumnDefinition.Builder("gen.birthDate", Date.class, 100)
            .add(new DayOfYearComparator()).build();

    private final static ColumnDefinition TAGS =
        new ColumnDefinition("gen.tags", String.class, 100);

    private final static ColumnDefinition REMARKS =
        new ColumnDefinition("gen.remarks", String.class, 100);

    private final static List<ColumnDefinition> COLUMN_DEFINITIONS =
        Arrays.asList(ID, NAME, ADDRESS, ZIP_CODE, CITY, BIRTH_DATE, TAGS, REMARKS);

    private Map<String, List<String>> partyIdToTags;

    public PartiesTableModel(List<Party> parties, Map<String, List<String>> partyIdToTags) {
        super(COLUMN_DEFINITIONS, parties);
        this.partyIdToTags = partyIdToTags;
    }

    public void replaceRows(List<Party> parties, Map<String, List<String>> partyIdToTags) {
        super.replaceRows(parties);
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
