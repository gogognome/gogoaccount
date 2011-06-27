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

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import nl.gogognome.lib.gui.beans.BeanFactory;
import nl.gogognome.lib.gui.beans.DateSelectionBean;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Database;
import cf.engine.Party;

/**
 * This class implements a view to edit a party. Either an existing party or
 * a new party can be edited.
 *
 * @author Sander Kooijmans
 */
public class EditPartyView extends View {

    private JTextField tfId;
    private JTextField tfName;
    private JTextField tfAddress;
    private JTextField tfZipCode;
    private JTextField tfCity;
    private JTextField tfType;
    private JTextArea taRemarks;
    private DateModel dateModel;
    private JTextField lbIdRemark;

    /** The database to which the party has to be added. */
    private Database database;

    /**
     * The party that was entered by the user. If the user cancels this dialog,
     * then <code>resultParty</code> is <code>null</code>.
     */
    private Party resultParty;

    /** The party used to initialize the view. */
    private Party initialParty;

    /** The ok button. */
    private JButton okButton;

    /**
     * Constructor.
     * @param party the party used to initialize the view
     */
    protected EditPartyView(Database database, Party party) {
        super();
        this.database = database;
        this.initialParty = party;
    }

    /** This method is called when the view is to be shown. */
    @Override
	public void onInit() {
        add(createPanel());
        setDefaultButton(okButton);
    }

    /** This method is called when the view is to be closed. */
    @Override
	public void onClose() {

    }

    /**
     * Gets the panel for editing a party.
     * @return the panel
     */
    private JPanel createPanel() {
        // Create the panel with labels and text fields
        JPanel textfieldPanel = new JPanel(new GridBagLayout());

        WidgetFactory wf = WidgetFactory.getInstance();

        int row = 0;
        tfId = wf.createTextField(10);
        JLabel label = wf.createLabel("editPartyView.id", tfId);
        textfieldPanel.add(label,
                SwingUtils.createLabelGBConstraints(0, row));
        textfieldPanel.add(tfId, SwingUtils.createTextFieldGBConstraints(1, row));
        lbIdRemark = new JTextField(20);
        lbIdRemark.setEditable(false);
        lbIdRemark.setEnabled(false);
        lbIdRemark.setBorder(null);
        textfieldPanel.add(lbIdRemark, SwingUtils.createTextFieldGBConstraints(2, row));
        row++;

        tfName = wf.createTextField(30);
        label = wf.createLabel("editPartyView.name", tfName);
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        textfieldPanel.add(tfName, SwingUtils.createGBConstraints(1, row, 2, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 3, 0));
        row++;

        tfAddress = wf.createTextField(30);
        label = wf.createLabel("editPartyView.address", tfAddress);
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        textfieldPanel.add(tfAddress, SwingUtils.createGBConstraints(1, row, 2, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 3, 0));
        row++;

        tfZipCode = wf.createTextField(30);
        label = wf.createLabel("editPartyView.zipCode", tfZipCode);
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        textfieldPanel.add(tfZipCode, SwingUtils.createGBConstraints(1, row, 2, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 3, 0));
        row++;

        tfCity = wf.createTextField(30);
        label = wf.createLabel("editPartyView.city", tfCity);
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        textfieldPanel.add(tfCity, SwingUtils.createGBConstraints(1, row, 2, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 3, 0));
        row++;

        tfType = wf.createTextField(30);
        label = wf.createLabel("editPartyView.type", tfType);
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        textfieldPanel.add(tfType, SwingUtils.createGBConstraints(1, row, 2, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 3, 0));
        row++;

        taRemarks = new JTextArea(5, 30);
        label = wf.createLabel("editPartyView.remarks", taRemarks);
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        JScrollPane remarksPane = new JScrollPane(taRemarks);
        textfieldPanel.add(remarksPane, SwingUtils.createGBConstraints(1, row, 2, 1, 1.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 3, 0));
        row++;

        dateModel = new DateModel();
        DateSelectionBean dsbBirthDate = BeanFactory.getInstance().createDateSelectionBean(dateModel);
        label = wf.createLabel("editPartyView.birthDate", dsbBirthDate);
        textfieldPanel.add(label, SwingUtils.createLabelGBConstraints(0, row));
        textfieldPanel.add(dsbBirthDate, SwingUtils.createLabelGBConstraints(1, row));

        if (initialParty != null) {
            tfId.setText(initialParty.getId());
            tfId.setEditable(false);
            tfId.setEnabled (false);
            tfName.setText(initialParty.getName());
            tfAddress.setText(initialParty.getAddress());
            tfZipCode.setText(initialParty.getZipCode());
            tfCity.setText(initialParty.getCity());
            tfType.setText(initialParty.getType());
            taRemarks.setText(initialParty.getRemarks());
            dateModel.setDate(initialParty.getBirthDate(), null);
        } else {
            tfId.setText(suggestNewId());
            tfId.getDocument().addDocumentListener(new DocumentListener() {
                @Override
				public void changedUpdate(DocumentEvent e) {
                    onIdChange();
                }

                @Override
				public void insertUpdate(DocumentEvent e) {
                    onIdChange();
                }

                @Override
				public void removeUpdate(DocumentEvent e) {
                    onIdChange();
                }
            });
            onIdChange();
        }

        // Create panel with buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        okButton = wf.createButton("gen.ok", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent event) {
                resultParty = new Party(tfId.getText(), tfName.getText(),
                    tfAddress.getText(), tfZipCode.getText(), tfCity.getText(), dateModel.getDate(),
                    tfType.getText(), taRemarks.getText());
                closeAction.actionPerformed(event);
            }
        });
        buttonPanel.add(okButton);
        buttonPanel.add(wf.createButton("gen.cancel", closeAction));

        // Create overall panel
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(textfieldPanel, SwingUtils.createPanelGBConstraints(0, 0));
        panel.add(buttonPanel, SwingUtils.createPanelGBConstraints(0, 1));

        return panel;
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
        return TextResource.getInstance().getString(initialParty != null ? "editPartyView.titleEdit" : "editPartyView.titleAdd");
    }

    /**
     * This method is called when the user content of tfId has changed.
     * If the ID is not valid, then this is shown to the user.
     */
    private void onIdChange() {
        String remark = "";
        String id = tfId.getText();
        if (initialParty == null && database.getParty(id) != null ) {
            remark = TextResource.getInstance().getString("editPartyView.idExistsAlready");
        } else if (id.length() == 0) {
            remark = TextResource.getInstance().getString("editPartyView.idIsEmpty");
        }
        lbIdRemark.setText(remark);
    }

    /**
     * Generates an ID that does not exist yet.
     * @return the suggested ID
     */
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
}
