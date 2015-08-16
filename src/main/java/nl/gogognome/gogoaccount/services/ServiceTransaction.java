package nl.gogognome.gogoaccount.services;

import nl.gogognome.dataaccess.DataAccessException;
import nl.gogognome.dataaccess.transaction.NewTransaction;
import nl.gogognome.dataaccess.transaction.RunnableWithReturnValue;
import nl.gogognome.dataaccess.transaction.RunnableWithoutReturnValue;

public class ServiceTransaction {

    public static void withoutResult(RunnableWithoutReturnValue runnable) throws ServiceException {
        try {
            NewTransaction.withoutResult(runnable);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    public static <T> T withResult(RunnableWithReturnValue<T> runnable) throws ServiceException {
        try {
            return NewTransaction.withResult(runnable);
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

}
