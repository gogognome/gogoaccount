package nl.gogognome.gogoaccount.models;

import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;

public class PartyModel extends AbstractModel {

    private Party party;

    public Party getParty() {
        return party;
    }

    /**
     * Sets the party of this model.
     * @param newParty the new value of the party
     */
    public void setParty(Party newParty) {
    	setParty(newParty, null);
    }

    /**
     * Sets the party of this model.
     * @param newParty the new value of the party
     * @param source the model change listener that sets the party. It will not
     *         get notified. It may be <code>null</code>.
     */
    public void setParty(Party newParty, ModelChangeListener source) {
        if (party != newParty) {
            party = newParty;
            notifyListeners(source);
        }
    }
}
