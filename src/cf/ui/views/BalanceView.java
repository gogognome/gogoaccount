/*
 * $Id: BalanceView.java,v 1.1 2007-04-07 15:27:25 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.ui.components.BalanceComponent;

/**
 * This class implements a view the shows a balance. 
 *
 * @author Sander Kooijmans
 */
public class BalanceView extends View {

    private DateSelectionBean dateSelectionBean;

    private Database database;
    
    private DateModel dateModel;
    
    public BalanceView(Database database) {
        this.database = database;
    }
    
    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#getTitle()
     */
    public String getTitle() {
        return TextResource.getInstance().getString("balanceView.title");
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onInit()
     */
    public void onInit() {
        setLayout(new BorderLayout());
        
        TextResource tr = TextResource.getInstance();
        
        JPanel datePanel = new JPanel(new FlowLayout());
        datePanel.add(new JLabel(tr.getString("balanceView.selectDate")));
        
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
                
        dateSelectionBean = new DateSelectionBean(dateModel);
        datePanel.add(dateSelectionBean);
        
        add(datePanel, BorderLayout.NORTH);
        
		BalanceComponent balanceComponent = new BalanceComponent(database, dateModel);
		
		JScrollPane scrollPane = new JScrollPane(balanceComponent);
		scrollPane.setSize(500, 500);
		add(scrollPane, BorderLayout.CENTER);
		
		setPreferredSize(new Dimension(900, 500));
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onClose()
     */
    public void onClose() {
        dateSelectionBean = null;
        database = null;
    }
}
