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

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;

import nl.gogognome.lib.gui.beans.ComboBoxBean;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;
import nl.gogognome.lib.swing.views.ViewPopup;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.lib.util.StringUtil;

/**
 * Bean for selecting the type of a party. It also contains a button that allows the user
 * to add a new party type.
 *
 * @author Sander Kooijmans
 */
public class PartyTypeBean extends ComboBoxBean<String> {

	private static final long serialVersionUID = 1L;

	private ListModel<String> types;

	public PartyTypeBean(ListModel<String> types) {
		super(types);
		this.types = types;
	}

	@Override
	public void initBean() {
		super.initBean();

    	WidgetFactory wf = Factory.getInstance(WidgetFactory.class);
    	JButton button = wf.createIconButton("gen.btnCalendar", new NewPartyTypeAction(), 21);
    	add(button);
	}

	private void showNewPartyTypePopup() {
		NewPartyView view = new NewPartyView();
		ViewPopup viewPopup = new ViewPopup(view);
		viewPopup.show(this, SwingUtils.getCoordinatesRelativeToTopLevelContainer(this));
	}

	private final class NewPartyTypeAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
			showNewPartyTypePopup();
		}
	}

	private class NewPartyView extends OkCancelView {

		private StringModel newPartyTypeModel;

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
			ifc.addField("PartyTypeBean.newPartyType", newPartyTypeModel);
			return ifc;
		}

		@Override
		protected void onOk() {
			String newParty = newPartyTypeModel.getString();
			if (!StringUtil.isNullOrEmpty(newParty)) {
				types.addItem(newParty, null);
				requestClose();
			}
		}

	}
}
