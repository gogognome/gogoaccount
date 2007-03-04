/*
 * $Id: ViewBalanceDialog.java,v 1.4 2007-02-10 16:28:46 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.Frame;
import java.util.Date;

import javax.swing.JScrollPane;

import nl.gogognome.swing.DialogWithButtons;

import cf.ui.components.BalanceComponent;
import cf.engine.Database;

/**
 * This class implements the View Balance dialog. 
 *
 * @author Sander Kooijmans
 */
public class ViewBalanceDialog extends DialogWithButtons 
{
	/** 
	 * Creates a "View Balance" dialog.
	 * @param frame the frame that owns this dialog. 
	 * @param date the date of the balance.
	 */
	public ViewBalanceDialog( Frame frame, Date date ) 
	{
		super(frame, "vb.viewBalance", BT_OK);
		
		BalanceComponent balanceComponent = new BalanceComponent( 
		        Database.getInstance().getBalance(date) );
		
		JScrollPane scrollPane = new JScrollPane(balanceComponent);
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
