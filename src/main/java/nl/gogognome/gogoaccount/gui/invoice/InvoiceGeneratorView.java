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
import nl.gogognome.lib.gui.Closeable;
import nl.gogognome.lib.gui.beans.Bean;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
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
    private final MessageDialog messageDialog;
    private final HandleException handleException;

    private final Document document;
    private final AmountFormulaParser amountFormulaParser;
    private final ViewFactory viewFactory;

    private List<Account> accounts;

    private JPanel debtorOrCreditorPanel = new JPanel(new GridBagLayout());
    private StringModel partyReferenceModel;
    private StringModel descriptionModel;
    private DateModel invoiceGenerationDateModel;
    private JRadioButton rbPurchaseInvoice;
    private JRadioButton rbSalesInvoice;
    private JButton addInvoicesToBookkeepingButton;

    private ListModel<Account> debtorAccountModel;
    private ListModel<Account> creditorAccountModel;

    private class InvoiceTemplateLineModel {
        private final ListModel<Account> accountListModel = new ListModel<>();
        private StringModel descriptionModel;
        private StringModel amountModel;
        private AmountFormula amountFormula;

        InvoiceTemplateLineModel(List<Account> accounts) {
            amountModel = new StringModel();
            descriptionModel = new StringModel();
            accountListModel.setItems(accounts);
            accountListModel.addModelChangeListener(
                    model -> descriptionModel.setString(accountListModel.getSelectedItem() != null ? accountListModel.getSelectedItem().getName() : null));
            amountModel.addModelChangeListener(model -> {
                if (amountModel.getString().indexOf('|') != amountModel.getString().lastIndexOf('|')) {
                    // Use invokeLater because this code is called while a mutation notification is handled.
                    // It is not allowed to change the model right now.
                    SwingUtilities.invokeLater(() -> updateTemplateLine(amountModel.getString()));
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

    private final List<InvoiceTemplateLineModel> invoiceTemplateLineModels = new ArrayList<>();
    private JPanel templateLinesPanel;
    private List<Bean> templateLineBeans = new ArrayList<>();

    public InvoiceGeneratorView(Document document, ConfigurationService configurationService, LedgerService ledgerService, AmountFormulaParser amountFormulaParser, ViewFactory viewFactory) {
        this.configurationService = configurationService;
        this.ledgerService = ledgerService;
        this.document = document;
        this.amountFormulaParser = amountFormulaParser;
        this.viewFactory = viewFactory;

        messageDialog = new MessageDialog(textResource, this);
        handleException = new HandleException(messageDialog);
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
            messageDialog.showErrorMessage(e, "gen.error");
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

        initDebtorOrCreditorSelection();
    }

    private ButtonPanel createButtonPanel() {
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
        addInvoicesToBookkeepingButton = widgetFactory.createButton("invoiceGeneratorView.addInvoices", this::onAddInvoicesToBookkeeping);
        buttonPanel.add(addInvoicesToBookkeepingButton);
        buttonPanel.setBorder(new EmptyBorder(20, 0, 0, 0));
        return buttonPanel;
    }

    private void initTemplateLinesPanel() {
        templateLinesPanel = new JPanel(new GridBagLayout());

        // Add one empty line so the user can start editing the template.
        invoiceTemplateLineModels.add(new InvoiceTemplateLineModel(accounts));

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

        rbPurchaseInvoice = new JRadioButton();
        invoiceTypePanel.add(rbPurchaseInvoice,
                SwingUtils.createGBConstraints(0, row, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.NONE,
                        3, 0, 3, 5));
        invoiceTypePanel.add(new JLabel(textResource.getString("invoiceGeneratorView.purchaseInvoice")),
                SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        rbSalesInvoice.addActionListener(a -> initDebtorOrCreditorSelection());
        rbPurchaseInvoice.addActionListener(a -> initDebtorOrCreditorSelection());
        ButtonGroup invoiceTypeButtonGroup = new ButtonGroup();
        invoiceTypeButtonGroup.add(rbSalesInvoice);
        invoiceTypeButtonGroup.add(rbPurchaseInvoice);

        return invoiceTypePanel;
    }

    private void initDebtorOrCreditorSelection() {
        debtorOrCreditorPanel.removeAll();

        InputFieldsColumn ifc = new InputFieldsColumn();
        if (rbSalesInvoice.isSelected()) {
            ifc.addComboBoxField("invoiceGeneratorView.debtorAccount", debtorAccountModel, new AccountFormatter());
        } else if (rbPurchaseInvoice.isSelected()) {
            ifc.addComboBoxField("invoiceGeneratorView.creditorAccount", creditorAccountModel, new AccountFormatter());
        }

        debtorOrCreditorPanel.add(ifc, SwingUtils.createLabelGBConstraints(0, 0));
        debtorOrCreditorPanel.add(new JLabel(""), SwingUtils.createTextFieldGBConstraints(1, 0));

        onTypeSelected();

        repaint();
        revalidate();
    }

    private void onTypeSelected() {
        boolean typeSelected = rbPurchaseInvoice.isSelected() || rbSalesInvoice.isSelected();
        partyReferenceModel.setEnabled(typeSelected, null);
        descriptionModel.setEnabled(typeSelected, null);
        invoiceGenerationDateModel.setEnabled(typeSelected, null);
        addInvoicesToBookkeepingButton.setEnabled(typeSelected);
        updateTemplateLinesPanel();
    }

    private JPanel createHeaderPanel() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        partyReferenceModel = new StringModel();
        ifc.addField("invoiceGeneratorView.partyReference", partyReferenceModel);

        invoiceGenerationDateModel = new DateModel();
        invoiceGenerationDateModel.setDate(new Date(), null);
        ifc.addField("invoiceGeneratorView.date", invoiceGenerationDateModel);

        descriptionModel = new StringModel();
        ifc.addField("invoiceGeneratorView.description", descriptionModel);

        ifc.addVariableSizeField("gen.emptyString", widgetFactory.createLabel("invoiceGeneratorView.tooltip"));

        ifc.setBorder(new EmptyBorder(0, 0, 12, 0));
        return ifc;
    }

    private void updateTemplateLinesPanelAndRepaint() {
        updateTemplateLinesPanel();
        revalidate();
        repaint();
    }

    private void updateTemplateLinesPanel() {
        clearTemplateLinesPanel();

        int row = 0;
        createTemplatePanelHeader(row++);
        for (int i = 0; i< invoiceTemplateLineModels.size(); i++) {
            addLineToTemplateLinePanel(row++, i);
        }
        addNewButtonToTemplateLinesPanel(row++);
    }

    private void clearTemplateLinesPanel() {
        templateLinesPanel.removeAll();
        templateLineBeans.forEach(Closeable::close);
        templateLineBeans.clear();
    }

    private void createTemplatePanelHeader(int row) {
        templateLinesPanel.add(new JLabel(textResource.getString("gen.account")),
                SwingUtils.createLabelGBConstraints(1, row));
        templateLinesPanel.add(new JLabel(textResource.getString("gen.description")),
                SwingUtils.createLabelGBConstraints(2, row));
        templateLinesPanel.add(new JLabel(textResource.getString("gen.amount")),
                SwingUtils.createLabelGBConstraints(3, row));
    }

    private void addLineToTemplateLinePanel(int row, int index) {
        boolean typeSelected = rbPurchaseInvoice.isSelected() || rbSalesInvoice.isSelected();

        InvoiceTemplateLineModel line = invoiceTemplateLineModels.get(index);
        Bean cbAccount = beanFactory.createComboBoxBean(line.accountListModel, new AccountFormatter());
        line.accountListModel.setEnabled(typeSelected, null);
        templateLinesPanel.add(cbAccount.getComponent(),
                SwingUtils.createLabelGBConstraints(1, row));

        Bean descriptionBean = beanFactory.createTextFieldBean(line.descriptionModel);
        line.descriptionModel.setEnabled(typeSelected, null);
        templateLineBeans.add(descriptionBean);
        templateLinesPanel.add(descriptionBean.getComponent(),
                SwingUtils.createTextFieldGBConstraints(2, row));

        Bean amountBean = beanFactory.createRightAlignedTextFieldBean(line.amountModel);
        line.amountModel.setEnabled(typeSelected, null);
        templateLineBeans.add(amountBean);
        templateLinesPanel.add(amountBean.getComponent(),
                SwingUtils.createTextFieldGBConstraints(3, row));

        JButton deleteButton = widgetFactory.createButton("invoiceGeneratorView.delete", () -> onDelete(index));
        deleteButton.setEnabled(typeSelected);
        templateLinesPanel.add(deleteButton,
                SwingUtils.createLabelGBConstraints(4, row));
    }

    private void addNewButtonToTemplateLinesPanel(int row) {
        JButton newButton = widgetFactory.createButton("invoiceGeneratorView.new", () -> {
                    invoiceTemplateLineModels.add(new InvoiceTemplateLineModel(accounts));
                    updateTemplateLinesPanelAndRepaint();
                    SwingUtilities.invokeLater(() -> templateLinesPanel.getComponent(templateLinesPanel.getComponentCount() - 5).requestFocus());
                }
        );
        newButton.setEnabled(rbPurchaseInvoice.isSelected() || rbSalesInvoice.isSelected());
        templateLinesPanel.add(newButton, SwingUtils.createLabelGBConstraints(4, row));
    }

    private void onDelete(int index) {
        invoiceTemplateLineModels.remove(index);
        updateTemplateLinesPanelAndRepaint();
    }

    private void onAddInvoicesToBookkeeping() {
        handleException.of(() -> {
            Date date = invoiceGenerationDateModel.getDate();
            List<InvoiceTemplateLine> invoiceLines = invoiceTemplateLineModels.stream()
                    .map(line -> new InvoiceTemplateLine(line.amountFormula, line.descriptionModel.getString(), line.accountListModel.getSelectedItem()))
                    .collect(toList());

            if (!validateInput(date, invoiceLines)) {
                return;
            }

            Party[] parties = selectParties();
            if (parties == null) {
                return;
            }

            int choice = messageDialog.showYesNoQuestion("gen.titleWarning", "invoiceGeneratorView.areYouSure");
            if (choice != MessageDialog.YES_OPTION) {
                return;
            }

            generateInvoices(date, invoiceLines, parties);
            messageDialog.showInfoMessage("invoiceGeneratorView.messageSuccess");
        });
    }

    private void generateInvoices(Date date, List<nl.gogognome.gogoaccount.component.invoice.InvoiceTemplateLine> invoiceLines, Party[] parties) throws ServiceException {
        Account account = rbSalesInvoice.isSelected() ? debtorAccountModel.getSelectedItem() : creditorAccountModel.getSelectedItem();
        InvoiceTemplate.Type type = rbSalesInvoice.isSelected() ? SALE : PURCHASE;
        InvoiceTemplate template = new InvoiceTemplate(type, partyReferenceModel.getString(), date, descriptionModel.getString(), invoiceLines);
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

    private boolean validateInput(Date date, List<nl.gogognome.gogoaccount.component.invoice.InvoiceTemplateLine> invoiceLines) {
        if (date == null) {
            messageDialog.showWarningMessage("gen.invalidDate");
            return false;
        }

        for (nl.gogognome.gogoaccount.component.invoice.InvoiceTemplateLine line : invoiceLines) {
            if (line.getAmountFormula() == null) {
                messageDialog.showWarningMessage("invoiceGeneratorView.emptyAmountsFound");
                return false;
            }

            if (line.getAccount() == null) {
                messageDialog.showWarningMessage("invoiceGeneratorView.emptyAccountFound");
                return false;
            }
        }
        return true;
    }

}
