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

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.LinkedList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.lib.swing.ActionWrapper;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.util.Factory;
import cf.engine.Party;
import cf.ui.dialogs.PartySelectionDialog;

/**
 * This class implements a widget for selecting a <code>Party</code>.
 *
 * @author Sander Kooijmans
 */
public class PartySelector extends JPanel {

    /** Contains a description of the selected party. */
    private JTextField tfDescription;

    /** The button to select a party from a dialog. */
    private JButton btSelect;

    /** The button to clear the selected party. */
    private JButton btClear;

    /** The party that is selected in this selector. */
    private Party selectedParty;

    /** The listeners for this component. */
    private List<PartySelectorListener> listeners = new LinkedList<PartySelectorListener>();

    /**
     * Constructor.
     */
    public PartySelector() {
        WidgetFactory wf = Factory.getInstance(WidgetFactory.class);
        setLayout(new GridBagLayout());

        tfDescription = new JTextField();
        tfDescription.setEditable(false);
        tfDescription.setFocusable(false);

        Dimension dimension = new Dimension(21, 21);
        ActionWrapper actionWrapper = wf.createAction("gen.btSelectParty");
        btSelect = new JButton(actionWrapper);
        btSelect.setText(null);
        btSelect.setPreferredSize(dimension);
        actionWrapper.setAction(new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent event) {
                selectParty();
            }
        });

        actionWrapper = wf.createAction("gen.btClearParty");
        btClear = new JButton(actionWrapper);
        btClear.setText(null);
        btClear.setPreferredSize(dimension);
        actionWrapper.setAction(new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent event) {
                setSelectedParty(null);
            }
        });

        add(tfDescription, SwingUtils.createTextFieldGBConstraints(0, 0));
        add(btSelect, SwingUtils.createGBConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        0, 5, 0, 0));
        add(btClear, SwingUtils.createGBConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                0, 2, 0, 0));
    }

    /**
     * Gets the selected party.
     * @return the selected party or <code>null</code> if no party is selected.
     */
    public Party getSelectedParty() {
        return selectedParty;
    }

    /**
     * Selecs a party.
     * @param party the party
     */
    public void setSelectedParty(Party party) {
        selectedParty = party;
        if (selectedParty != null) {
            tfDescription.setText(party.getId() + " " + party.getName());
        } else {
            tfDescription.setText(null);
        }
        synchronized(listeners) {
            for (PartySelectorListener listener : listeners) {
                listener.onSelectedPartyChanged(party);
            }
        }
    }

    /**
     * Lets the user select a party in a dialog.
     */
    public void selectParty() {
        Container parent = getParent();
        while(!(parent instanceof Frame)) {
            parent = parent.getParent();
        }
        PartySelectionDialog dialog = new PartySelectionDialog((Frame)parent,
                PartySelectionDialog.SELECTION_MODE);
        dialog.showDialog();
        if (dialog.getSelectedParty() != null) {
            setSelectedParty(dialog.getSelectedParty());
        }
    }

    public void addListener(PartySelectorListener listener) {
        synchronized(listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(PartySelectorListener listener) {
        synchronized(listeners) {
            listeners.remove(listener);
        }
    }
}
