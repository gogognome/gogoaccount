/*
 * $Id: BalanceAndOperationResultView.java,v 1.1 2008-11-10 20:12:11 sanderk Exp $
 *
 * Copyright (C) 2005 Sander Kooijmans
 *
 */

package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import cf.engine.Database;
import cf.ui.components.BalanceComponent;
import cf.ui.components.OperationalResultComponent;
import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.text.TextResource;

/**
 * This class implements a view the shows a balance and operation result. 
 *
 * @author Sander Kooijmans
 */
public class BalanceAndOperationResultView extends View {

    private final static Color BACKGROUND_COLOR = new Color(255, 255, 209);
    
    private DateSelectionBean dateSelectionBean;

    private Database database;
    
    private DateModel dateModel;
    
    public BalanceAndOperationResultView(Database database) {
        this.database = database;
    }
    
    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#getTitle()
     */
    public String getTitle() {
        return TextResource.getInstance().getString("balanceAndOperationalResultView.title");
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
        datePanel.add(new JLabel(tr.getString("balanceAndOperationalResultView.selectDate")),
                SwingUtils.createLabelGBConstraints(0, 0));
        
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
                
        dateSelectionBean = new DateSelectionBean(dateModel);
        datePanel.add(dateSelectionBean,
                SwingUtils.createLabelGBConstraints(1, 0));
        
        datePanel.add(new JLabel(), 
                SwingUtils.createTextFieldGBConstraints(2, 0));
        add(datePanel, BorderLayout.NORTH);
        
        JPanel balanceAndOperationResultPanel = new JPanel(new GridBagLayout());
        balanceAndOperationResultPanel.setBackground(BACKGROUND_COLOR);
        BalanceComponent balanceComponent = new BalanceComponent(database, dateModel);
        balanceComponent.setBackground(BACKGROUND_COLOR);
        balanceAndOperationResultPanel.add(balanceComponent, 
            SwingUtils.createPanelGBConstraints(0, 0));
        
        OperationalResultComponent operationalResultComponent = 
            new OperationalResultComponent(database, dateModel);
        operationalResultComponent.setBackground(BACKGROUND_COLOR);
        balanceAndOperationResultPanel.add(operationalResultComponent,
            SwingUtils.createPanelGBConstraints(0, 1));

        add(balanceAndOperationResultPanel, BorderLayout.CENTER);
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onClose()
     */
    public void onClose() {
        dateSelectionBean = null;
        database = null;
    }

}
