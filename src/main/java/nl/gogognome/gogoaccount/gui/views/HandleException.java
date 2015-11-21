package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.lib.swing.MessageDialog;

import java.awt.*;

public class HandleException {

    public static void for_(Component parentComponent, RunnableWithException runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            MessageDialog.showErrorMessage(parentComponent, "gen.internalError", e);
        }
    }

    public static interface RunnableWithException {
        void run() throws Exception;
    }
}
