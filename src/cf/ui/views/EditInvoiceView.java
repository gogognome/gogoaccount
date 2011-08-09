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
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import nl.gogognome.gogoaccount.gui.beans.PartyBean;
import nl.gogognome.gogoaccount.models.PartyModel;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.AbstractListTableModel;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.lib.util.Tuple;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.Party;

/**
 * This class lets the user edit an existing invoice.
 */
public class EditInvoiceView extends OkCancelView {

	private static final long serialVersionUID = 1L;

	private Database database;

    private String titleId;

    private Invoice initialInvoice;

    private StringModel idModel = new StringModel();
    private StringModel amountModel = new StringModel();
    private DateModel dateModel = new DateModel();
    private PartyModel concerningPartyModel = new PartyModel();
    private PartyModel payingPartyModel = new PartyModel();

    private ModelChangeListener concerningPartyListener;

    private JTable table;
    private DescriptionAndAmountTableModel tableModel;

    private Invoice editedInvoice;

    private AmountFormat amountFormat = Factory.getInstance(AmountFormat.class);

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
        concerningPartyModel.removeModelChangeListener(concerningPartyListener);
    }

    @Override
    public void onInit() {
    	initModels();
    	addComponents();
        addListeners();
    }

	private void initModels() {
        if (initialInvoice != null) {
            idModel.setString(initialInvoice.getId());
            idModel.setEnabled(false, null);
            dateModel.setDate(initialInvoice.getIssueDate());
            concerningPartyModel.setParty(initialInvoice.getConcerningParty());
            payingPartyModel.setParty(initialInvoice.getPayingParty());
            amountModel.setString(amountFormat.formatAmountWithoutCurrency(
                initialInvoice.getAmountToBePaid()));
        } else {
            dateModel.setDate(new Date());
            idModel.setString(database.suggestNewInvoiceId(
                textResource.formatDate("editInvoiceView.dateFormatForNewId", dateModel.getDate())));
        }

	}

	private void addListeners() {
        concerningPartyListener = new CopyConceringPartyToPayingPartyListener();
        concerningPartyModel.addModelChangeListener(concerningPartyListener);
	}

    @Override
    protected JComponent createNorthComponent() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);

        ifc.addField("editInvoiceView.id", idModel);
        ifc.addField("editInvoiceView.issueDate", dateModel);
        ifc.addVariableSizeField("editInvoiceView.concerningParty",
        		new PartyBean(database, concerningPartyModel));
        ifc.addVariableSizeField("editInvoiceView.payingParty",
        		new PartyBean(database, payingPartyModel));
        ifc.addField("editInvoiceView.amount", amountModel);

    	return ifc;
    }

    @Override
	protected JComponent createCenterComponent() {
        // Create panel with descriptions and amounts table.
        JPanel middlePanel = new JPanel(new BorderLayout());
        List<Tuple<String, Amount>> tuples = new ArrayList<Tuple<String,Amount>>();
        if (initialInvoice != null) {
        	String[] descriptions = initialInvoice.getDescriptions();
        	Amount[] amounts = initialInvoice.getAmounts();
        	for (int i=0; i<descriptions.length; i++) {
        		tuples.add(new Tuple<String, Amount>(descriptions[i], amounts[i]));
        	}
        }
        tableModel = new DescriptionAndAmountTableModel(tuples);
        table = widgetFactory.createTable(tableModel);
        JScrollPane scrollPane =widgetFactory.createScrollPane(table);
        middlePanel.add(scrollPane, BorderLayout.CENTER);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.TOP, SwingConstants.VERTICAL);
        buttonPanel.addButton("editInvoiceView.addRow", new AddAction());
        buttonPanel.addButton("editInvoiceView.editRow", new EditAction());
        buttonPanel.addButton("editInvoiceView.deleteRow", new DeleteAction());

        middlePanel.add(buttonPanel, BorderLayout.EAST);

        return middlePanel;
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
            MessageDialog.showInfoMessage(this, "editInvoiceView.noRowsSelectedToEdit");
        } else if (rows.length == 0) {
            MessageDialog.showInfoMessage(this, "editInvoiceView.multipleRowsSelectedToEdit");
        } else {
        	Tuple<String, Amount> tuple = tableModel.getRow(rows[0]);
            EditDescriptionAndAmountView editDescriptionAndAmountView = new EditDescriptionAndAmountView(
                "editInvoiceView.editRowTileId",
                tuple.getFirst(),
                tuple.getSecond(),
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
            MessageDialog.showInfoMessage(this, "editInvoiceView.noRowsSelectedForDeletion");
        } else {
            tableModel.removeRows(rows);
        }
    }

    @Override
	protected void onOk() {
        String id = idModel.getString();
        if (id.length() == 0) {
            MessageDialog.showMessage(this, "gen.warning", "editInvoiceView.noIdEntered");
            return;
        }
        Date issueDate = dateModel.getDate();
        if (issueDate == null) {
            MessageDialog.showMessage(this, "gen.warning", "editInvoiceView.noDateEntered");
            return;
        }

        Party concerningParty = concerningPartyModel.getParty();
        if (concerningParty == null) {
            MessageDialog.showMessage(this, "gen.warning", "editInvoiceView.noConcerningPartyEntered");
            return;
        }

        Party payingParty = payingPartyModel.getParty();
        if (payingParty == null) {
            MessageDialog.showMessage(this, "gen.warning", "editInvoiceView.noPayingPartyEntered");
            return;
        }

        Amount amount;
        try {
             amount = amountFormat.parse(amountModel.getString(), database.getCurrency());
        } catch (ParseException e) {
            MessageDialog.showMessage(this, "gen.warning", "gen.invalidAmount");
            return;
        }

        List<Tuple<String, Amount>> tuples = tableModel.getRows();
        String[] descriptions = new String[tuples.size()];
        Amount[] amounts = new Amount[tuples.size()];
        for (int i=0; i<tuples.size(); i++) {
        	descriptions[i] = tuples.get(i).getFirst();
        	amounts[i] = tuples.get(i).getSecond();
        	if (amounts[i] != null && amounts[i].isZero()) {
        		amounts[i] = null;
        	}
        }

        editedInvoice = new Invoice(id, payingParty, concerningParty, amount, issueDate, descriptions, amounts);
        closeAction.actionPerformed(null);
    }

    private final class DeleteAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    onDeleteRow();
		}
	}

	private final class EditAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    onEditRow();
		}
	}

	private final class AddAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    onAddRow();
		}
	}

	private final class CopyConceringPartyToPayingPartyListener implements
			ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
		    if (payingPartyModel.getParty() == null) {
		        payingPartyModel.setParty(concerningPartyModel.getParty());
		    }
		}
	}

	/**
     * Table model for the table containing descriptions and models.
     */
    private static class DescriptionAndAmountTableModel
    		extends AbstractListTableModel<Tuple<String, Amount>> {

    	private final static ColumnDefinition DESCRIPTIONS =
    		new ColumnDefinition("editInvoiceView.tableHeader.descriptions", String.class, 300);

    	private final static ColumnDefinition AMOUNTS =
    		new ColumnDefinition("editInvoiceView.tableHeader.amounts", String.class, 100);

    	private final static List<ColumnDefinition> COLUMN_DEFINITIONS =
    		Arrays.asList(DESCRIPTIONS, AMOUNTS);

        public DescriptionAndAmountTableModel(List<Tuple<String, Amount>> tuples) {
        	super(COLUMN_DEFINITIONS, tuples);
        }

        /**
         * Adds a row to the end of the table.
         * @param description the description of the row
         * @param amount the amount of the row; can be <code>null</code>
         */
        public void addRow(String description, Amount amount) {
            addRow(new Tuple<String, Amount>(description, amount));
        }

        /**
         * Updates a row.
         * @param index the index of the row
         * @param description the new description of the row
         * @param amount the new amount of the row; can be <code>null</code>
         */
        public void updateRow(int index, String description, Amount amount) {
            updateRow(index, new Tuple<String, Amount>(description, amount));
        }

        @Override
		public Object getValueAt(int rowIndex, int columnIndex) {
        	ColumnDefinition colDef = COLUMN_DEFINITIONS.get(columnIndex);
        	Tuple<String, Amount> tuple = getRow(rowIndex);
            Object result = null;

            if (DESCRIPTIONS == colDef) {
                result = tuple.getFirst();
            } else if (AMOUNTS == colDef) {
                Amount a = tuple.getSecond();
                if (a != null && !a.isZero()) {
                    result = Factory.getInstance(AmountFormat.class)
                    	.formatAmountWithoutCurrency(a);
                }
            }

            return result;
        }

    }
}
