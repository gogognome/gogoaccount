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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import nl.gogognome.gogoaccount.businessobjects.Party;
import nl.gogognome.gogoaccount.database.Database;
import nl.gogognome.gogoaccount.gui.components.PartyTypeBean;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.*;
import nl.gogognome.lib.swing.views.OkCancelView;

/**
 * This class implements a view to edit a party. Either an existing party or
 * a new party can be edited.
 *
 * @author Sander Kooijmans
 */
public class EditPartyView extends OkCancelView {

	private static final long serialVersionUID = 1L;

    private final Database database;

    private final StringModel idModel = new StringModel();
    private final StringModel nameModel = new StringModel();
    private final StringModel addressModel = new StringModel();
    private final StringModel zipCodeModel = new StringModel();
    private final StringModel cityModel = new StringModel();
    private final ListModel<String> typeListModel = new ListModel<String>();
    private final DateModel birthDateModel = new DateModel();
    private JTextField lbIdRemark = new JTextField(); // text field 'misused' as text label
    private final JTextArea taRemarks = new JTextArea(5, 30);

    private final Party initialParty;
    private Party resultParty;

    private ModelChangeListener idUpdateListener;

    /**
     * Constructor.
     * @param party the party used to initialize the view
     */
    protected EditPartyView(Database database, Party party) {
        super();
        this.database = database;
        this.initialParty = party;
    }

    @Override
	public void onInit() {
    	initModels();
        addComponents();
        addListeners();
        updateIdMessage();
    }

    private void initModels() {
    	List<String> items = new ArrayList<String>();
    	items.add("");
    	items.addAll(Arrays.asList(database.getPartyTypes()));
    	typeListModel.setItems(items);

    	if (initialParty != null) {
            idModel.setString(initialParty.getId());
            idModel.setEnabled(false, null);
            nameModel.setString(initialParty.getName());
            addressModel.setString(initialParty.getAddress());
            zipCodeModel.setString(initialParty.getZipCode());
            cityModel.setString(initialParty.getCity());
            typeListModel.setSelectedItem(initialParty.getType(), null);
            taRemarks.setText(initialParty.getRemarks());
            birthDateModel.setDate(initialParty.getBirthDate(), null);
        } else {
            idModel.setString(suggestNewId());
        }
	}

    @Override
    protected JComponent createCenterComponent() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);

        ifc.addField("editPartyView.id", idModel);
        lbIdRemark = new JTextField(20);
        lbIdRemark.setEditable(false);
        lbIdRemark.setEnabled(false);
        lbIdRemark.setBorder(null);
        ifc.add(lbIdRemark, SwingUtils.createTextFieldGBConstraints(2, 0));

        ifc.addField("editPartyView.name", nameModel);
        ifc.addField("editPartyView.address", addressModel);
        ifc.addField("editPartyView.zipCode", zipCodeModel);
        ifc.addField("editPartyView.city", cityModel);
        ifc.addField("editPartyView.birthDate", birthDateModel);
        PartyTypeBean typesBean = new PartyTypeBean(typeListModel);
        ifc.addVariableSizeField("editPartyView.type", typesBean);

        ifc.addVariableSizeField("editPartyView.remarks", taRemarks);

        return ifc;
    }

    private void addListeners() {
    	idUpdateListener = new IdChangeListener();
        idModel.addModelChangeListener(idUpdateListener);
    }

    @Override
    protected void onOk() {
        resultParty = new Party(idModel.getString(), nameModel.getString(),
                addressModel.getString(), zipCodeModel.getString(), cityModel.getString(),
                birthDateModel.getDate(), typeListModel.getSelectedItem(), taRemarks.getText());
        requestClose();
    }

    /**
     * Gets the party that was entered by the user.
     * @return the party or <code>null</code> if the user canceled this dialog
     */
    public Party getEnteredParty() {
        return resultParty;
    }

    /**
     * Gets the title of this view.
     * @return the title of this view
     */
    @Override
	public String getTitle() {
        return textResource.getString(initialParty != null ? "editPartyView.titleEdit" : "editPartyView.titleAdd");
    }

    private void updateIdMessage() {
        String remark = "";
        String id = idModel.getString();
        if (initialParty == null && database.getParty(id) != null ) {
            remark = textResource.getString("editPartyView.idExistsAlready");
        } else if (id.length() == 0) {
            remark = textResource.getString("editPartyView.idIsEmpty");
        }
        lbIdRemark.setText(remark);
    }

    private String suggestNewId() {
        Party[] parties = database.getParties();

        String suggestion = null;
        for (Party party : parties) {
            if (suggestion == null) {
                suggestion = party.getId();
            } else {
                if (suggestion.compareTo(party.getId()) < 0) {
                    suggestion = party.getId();
                }
            }
        }
        // If suggestion != null, then it contains the largest ID according to the
        // lexicographically order. Increase the ID to get a unique ID.
        if (suggestion != null) {
            StringBuilder sb = new StringBuilder(suggestion);
            int index = sb.length() - 1;
            boolean done = false;
            while (!done && index >= 0) {
                char c = (char) (sb.charAt(index) + 1);
                if (c == (char)('9' + 1)) {
                    c = '0';
                } else if (c == (char)('z' + 1)) {
                    c = 'a';
                } else if (c == (char)('Z' + 1)) {
                    c = 'A';
                } else {
                    done = true;
                }
                sb.setCharAt(index, c);
                index--;
            }
            if (!done) {
                sb.insert(0, '1');
            }
            suggestion = sb.toString();
        } else {
            suggestion = "001";
        }
        return suggestion;
    }

	@Override
	public void onClose() {
		removeListeners();
    }

	private void removeListeners() {
		idModel.removeModelChangeListener(idUpdateListener);
	}

	private final class IdChangeListener implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
			updateIdMessage();
		}
	}
}
