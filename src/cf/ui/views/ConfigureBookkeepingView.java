/*
 * $Id: ConfigureBookkeepingView.java,v 1.1 2009-12-01 19:23:08 sanderk Exp $
 */

package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.AbstractSortedTableModel;
import nl.gogognome.swing.ColumnDefinition;
import nl.gogognome.swing.SortedTable;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import cf.engine.Account;
import cf.engine.Database;

/**
 * In this view the user can configure the following aspects of the bookkeeping:
 * <ul>
 *   <li>description
 *   <li>start date
 *   <li>currency
 *   <li>accounts
 * </ul>
 *
 * @author Sander Kooijmans
 */
public class ConfigureBookkeepingView extends View {

    private Database database;

    private String enteredDescription;
    private DateModel startDateModel = new DateModel();
    private String enteredCurrency;
    private AccountTableModel tableModel;

    private JTextField tfDescription = new JTextField();
    private DateSelectionBean dsbStartDate = new DateSelectionBean(startDateModel);
    private JTextField tfCurrency = new JTextField();

    /**
     * Constructor.
     * @param database the database whose bookkeeping is to be configured.
     */
    public ConfigureBookkeepingView(Database database) {
        this.database = database;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle() {
        return TextResource.getInstance().getString("EditBookkeepingView.title");
    }

    /** {@inheritDoc} */
    @Override
    public void onClose() {
        enteredDescription = tfDescription.getText();
    }

    /** {@inheritDoc} */
    @Override
    public void onInit() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        TextResource tr = TextResource.getInstance();
        WidgetFactory wf = WidgetFactory.getInstance();

        // Create panel with general settings
        JPanel generalSettingsPanel = new JPanel(new GridBagLayout());
        generalSettingsPanel.setBorder(BorderFactory.createTitledBorder(
            tr.getString("ConfigureBookkeepingView.generalSettings")));

        int row = 0;
        tfDescription.setText(database.getDescription());
        generalSettingsPanel.add(wf.createLabel("ConfigureBookkeepingView.description", tfDescription),
            SwingUtils.createLabelGBConstraints(0, row));
        generalSettingsPanel.add(tfDescription,
            SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        startDateModel.setDate(database.getStartOfPeriod(), null);
        generalSettingsPanel.add(wf.createLabel("ConfigureBookkeepingView.startDate", dsbStartDate),
            SwingUtils.createLabelGBConstraints(0, row));
        generalSettingsPanel.add(dsbStartDate,
            SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfCurrency.setText(database.getCurrency().getCurrencyCode());
        tfCurrency.setColumns(4);
        generalSettingsPanel.add(wf.createLabel("ConfigureBookkeepingView.currency", tfCurrency),
            SwingUtils.createLabelGBConstraints(0, row));
        generalSettingsPanel.add(tfCurrency,
            SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        // Create panel with accounts table
        JPanel accountsPanel = new JPanel(new BorderLayout());
        accountsPanel.setBorder(BorderFactory.createTitledBorder(
            tr.getString("ConfigureBookkeepingView.accounts")));
        SortedTable table = wf.createSortedTable(tableModel);
        accountsPanel.add(table.getComponent(), BorderLayout.CENTER);

        // Add panels to view
        add(generalSettingsPanel, SwingUtils.createPanelGBConstraints(0, 0));
    }

    private static class AccountDefinition {
        public Account account;
        public boolean used;
    }

    private static class AccountTableModel extends AbstractSortedTableModel {

        private List<AccountDefinition> accountDefinitions;

        private final static ColumnDefinition NAME =
            new ColumnDefinition("ConfigureBookkeepingView.name", String.class, 200, null, null);

        private final static ColumnDefinition USED =
            new ColumnDefinition("ConfigureBookkeepingView.used", Icon.class, 20, null, null);

        private final static ColumnDefinition TYPE =
            new ColumnDefinition("ConfigureBookkeepingView.type", String.class, 100, null, null);

        private final static List<ColumnDefinition> COLUMN_DEFINITIONS = Arrays.asList(
            NAME, USED, TYPE
        );

        public AccountTableModel(List<AccountDefinition> accountDefinitions) {
            super(COLUMN_DEFINITIONS);
            this.accountDefinitions = accountDefinitions;
        }

        /** {@inheritDoc} */
        public int getRowCount() {
            return accountDefinitions.size();
        }

        /** {@inheritDoc} */
        public Object getValueAt(int rowIndex, int columnIndex) {
            ColumnDefinition colDef = getColumnDefinition(columnIndex);
            if (NAME.equals(colDef)) {
                return accountDefinitions.get(rowIndex).account.getName();
            } else if (USED.equals(colDef)) {
                return accountDefinitions.get(rowIndex).used ?
                    WidgetFactory.getInstance().createIcon("tick.png") : null;
            } else if (TYPE.equals(colDef)) {
                return TextResource.getInstance().getString("ConfigureBookkeepingView.TYPE_" +
                    accountDefinitions.get(rowIndex).account.getType().name());
            }
            return null;
        }
    }
}
