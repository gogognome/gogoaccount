/*
 * $Id: PartySelectorListener.java,v 1.1 2008-11-01 13:26:02 sanderk Exp $
 *
 * Copyright (C) 2005 Sander Kooijmans
 *
 */

package cf.ui.components;

import cf.engine.Party;

/**
 * Listener for changes in the selected party.
 */
public interface PartySelectorListener {

    /** 
     * This method is called when the selected party has been changed.
     * 
     * @param newParty the new party
     */
    public void onSelectedPartyChanged(Party newParty);
}
