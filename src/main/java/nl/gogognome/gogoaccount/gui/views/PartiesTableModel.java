package nl.gogognome.gogoaccount.gui.views;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.base.Joiner;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.ListTableModel;
import nl.gogognome.lib.util.DayOfYearComparator;

/**
 * The table model containing information about parties.
 */
public class PartiesTableModel extends ListTableModel<Party> {

    private Map<String, List<String>> partyIdToTags;

    public PartiesTableModel(List<Party> parties, Map<String, List<String>> partyIdToTags) {
        setColumnDefinitions(
                ColumnDefinition.<Party>builder("gen.id", String.class, 40)
                    .add(row -> row.getId())
                    .build(),
                ColumnDefinition.<Party>builder("gen.name", String.class, 200)
                    .add(row -> row.getName())
                    .build(),
                ColumnDefinition.<Party>builder("gen.address", String.class, 200)
                    .add(row -> row.getAddress())
                    .build(),
                ColumnDefinition.<Party>builder("gen.zipCode", String.class, 80)
                    .add(row -> row.getZipCode())
                    .build(),
                ColumnDefinition.<Party>builder("gen.city", String.class, 100)
                    .add(row -> row.getCity())
                    .build(),
                ColumnDefinition.<Party>builder("gen.emailAddress", String.class, 100)
                    .add(row -> row.getEmailAddress())
                    .build(),
                ColumnDefinition.<Party>builder("gen.birthDate", Date.class, 100)
                    .add(new DayOfYearComparator())
                    .add(row -> row.getBirthDate())
                    .build(),
                ColumnDefinition.<Party>builder("gen.tags", String.class, 100)
                    .add(row -> Joiner.on(", ").join(PartiesTableModel.this.partyIdToTags.get(row.getId())))
                    .build(),
                ColumnDefinition.<Party>builder("gen.remarks", String.class, 100)
                    .add(row -> { String remarks = row.getRemarks();
                            if (remarks != null && remarks.length() > 30) {
                                int size = Math.max(20, remarks.lastIndexOf(' ', 30));
                                remarks = remarks.substring(0, size) + "...";
                            }
                            return remarks; })
                    .build()
        );
        this.partyIdToTags = partyIdToTags;
        setRows(parties);
    }

    public void setRows(List<Party> parties, Map<String, List<String>> partyIdToTags) {
        super.setRows(parties);
        this.partyIdToTags = partyIdToTags;
    }

}
