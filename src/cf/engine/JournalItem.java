/*
 * $Id: JournalItem.java,v 1.6 2006-11-20 18:40:06 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.engine;

import java.text.ParseException;

import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;

/**
 * This class represents a single item of a journal. 
 *
 * @author Sander Kooijmans
 */
public class JournalItem 
{
    private Amount amount;
    
    private Account account;
    
    private Party party;
    
    /** 
     * Indicates whether the amount in this items is booked on the
     * debet side (true) or credit side (false).
     */
    private boolean debet;
    
    /**
     * Constructs a journal item.
     * @param amount the amount
     * @param account the account on which the amount is booked 
     * @param debet true if the amount is booked on the debet side; false if
     *              the amount is booked on the credit side 
     * @param party the party to which the amount is booked;
     *              <code>null</code> if no party is involved. 
     */
    public JournalItem(Amount amount, Account account, boolean debet, 
            Party party)
    {
        this.amount = amount;
        this.account = account;
        this.debet = debet;
        this.party = party;
    }

    /**
     * Constructs a journal item.
     * @param amount the amount
     * @param account the account on which the amount is booked 
     * @param debet true if the amount is booked on the debet side; false if
     *              the amount is booked on the credit side 
     * @param party the debtor or creditor to which the amount is booked;
     *              <code>null</code> if no debtor or creditor is involved.
     * @deprecated use the constructor with Amount instead of float. 
     */
    public JournalItem(float amount, Account account, boolean debet, 
            Party party)
    {
        AmountFormat af = TextResource.getInstance().getAmountFormat();
        try 
        {
            this.amount = af.parse(Float.toString(amount),
                    Database.getInstance().getCurrency());
        } 
        catch (ParseException e) 
        {
            // should never occur
            e.printStackTrace();
        }
        this.account = account;
        this.debet = debet;
        this.party = party;
    }
    
    public Account getAccount() 
    {
        return account;
    }

    public Amount getAmount() 
    {
        return amount;
    }

    public Party getParty() 
    {
        return party;
    }
    
    public boolean isDebet()
    {
        return debet;
    }
    
    public boolean isCredit()
    {
        return !debet;
    }
}
