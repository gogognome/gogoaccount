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
package nl.gogognome.gogoaccount.gui.beans;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.gogoaccount.models.PartyModel;
import nl.gogognome.lib.gui.Closeable;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.util.Factory;
import cf.engine.Database;
import cf.engine.Party;
import cf.ui.views.PartiesView;

/**
 * This class implements a widget for selecting a Party.
 *
 * @author Sander Kooijmans
 */
public class PartyBean extends JPanel implements Closeable {

	private Database database;
    private JTextField tfDescription;

    private JButton btSelect;
    private JButton btClear;

    private PartyModel model;
    private ModelChangeListener listener;

    /**
     * Constructor.
     */
    public PartyBean(Database database, PartyModel model) {
        this.database = database;
        this.model = model;
        WidgetFactory wf = Factory.getInstance(WidgetFactory.class);
        setLayout(new GridBagLayout());

        tfDescription = new JTextField(20);
        tfDescription.setEditable(false);
        tfDescription.setFocusable(false);

		btSelect = wf.createIconButton("gen.btSelectParty", new SelectAction(), 21);
		btClear = wf.createIconButton("gen.btClearParty", new ClearAction(), 21);

        add(tfDescription, SwingUtils.createTextFieldGBConstraints(0, 0));
        add(btSelect, SwingUtils.createGBConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.WEST, GridBagConstraints.NONE,
                        0, 5, 0, 0));
        add(btClear, SwingUtils.createGBConstraints(2, 0, 1, 1, 0.0, 0.0,
                GridBagConstraints.WEST, GridBagConstraints.NONE,
                0, 2, 0, 0));

        updateParty();

        listener = new ModelChangeListenerImpl();
        model.addModelChangeListener(listener);
    }

    @Override
    public void close() {
    	model.removeModelChangeListener(listener);
    }

    /**
     * Selects a party
     * @param party the party
     */
    private void updateParty() {
    	Party party = model.getParty();
        if (party != null) {
            tfDescription.setText(party.getId() + " - " + party.getName());
        } else {
            tfDescription.setText(null);
        }
    }

    /**
     * Lets the user select a party in a dialog.
     */
    public void selectParty() {
        Container parent = SwingUtils.getTopLevelContainer(this);

        PartiesView partiesView = new PartiesView(database);
        partiesView.setSelectioEnabled(true);
        ViewDialog dialog = new ViewDialog(parent, partiesView);
        dialog.showDialog();
        Party[] parties = partiesView.getSelectedParties();
        if (parties != null && parties.length == 1) {
        	model.setParty(parties[0]);
        }

    }

    private final class SelectAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    selectParty();
		}
	}

    private final class ClearAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    model.setParty(null);
		}
	}

    private class ModelChangeListenerImpl implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
			updateParty();
		}
    }
}
