package nl.gogognome.gogoaccount.component.ledger;

import nl.gogognome.gogoaccount.services.ServiceException;

public class DebetAndCreditAmountsDifferException extends ServiceException {

    DebetAndCreditAmountsDifferException(String message) {
        super(message);
    }
}
