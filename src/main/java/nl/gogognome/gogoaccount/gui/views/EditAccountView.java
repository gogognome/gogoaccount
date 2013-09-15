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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import nl.gogognome.gogoaccount.businessobjects.Account;
import nl.gogognome.gogoaccount.businessobjects.Account.Type;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.gui.beans.ObjectFormatter;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.TextResource;

/**
 * This view is used to edit a new or existing account.
 *
 * @author Sander Kooijmans
 */
public class EditAccountView extends View {

	private final static List<Account.Type> types = Arrays.asList(Account.Type.ASSET, Account.Type.LIABILITY,
			Account.Type.EXPENSE, Account.Type.REVENUE);

	private Account initialAccount;

	private Account editedAccount;

	private StringModel idModel = new StringModel();
	private StringModel descriptionModel = new StringModel();
	private ListModel<Account.Type> accountTypeModel = new ListModel<Account.Type>(types);
	
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
		editedAccount = new Account(idModel.getString(), descriptionModel.getString(), accountTypeModel.getSelectedItem());
		closeAction.actionPerformed(null);
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

class AccountTypeFormatter implements ObjectFormatter<Account.Type> {

	private TextResource textResource;
	
	public AccountTypeFormatter(TextResource textResource) {
		this.textResource = textResource;
	}

	@Override
	public String format(Type type) {
		return type == null ? null : textResource.getString("gen.TYPE_" + type.name());
	}
	
}