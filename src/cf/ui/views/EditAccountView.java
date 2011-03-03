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
import java.util.Arrays;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import nl.gogognome.framework.View;
import nl.gogognome.swing.ButtonPanel;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import cf.engine.Account;
import cf.engine.Account.Type;

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

	private JTextField tfId;
	private JTextField tfDescription;
	private JComboBox cmType;

	public EditAccountView(Account initialAccount) {
		this.initialAccount = initialAccount;
	}

	@Override
	public String getTitle() {
        return TextResource.getInstance().getString(
        		initialAccount != null ? "editAccountView.titleEdit" : "editAccountView.titleAdd");
	}

	@Override
	public void onClose() {
	}

	@Override
	public void onInit() {
        WidgetFactory wf = WidgetFactory.getInstance();

        // Create panel with ID, issue date, concerning party, paying party and amount to be paid.
        GridBagLayout gbl = new GridBagLayout();
        JPanel topPanel = new JPanel(gbl);

        String id;
        String description;
        if (initialAccount != null) {
        	id = initialAccount.getId();
        	description = initialAccount.getName();
        } else {
        	id = "";
        	description = "";
        }

        tfId = wf.createTextField(id);
        if (initialAccount != null) {
            tfId.setEditable(false); // prevent changing the id of an existing account
            tfId.setEnabled(false);
        }
        int row = 0;
        JLabel label = wf.createLabel("editAccountView.id", tfId);
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        topPanel.add(tfId, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        tfDescription = wf.createTextField(description);
        label = wf.createLabel("editAccountView.description", tfDescription);
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        topPanel.add(tfDescription, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        cmType = new JComboBox();
        TextResource tr = TextResource.getInstance();
        for (Account.Type type : types) {
        	cmType.addItem(tr.getString("gen.TYPE_" + type.name()));
        }
        label = wf.createLabel("editAccountView.type", cmType);
        topPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        topPanel.add(cmType, SwingUtils.createTextFieldGBConstraints(1, row));
        row++;

        // Create button panel
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        JButton button = wf.createButton("gen.ok", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        });
        buttonPanel.add(button);
        setDefaultButton(button);

        button = wf.createButton("gen.cancel", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });
        buttonPanel.add(button);

        // Put all panels on the view.
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);
	}

	private void onOk() {
		Account.Type type = types.get(cmType.getSelectedIndex());
		editedAccount = new Account(tfId.getText(), tfDescription.getText(),
				type == Type.ASSET || type == Type.EXPENSE, type);
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
