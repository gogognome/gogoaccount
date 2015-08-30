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
package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.businessobjects.AccountType;
import nl.gogognome.gogoaccount.businessobjects.Party;
import nl.gogognome.gogoaccount.database.AccountDAO;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.gui.components.AccountFormatter;
import nl.gogognome.gogoaccount.gui.components.AmountTextField;
import nl.gogognome.gogoaccount.services.InvoiceLineDefinition;
import nl.gogognome.gogoaccount.services.InvoiceService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.lib.awt.layout.VerticalLayout;
import nl.gogognome.lib.gui.beans.ComboBoxBean;
import nl.gogognome.lib.gui.beans.DateSelectionBean;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;

/**
 * This class implements a view in which the user can generate invoices
 * for multiple parties.
 *
 * @author Sander Kooijmans
 */
public class InvoiceGeneratorView extends View {

	private static final long serialVersionUID = 1L;

	private final Database database;

    private List<Account> accounts;
    private Currency currency;

	private final JTextField tfDescription = new JTextField();
	private DateModel invoiceGenerationDateModel;
	private final JTextField tfId = new JTextField();
	private JRadioButton rbSalesInvoice;
	private final ButtonGroup invoiceTypeButtonGroup = new ButtonGroup();

	/** Instances of this class represent a single line of the invoice template. */
	private class TemplateLine {
		JRadioButton rbAmountToBePaid = new JRadioButton();
		private final ListModel<Account> accountListModel = new ListModel<>();
		AmountTextField tfDebet;
		AmountTextField tfCredit;

		public TemplateLine(List<Account> accounts, Currency currency) {
			tfDebet = new AmountTextField(currency);
			tfCredit = new AmountTextField(currency);
			templateLinesButtonGroup.add(rbAmountToBePaid);
			accountListModel.setItems(accounts);
		}
	}

	private final ButtonGroup templateLinesButtonGroup = new ButtonGroup();
	private final ArrayList<TemplateLine> templateLines = newArrayList();
	private JPanel templateLinesPanel;

	public InvoiceGeneratorView(Database database) {
        this.database = database;
	}

	@Override
	public String getTitle() {
		return textResource.getString("invoiceGeneratorView.title");
	}

	@Override
	public void onClose() {
	}

	@Override
	public void onInit() {
        try {
            ServiceTransaction.withoutResult(() -> {
				accounts = new AccountDAO(database).findAll("id");
				currency = database.getCurrency();
			});
        } catch (ServiceException e) {
            MessageDialog.showMessage(this, "gen.error", "gen.problemOccurred");
			close();
            return;
        }

		JPanel invoiceTypePanel = createInvoiceTypePanel();
		JPanel headerPanel = createHeaderPanel();
		initTemplateLinesPanel();
		ButtonPanel buttonPanel = createButtonPanel();

		JPanel templatePanel = new JPanel(new BorderLayout());
		templatePanel.setBorder(new CompoundBorder(
				new TitledBorder(textResource.getString("invoiceGeneratorView.template")),
				new EmptyBorder(10, 10, 10, 10)));

		JPanel northPanel = new JPanel();
		northPanel.setLayout(new VerticalLayout(20, VerticalLayout.BOTH));
		northPanel.add(invoiceTypePanel);
		northPanel.add(headerPanel);
		templatePanel.add(northPanel, BorderLayout.NORTH);
		templatePanel.add(templateLinesPanel, BorderLayout.CENTER);
		templatePanel.add(buttonPanel, BorderLayout.SOUTH);

		setLayout(new BorderLayout());
		add(templatePanel, BorderLayout.CENTER);

		setBorder(new EmptyBorder(10, 10, 10, 10));

		onInvoiceTypeChanged();
	}

	private ButtonPanel createButtonPanel() {
		// Create button panel
		ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
		buttonPanel.add(widgetFactory.createButton("invoiceGeneratorView.addInvoices", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				onAddInvoicesToBookkeeping();
			}
		}));
		buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
		return buttonPanel;
	}

	private void initTemplateLinesPanel() {
		templateLinesPanel = new JPanel(new GridBagLayout());

		// Add two empty lines so the user can start editing the template.
		for (int i=0; i<2; i++) {
			templateLines.add(new TemplateLine(accounts, currency));
		}
		updateTemplateLinesPanel();
	}

	private JPanel createInvoiceTypePanel() {
		JPanel invoiceTypePanel = new JPanel(new GridBagLayout());
		int row = 0;
		rbSalesInvoice = new JRadioButton();
		invoiceTypePanel.add(rbSalesInvoice,
				SwingUtils.createGBConstraints(0, row, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE,
						3, 0, 3, 5));
		invoiceTypePanel.add(new JLabel(textResource.getString("invoiceGeneratorView.salesInvoice")),
				SwingUtils.createTextFieldGBConstraints(1, row));
		row++;

		JRadioButton rbPurchaseInvoice = new JRadioButton();
		invoiceTypePanel.add(rbPurchaseInvoice,
				SwingUtils.createGBConstraints(0, row, 1, 1, 0.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.NONE,
						3, 0, 3, 5));
		invoiceTypePanel.add(new JLabel(textResource.getString("invoiceGeneratorView.purchaseInvoice")),
				SwingUtils.createTextFieldGBConstraints(1, row));
		row++;

		ChangeListener changeListener = e -> onInvoiceTypeChanged();
		rbSalesInvoice.addChangeListener(changeListener);
		rbPurchaseInvoice.addChangeListener(changeListener);
		invoiceTypeButtonGroup.add(rbSalesInvoice);
		invoiceTypeButtonGroup.add(rbPurchaseInvoice);
		rbSalesInvoice.setSelected(true);

		return invoiceTypePanel;
	}

	private JPanel createHeaderPanel() {
		JPanel headerPanel = new JPanel(new GridBagLayout());
		int row = 0;

		headerPanel.add(widgetFactory.createLabel("invoiceGeneratorView.id", tfId),
				SwingUtils.createLabelGBConstraints(0, row));
		headerPanel.add(tfId,
				SwingUtils.createTextFieldGBConstraints(1, row));
		row++;

		tfId.setToolTipText(textResource.getString("invoiceGeneratorView.tooltip"));
		invoiceGenerationDateModel = new DateModel();
		invoiceGenerationDateModel.setDate(new Date(), null);
		DateSelectionBean dateSelectionBean = beanFactory.createDateSelectionBean(invoiceGenerationDateModel);
		headerPanel.add(widgetFactory.createLabel("invoiceGeneratorView.date", dateSelectionBean),
				SwingUtils.createLabelGBConstraints(0, row));
		headerPanel.add(dateSelectionBean,
				SwingUtils.createLabelGBConstraints(1, row));
		row++;

		headerPanel.add(widgetFactory.createLabel("invoiceGeneratorView.description", tfDescription),
				SwingUtils.createLabelGBConstraints(0, row));
		headerPanel.add(tfDescription,
				SwingUtils.createTextFieldGBConstraints(1, row));
		tfDescription.setToolTipText(textResource.getString("invoiceGeneratorView.tooltip"));
		row++;

		headerPanel.setBorder(new EmptyBorder(0, 0, 12, 0));
		return headerPanel;
	}

	private void updateTemplateLinesPanelAndRepaint() {
		updateTemplateLinesPanel();
		revalidate();
		repaint();
	}

	private void updateTemplateLinesPanel() {
		templateLinesPanel.removeAll();

		templateLinesPanel.add(new JLabel(textResource.getString("invoiceGeneratorView.amountToBePaid")),
				SwingUtils.createLabelGBConstraints(0, 0));
		templateLinesPanel.add(new JLabel(textResource.getString("gen.account")),
				SwingUtils.createLabelGBConstraints(1, 0));
		templateLinesPanel.add(new JLabel(textResource.getString("gen.debet")),
				SwingUtils.createLabelGBConstraints(2, 0));
		templateLinesPanel.add(new JLabel(textResource.getString("gen.credit")),
				SwingUtils.createLabelGBConstraints(3, 0));

		int row = 1;
		for (int i=0; i<templateLines.size(); i++) {
			TemplateLine line = templateLines.get(i);
			int top = 3;
			int bottom = 3;
			templateLinesPanel.add(line.rbAmountToBePaid,
					SwingUtils.createGBConstraints(0, row, 1, 1, 0.0, 0.0,
							GridBagConstraints.CENTER, GridBagConstraints.NONE,
							top, 0, bottom, 5));
			ComboBoxBean<Account> cbAccount = beanFactory.createComboBoxBean(line.accountListModel);
			cbAccount.setItemFormatter(new AccountFormatter());
			templateLinesPanel.add(cbAccount,
					SwingUtils.createGBConstraints(1, row, 1, 1, 3.0, 0.0,
							GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
							top, 0, bottom, 5));
			templateLinesPanel.add(line.tfDebet,
					SwingUtils.createGBConstraints(2, row, 1, 1, 1.0, 0.0,
							GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
							top, 0, bottom, 5));
			templateLinesPanel.add(line.tfCredit,
					SwingUtils.createGBConstraints(3, row, 1, 1, 1.0, 0.0,
							GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
							top, 0, bottom, 5));

			JButton deleteButton = widgetFactory.createButton("invoiceGeneratorView.delete", null);
			deleteButton.addActionListener(new DeleteActionListener(i));
			templateLinesPanel.add(deleteButton,
					SwingUtils.createGBConstraints(4, row, 1, 1, 1.0, 0.0,
							GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
							top, 5, bottom, 0));
			row++;
		}

		JButton newButton = widgetFactory.createButton("invoiceGeneratorView.new", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent event) {
				templateLines.add(new TemplateLine(accounts, currency));
				updateTemplateLinesPanelAndRepaint();
			}
		});
		templateLinesPanel.add(newButton,
				SwingUtils.createGBConstraints(4, row, 1, 1, 1.0, 0.0,
						GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
						0, 0, 0, 0));
	}

	private class DeleteActionListener implements ActionListener {
		/** Index of the line to be deleted by this action. */
		private final int index;

		/**
		 * Constructor.
		 * @param index index of the line to be deleted by this action.
		 */
		public DeleteActionListener(int index) {
			this.index = index;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			TemplateLine line = templateLines.remove(index);
			templateLinesButtonGroup.remove(line.rbAmountToBePaid);
			updateTemplateLinesPanelAndRepaint();
		}
	}

	/**
	 * This method is called when the "add invoices" button has been pressed.
	 * The user will be asked to select the parties for which invoices are generated.
	 * After that, a dialog asks whether the user is sure to continue. Only if the user explicitly
	 * states "yes" the invoices will be added to the bookkeeping.
	 */
	private void onAddInvoicesToBookkeeping() {
		// Validate the input.
		Date date = invoiceGenerationDateModel.getDate();
		if (date == null) {
			MessageDialog.showMessage(this, "gen.titleError", "gen.invalidDate");
			return;
		}

		List<InvoiceLineDefinition> invoiceLines = new ArrayList<>(templateLines.size());
		for (TemplateLine line : templateLines) {
			invoiceLines.add(new InvoiceLineDefinition(line.tfDebet.getAmount(), line.tfCredit.getAmount(),
					line.accountListModel.getSelectedItem(), line.rbAmountToBePaid.isSelected()));
		}

		boolean amountToBePaidSelected = false;
		for (InvoiceLineDefinition line : invoiceLines) {
			if (!amountToBePaidSelected) {
				amountToBePaidSelected = line.isAmountToBePaid();
			} else {
				if (line.isAmountToBePaid()) {
					MessageDialog.showMessage(this, "gen.titleError",
							"invoiceGeneratorView.moreThanOneAmountToBePaid");
					return;
				}
			}
			if (line.getDebet() == null && line.getCredit() == null) {
				MessageDialog.showMessage(this, "gen.titleError",
						"invoiceGeneratorView.emptyAmountsFound");
				return;
			}
			if (line.getDebet() != null && line.getCredit() != null) {
				MessageDialog.showMessage(this, "gen.titleError",
						"invoiceGeneratorView.doubleAmountsFound");
				return;
			}

			if (line.getAccount() == null) {
				MessageDialog.showMessage(this, "gen.titleError",
						"invoiceGeneratorView.emptyAccountFound");
				return;
			}
		}

		if (!amountToBePaidSelected) {
			MessageDialog.showMessage(this, "gen.titleError",
					"invoiceGeneratorView.noAmountToBePaidSelected");
			return;
		}

		// Let the user select the parties.
		PartiesView partiesView = new PartiesView(database);
		partiesView.setSelectioEnabled(true);
		partiesView.setMultiSelectionEnabled(true);
		ViewDialog dialog = new ViewDialog(getParentWindow(), partiesView);
		dialog.showDialog();
		Party[] parties = partiesView.getSelectedParties();
		if (parties == null) {
			// No parties have been selected. Abort this method.
			return;
		}

		// Ask the user whether he/she is sure to generate the invoices.
		int choice = MessageDialog.showYesNoQuestion(this, "gen.titleWarning",
				"invoiceGeneratorView.areYouSure");
		if (choice != MessageDialog.YES_OPTION) {
			// The user canceled the operation.
			return;
		}

		try {
			InvoiceService.createInvoiceAndJournalForParties(database, tfId.getText(), Arrays.asList(parties), date,
					tfDescription.getText(), invoiceLines);
		} catch (ServiceException e) {
			MessageDialog.showMessage(this, "gen.titleError",
					e.getMessage());
			return;
		}
		MessageDialog.showMessage(this, "gen.titleMessage",
				"invoiceGeneratorView.messageSuccess");
	}

	private void onInvoiceTypeChanged() {
		if (templateLines.isEmpty()) {
			return;
		}

		TemplateLine templateLine = templateLines.get(0);
		templateLine.rbAmountToBePaid.setSelected(true);
		String totalAmount = textResource.getString("invoiceGeneratorView.fillInAmountHere");
		AccountType accountType;
		if (rbSalesInvoice.isSelected()) {
			templateLine.tfDebet.setText(totalAmount);
			templateLine.tfCredit.setText(null);
			accountType = AccountType.DEBTOR;
		} else {
			templateLine.tfDebet.setText(null);
			templateLine.tfCredit.setText(totalAmount);
			accountType = AccountType.CREDITOR;
		}

        try {
            ServiceTransaction.withoutResult(() -> {
                List<Account> accounts = new AccountDAO(database).findAccountsOfType(accountType);
                if (!accounts.isEmpty()) {
                    templateLine.accountListModel.setSelectedItem(accounts.get(0), null);
                }
            });
        } catch (ServiceException e) {
            MessageDialog.showMessage(this, "gen.error", "gen.problemOccurred");
        }
	}

}
