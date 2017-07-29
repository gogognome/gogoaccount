package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionService;
import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionSettings;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.gui.beans.ObjectFormatter;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.ListTableModel;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.*;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.CurrencyUtil;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.lib.util.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static java.util.Arrays.asList;

public class ConfigureBookkeepingView extends View {

    private static final long serialVersionUID = 1L;

    private final Document document;
    private final AutomaticCollectionService automaticCollectionService;
    private final ConfigurationService configurationService;
    private final LedgerService ledgerService;
    private final MessageDialog messageDialog;
    private final HandleException handleException;

    private StringModel descriptionModel;
    private ListModel<Currency> currencyModel;
    private DateModel startDateModel;
    private StringModel invoiceIdFormatModel;
    private StringModel organisationNameModel;
    private StringModel organizationAddressModel;
    private StringModel organizationZipCodeModel;
    private StringModel organizationCityModel;
    private StringModel organizationCountryModel;
    private BooleanModel enableAutomaticCollectionModel;
    private StringModel ibanModel;
    private StringModel bicModel;
    private StringModel automaticCollectionContractNumberModel;
    private StringModel sequenceNumberModel;

    private AccountTableModel tableModel;
    private JTable table;

    private JButton editAccountButton;
    private JButton deleteAccountButton;

    private JPanel settingsPanel = new JPanel(new BorderLayout());
    private InputFieldsColumn automaticCollectionInputFields;

    public ConfigureBookkeepingView(Document document, AutomaticCollectionService automaticCollectionService, ConfigurationService configurationService, LedgerService ledgerService) {
        this.document = document;
        this.automaticCollectionService = automaticCollectionService;
        this.configurationService = configurationService;
        this.ledgerService = ledgerService;
        messageDialog = new MessageDialog(textResource, this);
        handleException = new HandleException(messageDialog);
    }

    @Override
    public String getTitle() {
        return textResource.getString("ConfigureBookkeepingView.title");
    }

    @Override
    public void onInit() {
        handleException.of(() -> {
            initModels();
            addComponents();
            updateButtonState();
        });
    }

    private void initModels() throws ServiceException {
        Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
        ModelChangeListener modelChangeListener = model -> updateDatabaseWithEnteredValues();
        startDateModel = Model.of(bookkeeping.getStartOfPeriod(), v -> {}, modelChangeListener);
        descriptionModel = Model.of(bookkeeping.getDescription(), v -> {}, modelChangeListener);

        currencyModel = Model.of(CurrencyUtil.getAllCurrencies(), v -> {});
        currencyModel.setSelectedItem(bookkeeping.getCurrency(), null);
        invoiceIdFormatModel = Model.of(bookkeeping.getInvoiceIdFormat(), v -> {}, modelChangeListener);

        organisationNameModel = Model.of(bookkeeping.getOrganizationName(), v -> {}, modelChangeListener);
        organizationAddressModel = Model.of(bookkeeping.getOrganizationAddress(), v -> {}, modelChangeListener);
        organizationCityModel = Model.of(bookkeeping.getOrganizationCity(), v -> {}, modelChangeListener);
        organizationZipCodeModel = Model.of(bookkeeping.getOrganizationZipCode(), v -> {}, modelChangeListener);
        organizationCountryModel = Model.of(bookkeeping.getOrganizationCountry(), v -> {}, modelChangeListener);
        enableAutomaticCollectionModel = Model.of(bookkeeping.isEnableAutomaticCollection(), v -> {}, modelChangeListener);
        enableAutomaticCollectionModel.addModelChangeListener(m -> updateVisibilityOfAutomaticCollectionInputFields());

        AutomaticCollectionSettings settings = automaticCollectionService.getSettings(document);
        ibanModel = Model.of(settings.getIban(), v -> {}, modelChangeListener);
        bicModel = Model.of(settings.getBic(), v -> {}, modelChangeListener);
        automaticCollectionContractNumberModel = Model.of(settings.getAutomaticCollectionContractNumber(), v -> {}, modelChangeListener);
        sequenceNumberModel = Model.of(Long.toString(settings.getSequenceNumber()), v -> {}, modelChangeListener);
    }

    private void updateVisibilityOfAutomaticCollectionInputFields() {
        if (enableAutomaticCollectionModel.getBoolean()) {
            settingsPanel.add(automaticCollectionInputFields, BorderLayout.SOUTH);
        } else {
            settingsPanel.remove(automaticCollectionInputFields);
        }
        validate();
    }

    private void addComponents() throws ServiceException {
        setLayout(new BorderLayout());

        InputFieldsColumn ifc = new InputFieldsColumn();
        automaticCollectionInputFields = new InputFieldsColumn();
        addCloseable(ifc);
        addCloseable(automaticCollectionInputFields);
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("ConfigureBookkeepingView.generalSettings"));

        ifc.addField("ConfigureBookkeepingView.description", descriptionModel);
        ifc.addField("ConfigureBookkeepingView.organizationName", organisationNameModel);
        ifc.addField("ConfigureBookkeepingView.organizationAddress", organizationAddressModel);
        ifc.addField("ConfigureBookkeepingView.organizationZipCode", organizationZipCodeModel);
        ifc.addField("ConfigureBookkeepingView.organizationCity", organizationCityModel);
        ifc.addField("ConfigureBookkeepingView.organizationCountry", organizationCountryModel);
        ifc.addField("ConfigureBookkeepingView.enableAutomaticCollection", enableAutomaticCollectionModel);
        ifc.addField("ConfigureBookkeepingView.startDate", startDateModel);
        ifc.addComboBoxField("ConfigureBookkeepingView.currency", currencyModel, new CurrencyFormatter());
        ifc.addField("ConfigureBookkeepingView.invoiceIdFormat", invoiceIdFormatModel);
        ifc.addVariableSizeField("gen.emptyString", widgetFactory.createLabel("ConfigureBookkeepingView.invoiceIdFormat.explanation"));

        automaticCollectionInputFields.setBorder(widgetFactory.createTitleBorderWithPadding("ConfigureBookkeepingView.automaticCollectionSettings"));
        automaticCollectionInputFields.addField("ConfigureBookkeepingView.iban", ibanModel);
        automaticCollectionInputFields.addField("ConfigureBookkeepingView.bic", bicModel);
        automaticCollectionInputFields.addField("ConfigureBookkeepingView.automaticCollectionContractNumber", automaticCollectionContractNumberModel);
        automaticCollectionInputFields.addField("ConfigureBookkeepingView.sequenceNumber", sequenceNumberModel);

        // Create panel with accounts table
        JPanel accountsAndButtonsPanel = new JPanel(new BorderLayout());
        accountsAndButtonsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(textResource.getString("ConfigureBookkeepingView.accounts")),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        tableModel = new AccountTableModel(getAccountDefinitions(document));
        table = Tables.createSortedTable(tableModel);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> updateButtonState());
        accountsAndButtonsPanel.add(widgetFactory.createScrollPane(table), BorderLayout.CENTER);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.TOP, SwingConstants.VERTICAL);
        buttonPanel.addButton("ConfigureBookkeepingView.addAccount", new AddAccountAction());
        editAccountButton = buttonPanel.addButton("ConfigureBookkeepingView.editAccount",
                new EditAccountAction());
        deleteAccountButton = buttonPanel.addButton("ConfigureBookkeepingView.deleteAccount",
                new DeleteAccountAction());
        accountsAndButtonsPanel.add(buttonPanel, BorderLayout.EAST);

        // Add panels to view
        settingsPanel.add(ifc, BorderLayout.NORTH);
        updateVisibilityOfAutomaticCollectionInputFields();

        add(settingsPanel, BorderLayout.NORTH);
        add(accountsAndButtonsPanel, BorderLayout.CENTER);
    }

    @Override
    public void onClose() {
    }

    private void updateDatabaseWithEnteredValues() {
        handleException.of(() -> {
            Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
            bookkeeping.setDescription(descriptionModel.getString());
            if (startDateModel.getDate() != null) {
                bookkeeping.setStartOfPeriod(startDateModel.getDate());
            }

            try {
                Currency currency = currencyModel.getSelectedItem();
                if (currency != null) {
                    bookkeeping.setCurrency(currency);
                }
            } catch (Exception e) {
                throw new ServiceException("Invalid currency entered");
            }
            bookkeeping.setOrganizationName(organisationNameModel.getString());
            bookkeeping.setOrganizationAddress(organizationAddressModel.getString());
            bookkeeping.setOrganizationZipCode(organizationZipCodeModel.getString());
            bookkeeping.setOrganizationCity(organizationCityModel.getString());
            bookkeeping.setOrganizationCountry(organizationCountryModel.getString());
            bookkeeping.setInvoiceIdFormat(invoiceIdFormatModel.getString());
            bookkeeping.setEnableAutomaticCollection(enableAutomaticCollectionModel.getBoolean());
            configurationService.updateBookkeeping(document, bookkeeping);

            if (enableAutomaticCollectionModel.getBoolean()) {
                AutomaticCollectionSettings settings = automaticCollectionService.getSettings(document);
                settings.setIban(ibanModel.getString());
                settings.setBic(bicModel.getString());
                settings.setAutomaticCollectionContractNumber(automaticCollectionContractNumberModel.getString());
                try {
                    if (!StringUtil.isNullOrEmpty(sequenceNumberModel.getString())) {
                        settings.setSequenceNumber(Long.parseLong(sequenceNumberModel.getString()));
                    }
                } catch (Exception e) {
                    messageDialog.showWarningMessage("gen.invalidNumber", textResource.getString("ConfigureBookkeepingView.sequenceNumber"));
                    settings.setSequenceNumber(0);
                    // probably incorrect syntax
                }
                automaticCollectionService.setSettings(document, settings);
            }
        });
    }

    private void updateButtonState() {
        AccountDefinition accountDefinition = getSelectedAccountDefinition();
        deleteAccountButton.setEnabled(accountDefinition != null && !accountDefinition.used);
        editAccountButton.setEnabled(accountDefinition != null);
    }

    /**
     * Gets the definition of the selected account.
     * @return the definition of the selected account or <code>null</code> if not exactly one
     *         row has been selected
     */
    private AccountDefinition getSelectedAccountDefinition() {
        AccountDefinition accountDefinition = null;
        int[] rows = Tables.getSelectedRowsConvertedToModel(table);
        if (rows.length == 1) {
            accountDefinition = tableModel.getRow(rows[0]);
        }
        return accountDefinition;
    }

    private void onDeleteAccount() {
        AccountDefinition accountDefinition = getSelectedAccountDefinition();
        Account account = accountDefinition.account;
        int choice = messageDialog.showYesNoQuestion("ConfigureBookkeepingView.deleteAccountTitle",
                "ConfigureBookkeepingView.deleteAccountAreYouSure",	account.getId() + " - " + account.getName());
        if (choice == MessageDialog.YES_OPTION) {
            try {
                configurationService.deleteAccount(document, account);
                int index = Tables.getSelectedRowConvertedToModel(table);
                tableModel.removeRow(index);
            } catch (ServiceException e) {
                messageDialog.showErrorMessage(e, "ConfigureBookkeepingView.deleteAccountException");
            }
        }
    }

    private void onAddAccount() {
        handleException.of(() -> {
            EditAccountView eav = new EditAccountView(null);
            ViewDialog dialog = new ViewDialog(getViewOwner().getWindow(), eav);
            dialog.showDialog();
            Account account = eav.getEditedAccount();
            if (account != null) {
                try {
                    configurationService.createAccount(document, account);
                    AccountDefinition definition = new AccountDefinition();
                    definition.account = account;
                    tableModel.addRow(definition);
                } catch (ServiceException e) {
                    messageDialog.showErrorMessage(e, "ConfigureBookkeepingView.addAccountException");
                }
            }
        });
    }

    private void onEditAccount() {
        handleException.of(() -> {
            int[] rows = Tables.getSelectedRowsConvertedToModel(table);
            if (rows.length == 1) {
                AccountDefinition definition = tableModel.getRow(rows[0]);
                EditAccountView eav = new EditAccountView(definition.account);

                ViewDialog dialog = new ViewDialog(getViewOwner().getWindow(), eav);
                dialog.showDialog();
                Account account = eav.getEditedAccount();
                if (account != null) {
                    try {
                        configurationService.updateAccount(document, account);
                        definition.account = account;
                        tableModel.updateRow(rows[0], definition);
                    } catch (ServiceException e) {
                        messageDialog.showErrorMessage(e, "ConfigureBookkeepingView.updateAccountException");
                    }
                }
            }
        });
    }

    private List<AccountDefinition> getAccountDefinitions(Document document) throws ServiceException {
        List<Account> accounts = configurationService.findAllAccounts(document);
        List<AccountDefinition> result = new ArrayList<>(accounts.size());
        for (Account account : accounts) {
            AccountDefinition accountDefinition = new AccountDefinition();
            accountDefinition.account = account;
            accountDefinition.used = ledgerService.isAccountUsed(document, account.getId());
            result.add(accountDefinition);
        }
        return result;
    }

    private final class AddAccountAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent evt) {
            onAddAccount();
        }
    }

    private final class EditAccountAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent evt) {
            onEditAccount();
        }
    }

    private final class DeleteAccountAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent evt) {
            onDeleteAccount();
        }
    }

    private static class AccountDefinition {
        public Account account;
        public boolean used;
    }

    private static class CurrencyFormatter implements ObjectFormatter<Currency> {
        @Override
        public String format(Currency currency) {
            return currency.getCurrencyCode();
        }
    }

    private static class AccountTableModel extends ListTableModel<AccountDefinition> {

        AccountTableModel(List<AccountDefinition> accountDefinitions) {
            super(asList(
                    ColumnDefinition.<AccountDefinition>builder("ConfigureBookkeepingView.id", String.class, 50)
                        .add(row -> row.account.getId())
                        .build(),
                    ColumnDefinition.<AccountDefinition>builder("ConfigureBookkeepingView.name", String.class, 400)
                        .add(row -> row.account.getName())
                        .build(),
                    ColumnDefinition.<AccountDefinition>builder("ConfigureBookkeepingView.used", Icon.class, 50)
                        .add(row -> row.used ? Factory.getInstance(WidgetFactory.class).createIcon("ConfigureBookkeepingView.tickIcon16") : null)
                        .build(),
                    ColumnDefinition.<AccountDefinition>builder("ConfigureBookkeepingView.type", String.class, 150)
                        .add(row -> Factory.getInstance(TextResource.class).getString("gen.ACCOUNTTYPE_" + row.account.getType().name()))
                        .build()
            ));
            setRows(accountDefinitions);
        }
    }
}
