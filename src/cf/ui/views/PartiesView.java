/*
 * $Id: PartiesView.java,v 1.1 2007-04-17 18:27:58 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.views;

import java.util.Date;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.engine.Party;

/**
 * This class implements a view for adding, removing and editing parties.
 *
 * @author Sander Kooijmans
 */
public class PartiesView extends View {

    private PartiesTableModel partiesTableModel;
    
    /** The database whose parties are to be shown and changed. */
    private Database database;
    
    /** The date model that determines the date for the balance of the parties. */  
    private DateModel dateModel;
    
    public PartiesView(Database database) {
        this.database = database;
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
    }
    
    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#getTitle()
     */
    public String getTitle() {
        return TextResource.getInstance().getString("partiesView.title");
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onInit()
     */
    public void onInit() {
        partiesTableModel = new PartiesTableModel(database);
        JTable table = new JTable(partiesTableModel);
        JScrollPane scrollPane = new JScrollPane(table);
        add(scrollPane);
    }

    /* (non-Javadoc)
     * @see nl.gogognome.framework.View#onClose()
     */
    public void onClose() {
        // TODO Auto-generated method stub
        
    }

    /** The table model that shows information about the parties. */
    private static class PartiesTableModel extends AbstractTableModel implements DatabaseListener {

        private Database database;
        
        /** The parties to be shown. */
        private Party[] parties;
        
        public PartiesTableModel(Database database) {
            this.database = database;
            parties = database.getParties();
            database.addListener(this);
        }
        
        public String getColumnName(int columnIndex) {
            String id; 
            switch(columnIndex) {
            case 0: id = "gen.id"; break;
            case 1: id = "gen.name"; break;
            case 2: id = "gen.address"; break;
            case 3: id = "gen.zipCode"; break;
            case 4: id = "gen.city"; break;
            default: 
                id = null;
            }
            return TextResource.getInstance().getString(id);
        }
        
        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        public int getColumnCount() {
            return 5;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public int getRowCount() {
            return parties.length;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        public Object getValueAt(int row, int col) {
            switch(col) {
            case 0: return parties[row].getId();
            case 1: return parties[row].getName();
            case 2: return parties[row].getAddress();
            case 3: return parties[row].getZipCode();
            case 4: return parties[row].getCity();
            default: return null;
            }
        }

        /* (non-Javadoc)
         * @see cf.engine.DatabaseListener#databaseChanged(cf.engine.Database)
         */
        public void databaseChanged(Database db) {
            parties = database.getParties();
            fireTableDataChanged();
        }
        
    }
}
