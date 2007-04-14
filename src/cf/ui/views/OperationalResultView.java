/*
 * $Id: OperationalResultView.java,v 1.1 2007-04-14 12:47:18 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.views;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.ui.components.OperationalResultComponent;

/**
 * This class implements a view the shows an operational result. 
 *
 * @author Sander Kooijmans
 */
public class OperationalResultView extends View {

    private DateSelectionBean dateSelectionBean;

    private Database database;
    
    private DateModel dateModel;
    
    public OperationalResultView(Database database) {
        this.database = database;
    }
    
    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#getTitle()
     */
    public String getTitle() {
        return TextResource.getInstance().getString("operationalResultView.title");
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onInit()
     */
    public void onInit() {
        setLayout(new BorderLayout());
        
        TextResource tr = TextResource.getInstance();
        
        JPanel datePanel = new JPanel(new GridBagLayout());
        datePanel.add(new JLabel(tr.getString("operationalResultView.selectDate")),
                SwingUtils.createLabelGBConstraints(0, 0));
        
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
                
        dateSelectionBean = new DateSelectionBean(dateModel);
        datePanel.add(dateSelectionBean,
                SwingUtils.createLabelGBConstraints(1, 0));
        
        datePanel.add(new JLabel(), 
                SwingUtils.createTextFieldGBConstraints(2, 0));
        add(datePanel, BorderLayout.NORTH);
        
		OperationalResultComponent operationalResultComponent = 
		    new OperationalResultComponent(database, dateModel);
		add(operationalResultComponent, BorderLayout.CENTER);
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onClose()
     */
    public void onClose() {
        dateSelectionBean = null;
        database = null;
    }
}
