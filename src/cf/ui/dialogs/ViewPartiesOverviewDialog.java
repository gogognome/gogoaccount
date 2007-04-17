/*
 * $Id: ViewPartiesOverviewDialog.java,v 1.5 2007-04-17 18:29:22 sanderk Exp $
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

import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.DialogWithButtons;
import nl.gogognome.text.TextResource;

import cf.engine.Database;
import cf.ui.components.PartiesOverviewTableModel;

/**
 * This class implements a dialog which shows the overview of all parties 
 * at a specified date.
 *
 * @author Sander Kooijmans
 */
public class ViewPartiesOverviewDialog extends DialogWithButtons 
{

    /**
     * Constructor.
     * @param parent the parent frame of this dialog
     */
    public ViewPartiesOverviewDialog(Frame parent, Date date) 
    {
        super(parent, "vpos.title", DialogWithButtons.BT_OK);
        
        JPanel panel = new JPanel(new BorderLayout());
        
        TextResource tr = TextResource.getInstance();
        String s = tr.getString("vpos.overviewOfPartiesAt", 
                new Object[] {tr.formatDate("gen.dateFormat", date)});
        panel.add(new JLabel(s), BorderLayout.NORTH);

        // TODO: Move datemodel and database to parameters of this constructor.
        DateModel dateModel = new DateModel();
        dateModel.setDate(date, null);
        JTable table = new JTable(new PartiesOverviewTableModel(Database.getInstance(), dateModel));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        
        componentInitialized(panel);
		setResizable(true);
    }

    
}
