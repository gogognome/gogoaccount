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
package cf.ui.components;

import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;

import nl.gogognome.lib.gui.beans.BeanFactory;
import nl.gogognome.lib.gui.beans.ComboBoxBean;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.lib.util.StringUtil;

/**
 * Bean for selecting the type of a party. It also contains a button that allows the user
 * to add a new party type.
 *
 * @author Sander Kooijmans
 */
public class PartyTypeBean extends JPanel {

	private static final long serialVersionUID = 1L;

	private ListModel<String> types;

	public PartyTypeBean(ListModel<String> types) {
		this.types = types;
		initBean();
	}

	private void initBean() {
        setLayout(new GridBagLayout());
        BeanFactory beanFactory = Factory.getInstance(BeanFactory.class);
        ComboBoxBean<String> comboboxBean = beanFactory.createComboBoxBean(types);

        add(comboboxBean, SwingUtils.createTextFieldGBConstraints(0, 0));
    	WidgetFactory wf = Factory.getInstance(WidgetFactory.class);
    	JButton button = wf.createIconButton("gen.btnNew", new NewPartyTypeAction(), 21);
    	add(button);
	}

	private void showNewPartyTypeDialog() {
		NewPartyView view = new NewPartyView();
		ViewDialog dialog = new ViewDialog(this, view);
		dialog.showDialog();

		String newParty = view.getEnteredType();
		if (newParty != null) {
			types.addItem(newParty, null);
			types.setSelectedItem(newParty, null);
		}
	}

	private final class NewPartyTypeAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			showNewPartyTypeDialog();
		}
	}

	private class NewPartyView extends OkCancelView {

		private StringModel newPartyTypeModel = new StringModel();

		private String enteredType;

		@Override
		public String getTitle() {
			return null;
		}

		@Override
		public void onClose() {
		}

		@Override
		public void onInit() {
			addComponents();
		}

		@Override
		protected JComponent createCenterComponent() {
			InputFieldsColumn ifc = new InputFieldsColumn();
			addCloseable(ifc);
			ifc.addField("PartyTypeBean.newPartyType", newPartyTypeModel, 20);
			return ifc;
		}

		@Override
		protected void onOk() {
			String newParty = newPartyTypeModel.getString();
			if (!StringUtil.isNullOrEmpty(newParty)) {
				enteredType = newParty;
				requestClose();
			}
		}

		public String getEnteredType() {
			return enteredType;
		}
	}
}
