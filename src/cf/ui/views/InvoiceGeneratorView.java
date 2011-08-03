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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import nl.gogognome.cf.services.CreationException;
import nl.gogognome.cf.services.InvoiceLineDefinition;
import nl.gogognome.cf.services.InvoiceService;
import nl.gogognome.lib.gui.beans.BeanFactory;
import nl.gogognome.lib.gui.beans.DateSelectionBean;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import cf.engine.Database;
import cf.engine.Party;
import cf.ui.components.AccountComboBox;
import cf.ui.components.AmountTextField;

/**
 * This class implements a view in which the user can generate invoices
 * for multiple parties.
 *
 * @author Sander Kooijmans
 */
public class InvoiceGeneratorView extends View {

    /** The database used to get data from and to which the generated invoices are added. */
    private Database database;

    /** Text field containing the description of the generated invoices. */
    private JTextField tfDescription = new JTextField();

    /** The date model for the date when the invoices are generated. */
    private DateModel invoiceGenerationDateModel;

    /** Text field containing the ID of the generated invoices. */
    private JTextField tfId = new JTextField();

    /** Instances of this class represent a single line of the invoice template. */
    private class TemplateLine {
        JRadioButton rbAmountToBePaid = new JRadioButton();
        AccountComboBox cbAccount =
            new AccountComboBox(database);
        AmountTextField tfDebet =
            new AmountTextField(database.getCurrency());
        AmountTextField tfCredit =
            new AmountTextField(database.getCurrency());

        public TemplateLine() {
            radioButtonGroup.add(rbAmountToBePaid);
            cbAccount.selectAccount(null);
        }
    }

    /** The button group of the radio buttons. */
    private ButtonGroup radioButtonGroup = new ButtonGroup();

    /** Contains the lines of the template. */
    private ArrayList<TemplateLine> templateLines = new ArrayList<TemplateLine>();

    /** The panel that contains the template lines. */
    private JPanel templateLinesPanel;

    /**
     * Constructor.
     * @param database the database used to get data from and to which the generated invoices are added
     */
    public InvoiceGeneratorView(Database database) {
        super();
        this.database = database;
    }

    /** Gets the title of this view. */
    @Override
    public String getTitle() {
        return textResource.getString("invoiceGeneratorView.title");
    }

    /** This method is called when the view is closed. */
    @Override
    public void onClose() {
    }

    /**
     * Initializes the view.
     */
    @Override
    public void onInit() {
        // Initialize template panel
        JPanel templatePanel = new JPanel(new BorderLayout());
        templatePanel.setBorder(new CompoundBorder(
            new TitledBorder(textResource.getString("invoiceGeneratorView.template")),
            new EmptyBorder(10, 10, 10, 10)));

        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(widgetFactory.createLabel("invoiceGeneratorView.id", tfId),
                SwingUtils.createLabelGBConstraints(0, 0));
        panel.add(tfId,
                SwingUtils.createTextFieldGBConstraints(1, 0));
        tfId.setToolTipText(textResource.getString("invoiceGeneratorView.tooltip"));
        invoiceGenerationDateModel = new DateModel();
        invoiceGenerationDateModel.setDate(new Date(), null);
        DateSelectionBean dateSelectionBean = BeanFactory.getInstance().createDateSelectionBean(invoiceGenerationDateModel);
        panel.add(widgetFactory.createLabel("invoiceGeneratorView.date", dateSelectionBean),
                SwingUtils.createLabelGBConstraints(0, 1));
        panel.add(dateSelectionBean,
                SwingUtils.createLabelGBConstraints(1, 1));
        panel.add(widgetFactory.createLabel("invoiceGeneratorView.description", tfDescription),
                SwingUtils.createLabelGBConstraints(0, 2));
        panel.add(tfDescription,
                SwingUtils.createTextFieldGBConstraints(1, 2));
        tfDescription.setToolTipText(textResource.getString("invoiceGeneratorView.tooltip"));

        panel.setBorder(new EmptyBorder(0, 0, 12, 0));

        templateLinesPanel = new JPanel(new GridBagLayout());

        // Add two empty lines so the user can start editing the template.
        for (int i=0; i<2; i++) {
            templateLines.add(new TemplateLine());
        }
        updateTemplateLinesPanel();

        // Create button panel
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
        buttonPanel.add(widgetFactory.createButton("invoiceGeneratorView.addInvoices", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent e) {
                onAddInvoicesToBookkeeping();
            }
        }));
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));

        templatePanel.add(panel, BorderLayout.NORTH);
        templatePanel.add(templateLinesPanel, BorderLayout.CENTER);
        templatePanel.add(buttonPanel, BorderLayout.SOUTH);

        // Add panels to the view
        setLayout(new BorderLayout());
        add(templatePanel, BorderLayout.CENTER);

        setBorder(new EmptyBorder(10, 10, 10, 10));
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
            templateLinesPanel.add(line.cbAccount,
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
                templateLines.add(new TemplateLine());
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
	    private int index;

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
            radioButtonGroup.remove(line.rbAmountToBePaid);
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

        List<InvoiceLineDefinition> invoiceLines = new ArrayList<InvoiceLineDefinition>(templateLines.size());
        for (TemplateLine line : templateLines) {
            invoiceLines.add(new InvoiceLineDefinition(line.tfDebet.getAmount(), line.tfCredit.getAmount(),
                line.cbAccount.getSelectedAccount(), line.rbAmountToBePaid.isSelected()));
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
        PartiesView partiesView = new PartiesView(database, true, true);
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
        } catch (CreationException e) {
            MessageDialog.showMessage(this, "gen.titleError",
                e.getMessage());
            return;
        }
	    MessageDialog.showMessage(this, "gen.titleMessage",
	            "invoiceGeneratorView.messageSuccess");
	}
}
