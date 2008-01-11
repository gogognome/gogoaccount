/*
 * $Id: ViewAccountOverviewDialog.java,v 1.5 2008-01-11 18:56:55 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import nl.gogognome.swing.DialogWithButtons;
import nl.gogognome.text.TextResource;

import cf.engine.Account;
import cf.engine.Database;
import cf.ui.components.AccountOverviewTableModel;

/**
 * This class implements a dialog that shows the overview of an account
 * at a specific date. 
 *
 * @author Sander Kooijmans
 */
public class ViewAccountOverviewDialog extends DialogWithButtons 
{

    /**
	 * Creates a "Account overview" dialog.
	 * @param frame the frame that owns this dialog.
	 * @param account the account to be shown.
	 * @param date the date.
     */
    public ViewAccountOverviewDialog( Frame frame, Database database, Account account, Date date ) 
    {
		super(frame, "vao.title", BT_OK);
		
		AccountOverviewTableModel model = new AccountOverviewTableModel(database, account, date); 
		JTable table = new JTable(model);
		
		TableColumnModel columnModel = table.getColumnModel();
		
		// Set right-aligned renderers for column 3 and 4.
		TableCellRenderer rightAlignedRenderer = new DefaultTableCellRenderer() {
		    public void setValue(Object value) {
		        super.setValue(value);
		        setHorizontalAlignment(SwingConstants.RIGHT);
		    }
		};
		columnModel.getColumn(3).setCellRenderer(rightAlignedRenderer);
		columnModel.getColumn(4).setCellRenderer(rightAlignedRenderer);
		
		// Set column widths
		columnModel.getColumn(0).setPreferredWidth(150);
		columnModel.getColumn(1).setPreferredWidth(150);
		columnModel.getColumn(2).setPreferredWidth(300);
		columnModel.getColumn(3).setPreferredWidth(150);
		columnModel.getColumn(4).setPreferredWidth(150);
		
		// Create panel with date and name of account.
		JLabel label = new JLabel();
		TextResource tr = TextResource.getInstance();
		label.setText(tr.getString("vao.accountAtDate",
		        new String[] { account.getId() + " - " + account.getName(), 
		        tr.formatDate("gen.dateFormat", date) }));

		// Create panel with label and table.
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(label, BorderLayout.NORTH);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		
		componentInitialized(panel);
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
