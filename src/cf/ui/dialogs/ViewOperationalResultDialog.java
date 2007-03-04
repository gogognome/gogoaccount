/*
 * $Id: ViewOperationalResultDialog.java,v 1.4 2007-02-10 16:28:46 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.Frame;
import java.util.Date;

import javax.swing.JScrollPane;

import nl.gogognome.swing.DialogWithButtons;

import cf.engine.Database;
import cf.ui.components.OperationalResultComponent;

/**
 * This class implements the View operational results dialog.
 *  
 * @author Sander Kooijmans
 */
public class ViewOperationalResultDialog extends DialogWithButtons
{
	/** 
	 * Creates a "View Operational Results" dialog.
	 * @param frame the frame that owns this dialog. 
	 * @param date the date of the balance.
	 */
	public ViewOperationalResultDialog( Frame frame, Date date ) 
	{
		super(frame, "vb.viewOperationalResult", BT_OK);
		
		OperationalResultComponent operationalResultComponent = 
		    new OperationalResultComponent( Database.getInstance().getOperationalResult(date) );
		
		JScrollPane scrollPane = new JScrollPane(operationalResultComponent);
		componentInitialized(scrollPane);
		setResizable(true);
	}

	/**
	 * Handles the cancel event. This method should not be called, since the cancel
	 * button is disabled.
	 */	
	protected void handleCancel() 
	{
		// release resources
		handleButton(0);
	}

	/**
	 * Handles the OK event. This method hides the dialog and frees resources.
	 * @param index the index of the button. 
	 */
	protected void handleButton( int index ) 
	{
		// release resources
		hideDialog();
	}

}
