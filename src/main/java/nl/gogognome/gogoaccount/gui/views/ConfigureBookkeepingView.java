package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionService;
import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionSettings;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.gui.beans.ObjectFormatter;
import nl.gogognome.lib.swing.*;
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
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

public class ConfigureBookkeepingView extends View {

    private static final long serialVersionUID = 1L;

    private final Document document;
    private final AutomaticCollectionService automaticCollectionService = ObjectFactory.create(AutomaticCollectionService.class);
    private final ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
    private final LedgerService ledgerService = ObjectFactory.create(LedgerService.class);

    private final StringModel descriptionModel = new StringModel();
    private final ListModel<Currency> currencyModel = new ListModel<>();
    private final DateModel startDateModel = new DateModel();
    private final StringModel organiztionNameModel = new StringModel();
    private final StringModel organiztionAddressModel = new StringModel();
    private final StringModel organiztionZipCodeModel = new StringModel();
    private final StringModel organiztionCityModel = new StringModel();
    private final StringModel organiztionCountryModel = new StringModel();
    private final BooleanModel enableAutomaticCollectionModel = new BooleanModel();
    private final StringModel ibanModel = new StringModel();
    private final StringModel bicModel = new StringModel();
    private final StringModel automaticCollectionContractNumberModel = new StringModel();
    private final StringModel sequenceNumberModel = new StringModel();

    private AccountTableModel tableModel;
    private JTable table;

    private ModelChangeListener modelChangeListener;

    private JButton editAccountButton;
    private JButton deleteAccountButton;

    private JPanel settingsPanel = new JPanel(new BorderLayout());
    private InputFieldsColumn automaticCollectionInputFields;

    /**
     * Constructor.
     * @param document the database whose bookkeeping is to be configured.
     */
    public ConfigureBookkeepingView(Document document) {
        this.document = document;
    }

    @Override
    public String getTitle() {
        return textResource.getString("ConfigureBookkeepingView.title");
    }

    @Override
    public void onInit() {
        try {
            initModels();
            addComponents();
            updateButtonState();
            addListeners();
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            close();
        }
    }

    private void initModels() throws ServiceException {
        Bookkeeping bookkeeping = ObjectFactory.create(ConfigurationService.class).getBookkeeping(document);
        startDateModel.setDate(bookkeeping.getStartOfPeriod());
        descriptionModel.setString(bookkeeping.getDescription());

        currencyModel.setItems(CurrencyUtil.getAllCurrencies());
        currencyModel.setSelectedItem(bookkeeping.getCurrency(), null);

        organiztionNameModel.setString(bookkeeping.getOrganizationName());
        organiztionAddressModel.setString(bookkeeping.getOrganizationAddress());
        organiztionCityModel.setString(bookkeeping.getOrganizationZipCode());
        organiztionZipCodeModel.setString(bookkeeping.getOrganizationCity());
        organiztionCountryModel.setString(bookkeeping.getOrganizationCountry());
        enableAutomaticCollectionModel.setBoolean(bookkeeping.isEnableAutomaticCollection());

        AutomaticCollectionSettings settings = automaticCollectionService.getSettings(document);
        ibanModel.setString(settings.getIban());
        bicModel.setString(settings.getBic());
        automaticCollectionContractNumberModel.setString(settings.getAutomaticCollectionContractNumber());
        sequenceNumberModel.setString(Long.toString(settings.getSequenceNumber()));
    }

    private void addListeners() {
        modelChangeListener = model -> updateDatabaseWithEnteredValues();
        startDateModel.addModelChangeListener(modelChangeListener);
        descriptionModel.addModelChangeListener(modelChangeListener);
        currencyModel.addModelChangeListener(modelChangeListener);
        organiztionNameModel.addModelChangeListener(modelChangeListener);
        organiztionAddressModel.addModelChangeListener(modelChangeListener);
        organiztionZipCodeModel.addModelChangeListener(modelChangeListener);
        organiztionCityModel.addModelChangeListener(modelChangeListener);
        organiztionCountryModel.addModelChangeListener(modelChangeListener);
        enableAutomaticCollectionModel.addModelChangeListener(modelChangeListener);
        ibanModel.addModelChangeListener(modelChangeListener);
        bicModel.addModelChangeListener(modelChangeListener);
        automaticCollectionContractNumberModel.addModelChangeListener(modelChangeListener);
        sequenceNumberModel.addModelChangeListener(modelChangeListener);

        enableAutomaticCollectionModel.addModelChangeListener(m -> updateVisibilityOfAutomaticCollectionInputFields());
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
        ifc.addField("ConfigureBookkeepingView.startDate", startDateModel);
        ifc.addComboBoxField("ConfigureBookkeepingView.currency", currencyModel, new CurrencyFormatter());
        ifc.addField("ConfigureBookkeepingView.organizationName", organiztionNameModel);
        ifc.addField("ConfigureBookkeepingView.organizationAddress", organiztionAddressModel);
        ifc.addField("ConfigureBookkeepingView.organizationZipCode", organiztionZipCodeModel);
        ifc.addField("ConfigureBookkeepingView.organizationCity", organiztionCityModel);
        ifc.addField("ConfigureBookkeepingView.organizationCountry", organiztionCountryModel);
        ifc.addField("ConfigureBookkeepingView.enableAutomaticCollection", enableAutomaticCollectionModel);

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
        table = widgetFactory.createSortedTable(tableModel);
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
        removeListeners();
    }

    private void removeListeners() {
        startDateModel.removeModelChangeListener(modelChangeListener);
        descriptionModel.removeModelChangeListener(modelChangeListener);
        currencyModel.removeModelChangeListener(modelChangeListener);
    }

    private void updateDatabaseWithEnteredValues() {
        try {
            Bookkeeping bookkeeping = ObjectFactory.create(ConfigurationService.class).getBookkeeping(document);
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
            bookkeeping.setOrganizationName(organiztionNameModel.getString());
            bookkeeping.setOrganizationAddress(organiztionAddressModel.getString());
            bookkeeping.setOrganizationZipCode(organiztionZipCodeModel.getString());
            bookkeeping.setOrganizationCity(organiztionCityModel.getString());
            bookkeeping.setOrganizationCountry(organiztionCountryModel.getString());
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
                    MessageDialog.showErrorMessage(this, "gen.invalidNumber", textResource.getString("ConfigureBookkeepingView.sequenceNumber"));
                    settings.setSequenceNumber(0);
                    // probably incorrect syntax
                }
                automaticCollectionService.setSettings(document, settings);
            }
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
        }
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
        AccountDefinition accountDefintion = null;
        int[] rows = SwingUtils.getSelectedRowsConvertedToModel(table);
        if (rows.length == 1) {
            accountDefintion = tableModel.getRow(rows[0]);
        }
        return accountDefintion;
    }

    private void onDeleteAccount() {
        AccountDefinition accountDefintion = getSelectedAccountDefinition();
        Account account = accountDefintion.account;
        int choice = MessageDialog.showYesNoQuestion(this, "ConfigureBookkeepingView.deleteAccountTitle",
                "ConfigureBookkeepingView.deleteAccountAreYouSure",	account.getId() + " - " + account.getName());
        if (choice == MessageDialog.YES_OPTION) {
            try {
                configurationService.deleteAccount(document, account);
                int index = SwingUtils.getSelectedRowConvertedToModel(table);
                tableModel.removeRow(index);
            } catch (ServiceException e) {
                MessageDialog.showErrorMessage(getParentWindow(), e, "ConfigureBookkeepingView.deleteAccountException");
            }
        }
    }

    private void onAddAccount() {
        HandleException.for_(this, () -> {
            EditAccountView eav = new EditAccountView(null);
            ViewDialog dialog = new ViewDialog(getParentWindow(), eav);
            dialog.showDialog();
            Account account = eav.getEditedAccount();
            if (account != null) {
                try {
                    configurationService.createAccount(document, account);
                    AccountDefinition definition = new AccountDefinition();
                    definition.account = account;
                    tableModel.addRow(definition);
                } catch (ServiceException e) {
                    MessageDialog.showErrorMessage(this, e, "ConfigureBookkeepingView.addAccountException");
                }
            }
        });
    }

    private void onEditAccount() {
        HandleException.for_(this, () -> {
            int[] rows = SwingUtils.getSelectedRowsConvertedToModel(table);
            if (rows.length == 1) {
                AccountDefinition definition = tableModel.getRow(rows[0]);
                EditAccountView eav = new EditAccountView(definition.account);

                ViewDialog dialog = new ViewDialog(getParentWindow(), eav);
                dialog.showDialog();
                Account account = eav.getEditedAccount();
                if (account != null) {
                    try {
                        configurationService.updateAccount(document, account);
                        definition.account = account;
                        tableModel.updateRow(rows[0], definition);
                    } catch (ServiceException e) {
                        MessageDialog.showErrorMessage(this, e, "ConfigureBookkeepingView.updateAccountException");
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

    private static class AccountTableModel extends AbstractListTableModel<AccountDefinition> {

        private final static ColumnDefinition ID =
                new ColumnDefinition("ConfigureBookkeepingView.id", String.class, 50);

        private final static ColumnDefinition NAME =
                new ColumnDefinition("ConfigureBookkeepingView.name", String.class, 400);

        private final static ColumnDefinition USED =
                new ColumnDefinition("ConfigureBookkeepingView.used", Icon.class, 50);

        private final static ColumnDefinition TYPE =
                new ColumnDefinition("ConfigureBookkeepingView.type", String.class, 150);

        private final static List<ColumnDefinition> COLUMN_DEFINITIONS = Arrays.asList(
                ID, NAME, USED, TYPE
                );

        public AccountTableModel(List<AccountDefinition> accountDefinitions) {
            super(COLUMN_DEFINITIONS, accountDefinitions);
        }

        /** {@inheritDoc} */
        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            ColumnDefinition colDef = getColumnDefinition(columnIndex);
            if (ID.equals(colDef)) {
                return getRow(rowIndex).account.getId();
            } else if (NAME.equals(colDef)) {
                return getRow(rowIndex).account.getName();
            } else if (USED.equals(colDef)) {
                return getRow(rowIndex).used ?
                        Factory.getInstance(WidgetFactory.class).createIcon("ConfigureBookkeepingView.tickIcon16") : null;
            } else if (TYPE.equals(colDef)) {
                return Factory.getInstance(TextResource.class).getString("gen.ACCOUNTTYPE_" +
                        getRow(rowIndex).account.getType().name());
            }
            return null;
        }
    }
}
