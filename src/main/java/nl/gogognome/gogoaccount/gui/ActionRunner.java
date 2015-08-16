package nl.gogognome.gogoaccount.gui;

import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.MessageDialog;

import java.awt.*;

public class ActionRunner {

    public static void run(Component parentComponent, RunnableWithServiceException runnable) {
        try {
            runnable.run();
        } catch (ServiceException e) {
            MessageDialog.showMessage(parentComponent, "gen.error", "gen.problemOccurred");
        }
    }

    public interface RunnableWithServiceException {
        void run() throws ServiceException;
    }

}
