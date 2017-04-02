package nl.gogognome.gogoaccount.gui.beans;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.gogoaccount.gui.views.PartiesView;
import nl.gogognome.gogoaccount.models.PartyModel;
import nl.gogognome.lib.gui.Closeable;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.util.Factory;

/**
 * This class implements a widget for selecting a Party.
 */
public class PartyBean extends JPanel implements Closeable {

	private final Document document;
    private final PartyModel model;
    private final ViewFactory viewFacatory;
    private final HandleException handleException;

    private JTextField tfDescription;
    private JButton btSelect;
    private JButton btClear;

    private ModelChangeListener listener;

    public PartyBean(Document document, PartyModel model, ViewFactory viewFacatory, HandleException handleException) {
        this.document = document;
        this.model = model;
        this.viewFacatory = viewFacatory;
        this.handleException = handleException;
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
        handleException.of(() -> {
            PartiesView partiesView = (PartiesView) viewFacatory.createView(PartiesView.class);
            partiesView.setSelectionEnabled(true);
            ViewDialog dialog = new ViewDialog(parent, partiesView);
            dialog.showDialog();
            Party[] parties = partiesView.getSelectedParties();
            if (parties != null && parties.length == 1) {
                model.setParty(parties[0]);
            }
        });

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
