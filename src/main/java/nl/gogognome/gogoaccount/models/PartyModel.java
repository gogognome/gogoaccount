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
