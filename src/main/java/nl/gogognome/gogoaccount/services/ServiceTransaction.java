package nl.gogognome.gogoaccount.services;

import nl.gogognome.dataaccess.DataAccessException;
import nl.gogognome.dataaccess.transaction.RequireTransaction;
import nl.gogognome.dataaccess.transaction.RunnableWithReturnValue;
import nl.gogognome.dataaccess.transaction.RunnableWithoutReturnValue;

public class ServiceTransaction {

    public static void withoutResult(RunnableWithoutReturnValue runnable) throws ServiceException {
        try {
            RequireTransaction.runs(runnable);
        } catch (Exception e) {
            rethrowServiceException(e);
        }
    }

    public static <T> T withResult(RunnableWithReturnValue<T> runnable) throws ServiceException {
        try {
            return RequireTransaction.returns(runnable);
        } catch (Exception e) {
            return rethrowServiceException(e);
        }
    }

    private static <T> T rethrowServiceException(Exception e) throws ServiceException {
        if (e instanceof DataAccessException && e.getCause() instanceof ServiceException) {
            throw (ServiceException) e.getCause();
        }
        if (e instanceof ServiceException) {
            throw (ServiceException) e;
        }
        throw new ServiceException(e.getMessage(), e);
    }

}
