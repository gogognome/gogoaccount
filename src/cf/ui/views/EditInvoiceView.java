/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

import nl.gogognome.lib.gui.beans.BeanFactory;
import nl.gogognome.lib.gui.beans.DateSelectionBean;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Party;
import cf.ui.components.PartySelector;
import cf.ui.components.PartySelectorListener;

/**
 * This class lets the user edit an existing invoice.
 */
public class EditInvoiceView extends View {

    /** The database. */
    private Database database;

    /** The id of the title. */
    private String titleId;

    /** The innvoice from which the initial values are taken. */
    private Invoice initialInvoice;

    /** Text field for the id. */
    private JTextField tfId;

    /** The date mdoel for the issue date. */
    private DateModel dateModel;

    /** The party selector for the party for whom the invoice is created. */
    private PartySelector psConcerningParty;

    /** The party selector for the  party that has to pay the invoice. */
    private PartySelector psPayingParty;

    /** Listener for changes in the concerning party. */
    private PartySelectorListener concerningPartyListener;

    /** The text field for the total amount to be paid. */
    private JTextField tfAmount;

    /** The table with descriptions and amounts. */
    private JTable table;

    /** The table model for the descriptions and amounts. */
    private DescriptionAndAmountTableModel tableModel;

    /** The invoice as entered by the user or <code>null</code> if the user cancelled the view. */
    private Invoice editedInvoice;

    /**
     * Constructor. To edit an existing invoice, give <code>invoice</code> a non-<code>null</code> value.
     * To create a new journal, set <code>invoice</code> to <code>null</code>.
     *
     * @param database the database to which the journal must be added
     * @param titleId the id of the title
     * @param invoice the invoice used to initialize the elements of the view. Must be <code>null</code>
     *        to edit a new invoice
     */
    public EditInvoiceView(Database database, String titleId, Invoice invoice) {
        this.database = database;
        this.titleId = titleId;
        this.initialInvoice = invoice;
    }

    @Override
    public String getTitle() {
        return textResource.getString(titleId);
    }

    @Override
    public void onClose() {
        if (psConcerningParty != null && concerningPartyListener != null) {
            psConcerningParty.removeListener(concerningPartyListener);
        }
    }

    @Override
    public void onInit() {
        // Create panel with ID, issue date, concerning party, paying party and amount to be paid.
        GridBagLayout gbl = new GridBagLayout();
        JPanel topPanel = new JPanel(gbl);

        String id;
        Date date;
        Party concerningParty;
        Party payingParty;
        String amount;
        if (initialInvoice != null) {
            id = initialInvoice.getId();
            date = initialInvoice.getIssueDate();
            concerningParty = initialInvoice.getConcerningParty();
            payingParty = initialInvoice.getPayingParty();
            amount = Factory.getInstance(AmountFormat.class).formatAmountWithoutCurrency(
                initialInvoice.getAmountToBePaid());
        } else {
            date = new Date();
            id = database.suggestNewInvoiceId(
                textResource.formatDate("editInvoiceView.dateFormatForNewId", date));
            concerningParty = null;
            payingParty = null;
            amount = "";
        }
        tfId = widgetFactory.createTextField(id);
        if (initialInvoice != null) {
            tfId.setEditable(false); // prevent changing the id of an existing invoice
            tfId.setEnabled(false);
        }
        int row = 0;
        JLabel label = widgetFactory.createLabel("editInvoiceView.id", tfId);
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        topPanel.add(tfId, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        dateModel = new DateModel();
        dateModel.setDate(date, null);
        DateSelectionBean sbDate = BeanFactory.getInstance().createDateSelectionBean(dateModel);
        label = widgetFactory.createLabel("editInvoiceView.issueDate", sbDate);
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        topPanel.add(sbDate, SwingUtils.createLabelGBConstraints(1, row));
        row++;

        psConcerningParty = new PartySelector();
        psConcerningParty.setSelectedParty(concerningParty);
        label = widgetFactory.createLabel("editInvoiceView.concerningParty", psConcerningParty);
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        topPanel.add(psConcerningParty, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        psPayingParty = new PartySelector();
        psPayingParty.setSelectedParty(payingParty);
        label = widgetFactory.createLabel("editInvoiceView.payingParty", psPayingParty);
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        topPanel.add(psPayingParty, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfAmount = new JTextField(amount);
        label = widgetFactory.createLabel("editInvoiceView.amount", tfAmount);
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        topPanel.add(tfAmount, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        // Create panel with descriptions and amounts table.
        JPanel middlePanel = new JPanel(new BorderLayout());
        List<String> descriptions = new LinkedList<String>();
        List<Amount> amounts = new LinkedList<Amount>();
        if (initialInvoice != null) {
            descriptions.addAll(Arrays.asList(initialInvoice.getDescriptions()));
            amounts.addAll(Arrays.asList(initialInvoice.getAmounts()));
        }
        tableModel = new DescriptionAndAmountTableModel(descriptions, amounts);
        table = new JTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        middlePanel.add(scrollPane, BorderLayout.CENTER);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.TOP, SwingConstants.VERTICAL);
        JButton button = widgetFactory.createButton("editInvoiceView.addRow", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent e) {
                onAddRow();
            }
        });
        buttonPanel.add(button);

        button = widgetFactory.createButton("editInvoiceView.editRow", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent e) {
                onEditRow();
            }
        });
        buttonPanel.add(button);

        button = widgetFactory.createButton("editInvoiceView.deleteRow", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent e) {
                onDeleteRow();
            }
        });
        buttonPanel.add(button);

        middlePanel.add(buttonPanel, BorderLayout.EAST);

        // Create button panel with ok and cancel buttons.
        buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        button = widgetFactory.createButton("gen.ok", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent event) {
                onOk();
            }
        });
        buttonPanel.add(button);
        button = widgetFactory.createButton("gen.cancel", closeAction);
        buttonPanel.add(button);

        // Put all panels on the view.
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(middlePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Add listener that copies the concerning party to the paying party if the paying
        // party has not been selected yet.
        concerningPartyListener = new PartySelectorListener() {
            @Override
			public void onSelectedPartyChanged(Party newParty) {
                if (psPayingParty.getSelectedParty() == null) {
                    psPayingParty.setSelectedParty(newParty);
                }
            }
        };
        psConcerningParty.addListener(concerningPartyListener);
    }

    /**
     * Gets the invoice as entered by the user.
     * @return the invoice as entered by the user or <code>null</code> if the user cancelled the view
     */
    public Invoice getEditedInvoice() {
        return editedInvoice;
    }

    /**
     * This method is called when the user wants to add a new row.
     */
    private void onAddRow() {
        EditDescriptionAndAmountView editDescriptionAndAmountView = new EditDescriptionAndAmountView(
            "editInvoiceView.addRowTileId", database.getCurrency());
        ViewDialog dialog = new ViewDialog(getParentWindow(), editDescriptionAndAmountView);
        dialog.showDialog();
        if (editDescriptionAndAmountView.getEditedDescription() != null) {
            tableModel.addRow(editDescriptionAndAmountView.getEditedDescription(),
                editDescriptionAndAmountView.getEditedAmount());
        }
    }

    /**
     * This method is called when the user wants to edit an existing row.
     */
    private void onEditRow() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            MessageDialog.showMessage(this, "gen.titleWarning",
                textResource.getString("editInvoiceView.noRowsSelectedToEdit"));
        } else if (rows.length == 0) {
            MessageDialog.showMessage(this, "gen.titleWarning",
                textResource.getString("editInvoiceView.multipleRowsSelectedToEdit"));
        } else {
            EditDescriptionAndAmountView editDescriptionAndAmountView = new EditDescriptionAndAmountView(
                "editInvoiceView.editRowTileId",
                tableModel.getDescription(rows[0]),
                tableModel.getAmount(rows[0]),
                database.getCurrency());
            ViewDialog dialog = new ViewDialog(getParentWindow(), editDescriptionAndAmountView);
            dialog.showDialog();
            if (editDescriptionAndAmountView.getEditedDescription() != null) {
                tableModel.updateRow(rows[0], editDescriptionAndAmountView.getEditedDescription(),
                    editDescriptionAndAmountView.getEditedAmount());
            }
        }
    }

    /**
     * This method is called when the user wants to delete an existing row.
     */
    private void onDeleteRow() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            MessageDialog.showMessage(this, "gen.titleWarning",
                textResource.getString("editInvoiceView.noRowsSelectedForDeletion"));
        } else {
            tableModel.deleteRows(rows);
        }
    }

    /**
     * This method is called when the user has finished editing the invoice and accepts the new values.
     */
    private void onOk() {
        String id = tfId.getText();
        if (id.length() == 0) {
            MessageDialog.showMessage(this, "gen.warning", "editInvoiceView.noIdEntered");
            return;
        }
        Date issueDate = dateModel.getDate();
        if (issueDate == null) {
            MessageDialog.showMessage(this, "gen.warning", "editInvoiceView.noDateEntered");
            return;
        }

        Party concerningParty = psConcerningParty.getSelectedParty();
        if (concerningParty == null) {
            MessageDialog.showMessage(this, "gen.warning", "editInvoiceView.noConcerningPartyEntered");
            return;
        }

        Party payingParty = psPayingParty.getSelectedParty();
        if (concerningParty == null) {
            MessageDialog.showMessage(this, "gen.warning", "editInvoiceView.noPayingPartyEntered");
            return;
        }

        Amount amount;
        try {
             amount = Factory.getInstance(AmountFormat.class).parse(tfAmount.getText(), database.getCurrency());
        } catch (ParseException e) {
            MessageDialog.showMessage(this, "gen.warning", "ejid.invalidAmount");
            return;
        }
        String[] descriptions = tableModel.getDescriptions();
        Amount[] amounts = tableModel.getAmounts();
        editedInvoice = new Invoice(id, payingParty, concerningParty, amount, issueDate, descriptions, amounts);
        closeAction.actionPerformed(null);
    }

    /**
     * Table model for the table containing descriptions and models.
     */
    private static class DescriptionAndAmountTableModel extends AbstractTableModel {

        private List<String> descriptions;

        private List<Amount> amounts;

        /**
         * Constructor. Precondition: <code>descriptions.size() == amounts.size()</code>.
         * @param descriptions the descriptions
         * @param amounts the amounts.
         */
        public DescriptionAndAmountTableModel(List<String> descriptions, List<Amount> amounts) {
            this.descriptions = new ArrayList<String>(descriptions);
            this.amounts = new ArrayList<Amount>(amounts);
        }

        /**
         * Gets the description of the specified row.
         * @param row the row index
         * @return the description
         */
        public String getDescription(int row) {
            return descriptions.get(row);
        }

        public String[] getDescriptions() {
            return descriptions.toArray(new String[descriptions.size()]);
        }

        /**
         * Gets the amount of the specified row.
         * @param row the row index
         * @return the amount
         */
        public Amount getAmount(int row) {
            return amounts.get(row);
        }

        public Amount[] getAmounts() {
            return amounts.toArray(new Amount[amounts.size()]);
        }

        /**
         * Adds a row to the end of the table.
         * @param description the description of the row
         * @param amount the amount of the row; can be <code>null</code>
         */
        public void addRow(String description, Amount amount) {
            descriptions.add(description);
            amounts.add(amount);
            fireTableRowsInserted(descriptions.size()-1, descriptions.size()-1);
        }

        /**
         * Updates a row.
         * @param index the index of the row
         * @param description the new description of the row
         * @param amount the new amount of the row; can be <code>null</code>
         */
        public void updateRow(int index, String description, Amount amount) {
            descriptions.set(index, description);
            amounts.set(index, amount);
            fireTableRowsUpdated(index, index);
        }

        /**
         * Deletes a number of rows.
         * @param indexes the indexes of the rows to be deleted
         */
        public void deleteRows(int[] indexes) {
            Arrays.sort(indexes);
            for (int i=0; i<indexes.length; i++) {
                descriptions.remove(indexes[i] - i);
                amounts.remove(indexes[i] - i);
            }
            fireTableDataChanged();
        }

        @Override
        public String getColumnName(int columnIndex) {
            String result;
            switch(columnIndex) {
            case 0:
                result = Factory.getInstance(TextResource.class).getString("editInvoiceView.tableHeader.descriptions");
                break;

            case 1:
                result = Factory.getInstance(TextResource.class).getString("editInvoiceView.tableHeader.amounts");
                break;

            default:
                result = "???";
            }
            return result;

        }

        @Override
		public int getColumnCount() {
            return 2;
        }

        @Override
		public int getRowCount() {
            return descriptions.size();
        }

        @Override
		public Object getValueAt(int rowIndex, int columnIndex) {
            Object result;
            switch(columnIndex) {
            case 0:
                result = descriptions.get(rowIndex);
                break;

            case 1:
                Amount a = amounts.get(rowIndex);
                if (a != null) {
                    result = Factory.getInstance(AmountFormat.class)
                    	.formatAmountWithoutCurrency(a);
                } else {
                    result = "";
                }
                break;

            default:
                result = "???";
            }
            return result;
        }

    }
}
