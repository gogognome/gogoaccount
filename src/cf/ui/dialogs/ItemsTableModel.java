/*
 * $Id: ItemsTableModel.java,v 1.8 2008-01-10 19:18:08 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.Invoice;
import cf.engine.JournalItem;

class ItemsTableModel implements TableModel
{
    /** Contains the items shown in the table. */
    private ArrayList<JournalItem> items = new ArrayList<JournalItem>();
    
    /** Contains the <code>TableModelListener</code>s of this <code>TableModel</code>. */
    private ArrayList<TableModelListener> itemTableModelListeners = new ArrayList<TableModelListener>();
    
    public void setJournalItems(JournalItem[] itemsArray) {
        items.clear();
        for (int i=0; i<itemsArray.length; i++) {
            items.add(itemsArray[i]);
        }
        notifyListeners(new TableModelEvent(this));
    }

    /**
     * Notifies the listeners of a change in the table.
     * @param event describes the change.
     */
    private void notifyListeners(TableModelEvent event)
    {
        for (Iterator iter = itemTableModelListeners.iterator(); iter.hasNext();)
        {
            TableModelListener listener = (TableModelListener)iter.next();
            listener.tableChanged(event);
        }
    }
    
    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() 
    {
        return 4;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() 
    {
        return items.size();
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#isCellEditable(int, int)
     */
    public boolean isCellEditable(int row, int col) {
        return false;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnClass(int)
     */
    public Class<?> getColumnClass(int col) 
    {
        Class result;
        switch(col)
        {
        case 0:
        case 3:
            result = String.class;
            break;
            
        case 1:
        case 2:
            result = Amount.class;
            break;
        default:
            result = null;
        }
        return result;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int row, int col) 
    {
        AmountFormat af = TextResource.getInstance().getAmountFormat();
        String result = null;
        JournalItem item = items.get(row);
        switch(col)
        {
        case 0:
            result = item.getAccount().getId() + " " + item.getAccount().getName();
            break;
            
        case 1:
            result = item.isDebet() ? af.formatAmountWithoutCurrency(item.getAmount()) : "" ;
            break;
            
        case 2:
            result = item.isCredit() ? af.formatAmountWithoutCurrency(item.getAmount()) : "" ;
            break;
            
        case 3:
            Invoice invoice = item.getInvoice();
            result = invoice != null ? invoice.getId() + " (" + invoice.getPayingParty() + ")" : "";
            break;
        }
        return result;
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#getColumnName(int)
     */
    public String getColumnName(int col) 
    {
        String id = null;
        switch(col)
        {
        case 0: id = "gen.account"; break;
        case 1: id = "gen.debet"; break;
        case 2: id = "gen.credit"; break;
        case 3: id = "gen.invoice"; break;
        }
        return TextResource.getInstance().getString(id);
    }

    /**
     * Gets the journal items of this model.
     * @return the journal items of this model.
     */
    public JournalItem[] getItems()
    {
        JournalItem[] result = new JournalItem[items.size()];
        items.toArray(result);
        return result;
    }
    
    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
     */
    public void addTableModelListener(TableModelListener listener) 
    {
        itemTableModelListeners.add(listener);
    }

    /* (non-Javadoc)
     * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
     */
    public void removeTableModelListener(TableModelListener listener) 
    {
        itemTableModelListeners.remove(listener);
    }

    /**
     * Adds an item to the table.
     * @param item the item to be added.
     */
    public void addItem(JournalItem item)
    {
        items.add(item);
        notifyListeners(new TableModelEvent(this));
    }
    
    /**
     * Gets a specified item from the table model.  
     * @param row the row in which the item is shown
     * @return the specified item or <code>null</code> if the specified row
     *         does not exist.
     */
    public JournalItem getItem(int row)
    {
        JournalItem result = null;
        if (0 <= row && row < items.size())
        {
            result = items.get(row);
        }
        return result;
    }

    /**
     * Updates the item at the specified row in the table model.  
     * @param row the row.
     * @param item the new value for the specified row.
     * @throws IllegalArgumentException if the specified row does not exist. 
     */
    public void updateItem(int row, JournalItem item)
    {
        if (0 <= row && row < items.size())
        {
            items.set(row, item);
            notifyListeners(new TableModelEvent(this));
        }
        else
        {
            throw new IllegalArgumentException("Row " + row + " does not exist!");
        }
    }

    /**
     * Adds an empty item to the table.
     */
    public void addEmptyItem()
    {
        Account dummyAccount = new Account("???", "???", true, Database.getInstance());
        addItem(new JournalItem(Amount.getZero(Database.getInstance().getCurrency()), dummyAccount, true, null));
    }
    
    /**
     * Deletes a row from the model.
     * @param row the row to be deleted. If <code>row</code> is not an existing
     *        row number, then this method returns without changing the model.
     */
    public void deleteItem(int row)
    {
        if (0 <= row && row < items.size())
        {
            items.remove(row);
            notifyListeners(new TableModelEvent(this));
        }
    }
    
    /** Removes all rows from the model. */
    public void clear() {
        items.clear();
        notifyListeners(new TableModelEvent(this));
    }

    /** Should never be called since all methods are not editable. */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }
}