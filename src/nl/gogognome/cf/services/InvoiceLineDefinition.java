/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package nl.gogognome.cf.services;

import nl.gogognome.text.Amount;
import cf.engine.Account;

/**
 * This class represents a line of an invoice.
 *
 * @author Sander Kooijmans
 */
public class InvoiceLineDefinition {

    private boolean amountToBePaid;

    private Amount debet;

    private Amount credit;

    private Account account;

    public InvoiceLineDefinition(Amount debet, Amount credit, Account account,
        boolean amountToBePaid) {
        super();
        this.debet = debet;
        this.credit = credit;
        this.account = account;
        this.amountToBePaid = amountToBePaid;
    }

    public boolean isAmountToBePaid() {
        return amountToBePaid;
    }

    public Amount getDebet() {
        return debet;
    }

    public Amount getCredit() {
        return credit;
    }

    public Account getAccount() {
        return account;
    }

}
