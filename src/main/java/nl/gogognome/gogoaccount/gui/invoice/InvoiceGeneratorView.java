package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate;
import nl.gogognome.gogoaccount.component.invoice.InvoiceTemplateLine;
import nl.gogognome.gogoaccount.component.invoice.amountformula.AmountFormula;
import nl.gogognome.gogoaccount.component.invoice.amountformula.AmountFormulaParser;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.components.AccountFormatter;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.gogoaccount.gui.views.PartiesView;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.awt.layout.VerticalLayout;
import nl.gogognome.lib.gui.beans.Bean;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.ParseException;
import java.util.*;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static nl.gogognome.gogoaccount.component.configuration.AccountType.CREDITOR;
import static nl.gogognome.gogoaccount.component.configuration.AccountType.DEBTOR;
import static nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate.Type.PURCHASE;
import static nl.gogognome.gogoaccount.component.invoice.InvoiceTemplate.Type.SALE;

/**
 * This class implements a view in which the user can generate invoices
 * for multiple parties.
 */
@SuppressWarnings("UnusedAssignment")
public class InvoiceGeneratorView extends View {

    private static final long serialVersionUID = 1L;

    private final ConfigurationService configurationService;
    private final LedgerService ledgerService;

    private final Document document;
    private final AmountFormulaParser amountFormulaParser;
    private final ViewFactory viewFactory;

    private List<Account> accounts;

    JPanel debtorOrCreditorPanel = new JPanel(new GridBagLayout());
    private final JTextField tfDescription = new JTextField();
    private DateModel invoiceGenerationDateModel;
    private final JTextField tfId = new JTextField();
    private JRadioButton rbSalesInvoice;
    private final ButtonGroup invoiceTypeButtonGroup = new ButtonGroup();

    private ListModel<Account> debtorAccountModel;
    private ListModel<Account> creditorAccountModel;

    /** Instances of this class represent a single line of the invoice template. */
    private class TemplateLine {
        private final ListModel<Account> accountListModel = new ListModel<>();
        private StringModel descriptionModel;
        private StringModel amountModel;
        private AmountFormula amountFormula;

        public TemplateLine(List<Account> accounts) {
            amountModel = new StringModel();
            descriptionModel = new StringModel();
            accountListModel.setItems(accounts);
            accountListModel.addModelChangeListener(
                    model -> descriptionModel.setString(accountListModel.getSelectedItem() != null ? accountListModel.getSelectedItem().getName() : null));
            amountModel.addModelChangeListener(model -> {
                if (amountModel.getString().indexOf('|') != amountModel.getString().lastIndexOf('|')) {
                    // Use invokeLater because this code is called while a mutation notification is handled.
                    // It is not allowed to change the model right now.
                    SwingUtilities.invokeLater(() -> {
                        updateTemplateLine(amountModel.getString());
                    });
                }
                try {
                    amountFormula = amountFormulaParser.parse(amountModel.getString());
                } catch (ParseException e) {
                    amountFormula = null;
                }
            });
        }

        private void updateTemplateLine(String line) {
            String[] parts = line.split("[|]");
            if (parts.length != 3) {
                return;
            }

            Optional<Account> account = accountListModel.getItems().stream().filter(a -> a.getId().equals(parts[0].trim())).findFirst();
            if (!account.isPresent()) {
                return;
            }

            accountListModel.setSelectedItem(account.get(), null);
            descriptionModel.setString(parts[1].trim());
            amountModel.setString(parts[2].trim());
        }
    }

    private final List<TemplateLine> templateLines = new ArrayList<>();
    private JPanel templateLinesPanel;

    public InvoiceGeneratorView(Document document, ConfigurationService configurationService, LedgerService ledgerService, AmountFormulaParser amountFormulaParser, ViewFactory viewFactory) {
        this.configurationService = configurationService;
        this.ledgerService = ledgerService;
        this.document = document;
        this.amountFormulaParser = amountFormulaParser;
        this.viewFactory = viewFactory;
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
            accounts = configurationService.findAllAccounts(document);
            debtorAccountModel = new ListModel<>(accounts.stream().filter(a -> a.getType() == DEBTOR).collect(toList()));
            if (debtorAccountModel.getItems().size() == 1) {
                debtorAccountModel.setSelectedIndex(0, null);
            }
            creditorAccountModel = new ListModel<>(accounts.stream().filter(a -> a.getType() == CREDITOR).collect(toList()));
            if (creditorAccountModel.getItems().size() == 1) {
                creditorAccountModel.setSelectedIndex(0, null);
            }
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.error");
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
        northPanel.add(widgetFactory.createLabel("invoiceGeneratorView.helpText.html"));
        northPanel.add(invoiceTypePanel);
        northPanel.add(debtorOrCreditorPanel);
        northPanel.add(headerPanel);
        templatePanel.add(northPanel, BorderLayout.NORTH);
        templatePanel.add(templateLinesPanel, BorderLayout.CENTER);
        templatePanel.add(buttonPanel, BorderLayout.SOUTH);

        setLayout(new BorderLayout());
        add(templatePanel, BorderLayout.CENTER);

        setBorder(new EmptyBorder(10, 10, 10, 10));
    }

    private ButtonPanel createButtonPanel() {
        // Create button panel
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
        buttonPanel.add(widgetFactory.createButton("invoiceGeneratorView.addInvoices", this::onAddInvoicesToBookkeeping));
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        return buttonPanel;
    }

    private void initTemplateLinesPanel() {
        templateLinesPanel = new JPanel(new GridBagLayout());

        // Add one empty line so the user can start editing the template.
        templateLines.add(new TemplateLine(accounts));

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
        invoiceTypeButtonGroup.add(rbSalesInvoice);
        invoiceTypeButtonGroup.add(rbPurchaseInvoice);
        rbSalesInvoice.setSelected(true);

        return invoiceTypePanel;
    }

    private void initDebtorOrCreditorSelection() {
        debtorOrCreditorPanel.removeAll();

        InputFieldsColumn ifc = new InputFieldsColumn();
        if (rbSalesInvoice.isSelected()) {
            ifc.addComboBoxField("invoiceGeneratorView.debtorAccount", debtorAccountModel, new AccountFormatter());
        } else {
            ifc.addComboBoxField("invoiceGeneratorView.creditorAccount", creditorAccountModel, new AccountFormatter());
        }

        debtorOrCreditorPanel.add(ifc, SwingUtils.createLabelGBConstraints(0, 0));
        debtorOrCreditorPanel.add(new JLabel(""), SwingUtils.createTextFieldGBConstraints(1, 0));

        repaint();
        revalidate();
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
        Bean dateSelectionBean = beanFactory.createDateSelectionBean(invoiceGenerationDateModel);
        headerPanel.add(widgetFactory.createLabel("invoiceGeneratorView.date", dateSelectionBean.getComponent()),
                SwingUtils.createLabelGBConstraints(0, row));
        headerPanel.add(dateSelectionBean.getComponent(),
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

        templateLinesPanel.add(new JLabel(textResource.getString("gen.account")),
                SwingUtils.createLabelGBConstraints(1, 0));
        templateLinesPanel.add(new JLabel(textResource.getString("gen.description")),
                SwingUtils.createLabelGBConstraints(2, 0));
        templateLinesPanel.add(new JLabel(textResource.getString("gen.amount")),
                SwingUtils.createLabelGBConstraints(3, 0));

        int row = 1;
        for (int i=0; i<templateLines.size(); i++) {
            TemplateLine line = templateLines.get(i);
            Bean cbAccount = beanFactory.createComboBoxBean(line.accountListModel, new AccountFormatter());
            templateLinesPanel.add(cbAccount.getComponent(),
                    SwingUtils.createLabelGBConstraints(1, row));
            templateLinesPanel.add(beanFactory.createTextFieldBean(line.descriptionModel) .getComponent(),
                    SwingUtils.createTextFieldGBConstraints(2, row));
            templateLinesPanel.add(beanFactory.createRightAlignedTextFieldBean(line.amountModel).getComponent(),
                    SwingUtils.createTextFieldGBConstraints(3, row));

            int index = i;
            JButton deleteButton = widgetFactory.createButton("invoiceGeneratorView.delete", () -> onDelete(index));
            templateLinesPanel.add(deleteButton,
                    SwingUtils.createLabelGBConstraints(4, row));
            row++;
        }

        JButton newButton = widgetFactory.createButton("invoiceGeneratorView.new", () -> {
                templateLines.add(new TemplateLine(accounts));
                updateTemplateLinesPanelAndRepaint();
                SwingUtilities.invokeLater(() -> templateLinesPanel.getComponent(templateLinesPanel.getComponentCount() - 5).requestFocus());
            }
        );
        templateLinesPanel.add(newButton, SwingUtils.createLabelGBConstraints(4, row));
    }

    private void onDelete(int index) {
        templateLines.remove(index);
        updateTemplateLinesPanelAndRepaint();
    }

    private void onAddInvoicesToBookkeeping() {
        HandleException.for_(this, () -> {
            Date date = invoiceGenerationDateModel.getDate();
            List<InvoiceTemplateLine> invoiceLines = templateLines.stream()
                    .map(line -> new InvoiceTemplateLine(line.amountFormula, line.descriptionModel.getString(), line.accountListModel.getSelectedItem()))
                    .collect(toList());

            if (!validateInput(date, invoiceLines)) {
                return;
            }

            Party[] parties = selectParties();
            if (parties == null) {
                return;
            }

            int choice = MessageDialog.showYesNoQuestion(this, "gen.titleWarning", "invoiceGeneratorView.areYouSure");
            if (choice != MessageDialog.YES_OPTION) {
                return;
            }

            generateInvoices(date, invoiceLines, parties);
            MessageDialog.showMessage(this, "gen.titleMessage", "invoiceGeneratorView.messageSuccess");
        });
    }

    private void generateInvoices(Date date, List<InvoiceTemplateLine> invoiceLines, Party[] parties) throws ServiceException {
        Account account = rbSalesInvoice.isSelected() ? debtorAccountModel.getSelectedItem() : creditorAccountModel.getSelectedItem();
        InvoiceTemplate.Type type = rbSalesInvoice.isSelected() ? SALE : PURCHASE;
        InvoiceTemplate template = new InvoiceTemplate(type, tfId.getText(), date, tfDescription.getText(), invoiceLines);
        ledgerService.createInvoiceAndJournalForParties(document, account, template, Arrays.asList(parties));
    }

    private Party[] selectParties() {
        PartiesView partiesView = (PartiesView) viewFactory.createView(PartiesView.class);
        partiesView.setSelectionEnabled(true);
        partiesView.setMultiSelectionEnabled(true);
        ViewDialog dialog = new ViewDialog(getViewOwner().getWindow(), partiesView);
        dialog.showDialog();
        return partiesView.getSelectedParties();
    }

    private boolean validateInput(Date date, List<InvoiceTemplateLine> invoiceLines) {
        if (date == null) {
            MessageDialog.showMessage(this, "gen.titleError", "gen.invalidDate");
            return false;
        }

        for (InvoiceTemplateLine line : invoiceLines) {
            if (line.getAmountFormula() == null) {
                MessageDialog.showMessage(this, "gen.titleError",
                        "invoiceGeneratorView.emptyAmountsFound");
                return false;
            }

            if (line.getAccount() == null) {
                MessageDialog.showMessage(this, "gen.titleError", "invoiceGeneratorView.emptyAccountFound");
                return false;
            }
        }
        return true;
    }

    private void onInvoiceTypeChanged() {
        initDebtorOrCreditorSelection();
    }

}
