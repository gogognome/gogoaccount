/*
 * $Id: BalanceView.java,v 1.5 2007-07-29 12:33:40 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.ui.components.BalanceComponent;

/**
 * This class implements a view the shows a balance. 
 *
 * @author Sander Kooijmans
 */
public class BalanceView extends View {

    private final static Color BACKGROUND_COLOR = new Color(255, 255, 209);
    
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
        setBackground(BACKGROUND_COLOR);
        
        TextResource tr = TextResource.getInstance();
        
        JPanel datePanel = new JPanel(new GridBagLayout());
        datePanel.setOpaque(false);
        datePanel.setBorder(new EmptyBorder(5, 10, 5, 5));
        datePanel.add(new JLabel(tr.getString("balanceView.selectDate")),
                SwingUtils.createLabelGBConstraints(0, 0));
        
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
                
        dateSelectionBean = new DateSelectionBean(dateModel);
        datePanel.add(dateSelectionBean,
                SwingUtils.createLabelGBConstraints(1, 0));
        
        datePanel.add(new JLabel(), 
                SwingUtils.createTextFieldGBConstraints(2, 0));
        add(datePanel, BorderLayout.NORTH);
        
		BalanceComponent balanceComponent = new BalanceComponent(database, dateModel);
        balanceComponent.setBackground(BACKGROUND_COLOR);
        
		add(balanceComponent, BorderLayout.CENTER);
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onClose()
     */
    public void onClose() {
        dateSelectionBean = null;
        database = null;
    }
}
