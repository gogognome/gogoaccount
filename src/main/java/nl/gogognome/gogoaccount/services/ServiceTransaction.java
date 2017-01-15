package nl.gogognome.gogoaccount.services;

import nl.gogognome.dataaccess.transaction.RequireTransaction;
import nl.gogognome.dataaccess.transaction.RunnableWithReturnValue;
import nl.gogognome.dataaccess.transaction.RunnableWithoutReturnValue;

public class ServiceTransaction {

    public static void withoutResult(RunnableWithoutReturnValue runnable) throws ServiceException {
        try {
            RequireTransaction.runs(runnable);
        } catch (Exception e) {
            if (e instanceof ServiceException) {
                throw e;
            }
            throw new ServiceException(e.getMessage(), e);
        }
    }

    public static <T> T withResult(RunnableWithReturnValue<T> runnable) throws ServiceException {
        try {
            return RequireTransaction.returns(runnable);
        } catch (Exception e) {
            if (e instanceof ServiceException) {
                throw e;
            }
            throw new ServiceException(e.getMessage(), e);
        }
    }

}
