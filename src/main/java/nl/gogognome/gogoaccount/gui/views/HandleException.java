package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.lib.swing.MessageDialog;

import java.awt.*;

public class HandleException {

    public static void for_(Component parentComponent, RunnableWithException runnable) {
        for_(parentComponent, "gen.problemOccurred", runnable);
    }

    public static void for_(Component parentComponent, String messageId, RunnableWithException runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            MessageDialog.showErrorMessage(parentComponent, e, messageId);
        }
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }
}
