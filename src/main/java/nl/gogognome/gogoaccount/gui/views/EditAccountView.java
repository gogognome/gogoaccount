package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.AccountType;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.gui.beans.ObjectFormatter;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.TextResource;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import static nl.gogognome.gogoaccount.component.configuration.AccountType.*;

/**
 * This view is used to edit a new or existing account.
 */
public class EditAccountView extends View {

    private static final long serialVersionUID = 1L;

    private final static List<AccountType> types = Arrays.asList(ASSET, DEBTOR, LIABILITY, CREDITOR, EQUITY,
            EXPENSE, REVENUE);

    private final Account initialAccount;

    private Account editedAccount;

    private final StringModel idModel = new StringModel().mustBeFilled(true);
    private final StringModel descriptionModel = new StringModel().mustBeFilled(true);
    private final ListModel<AccountType> accountTypeModel = new ListModel<>(types).mustBeFilled(true);

    public EditAccountView(Account initialAccount) {
        this.initialAccount = initialAccount;
    }

    @Override
    public String getTitle() {
        return textResource.getString(
                initialAccount != null ? "editAccountView.titleEdit" : "editAccountView.titleAdd");
    }

    @Override
    public void onClose() {
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(400, 170);
    }

    @Override
    public void onInit() {
        setBorder(new EmptyBorder(10, 10, 10, 10));

        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.addField("editAccountView.id", idModel);
        ifc.addField("editAccountView.description", descriptionModel);
        ifc.addComboBoxField("editAccountView.type", accountTypeModel, new AccountTypeFormatter(textResource));

        if (initialAccount != null) {
            idModel.setEnabled(false, null);
            idModel.setString(initialAccount.getId());
            descriptionModel.setString(initialAccount.getName());
            accountTypeModel.setSelectedItem(initialAccount.getType(), null);
        }

        // Create button panel
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        JButton button = widgetFactory.createButton("gen.ok", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });
        buttonPanel.add(button);
        setDefaultButton(button);

        button = widgetFactory.createButton("gen.cancel", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        buttonPanel.add(button);

        // Put all panels on the view.
        setLayout(new BorderLayout());
        add(ifc, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void onOk() {
        if (!validateInput()) {
            return;
        }

        editedAccount = new Account(idModel.getString(), descriptionModel.getString(), accountTypeModel.getSelectedItem());
        closeAction.actionPerformed(null);
    }

    private boolean validateInput() {
        return idModel.validate() & descriptionModel.validate() & accountTypeModel.validate();
    }

    private void onCancel() {
        closeAction.actionPerformed(null);
    }

    /**
     * Gets the account that has been edited by the user.
     * @return the account
     */
    public Account getEditedAccount() {
        return editedAccount;
    }
}

class AccountTypeFormatter implements ObjectFormatter<AccountType> {

    private final TextResource textResource;

    public AccountTypeFormatter(TextResource textResource) {
        this.textResource = textResource;
    }

    @Override
    public String format(AccountType type) {
        return type == null ? null : textResource.getString("gen.ACCOUNTTYPE_" + type.name());
    }

}