package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.ledger.*;

public class HandleException {

    private final nl.gogognome.lib.swing.dialogs.MessageDialog messageDialog;

    public HandleException(nl.gogognome.lib.swing.dialogs.MessageDialog messageDialog) {
        this.messageDialog = messageDialog;
    }

    public void of(RunnableWithException runnable) {
        try {
            runnable.run();
        } catch (DebetAndCreditAmountsDifferException e) {
            messageDialog.showErrorMessage("ajd.debitAndCreditAmountsDiffer");
        } catch (Exception e) {
            messageDialog.showErrorMessage(e, "gen.problemOccurred");
        }
    }

    public void of(String messageId, RunnableWithException runnable) {
        try {
            runnable.run();
        } catch (DebetAndCreditAmountsDifferException e) {
            messageDialog.showErrorMessage("ajd.debitAndCreditAmountsDiffer");
        } catch (Exception e) {
            messageDialog.showErrorMessage(e, messageId);
        }
    }

    public interface RunnableWithException {
        void run() throws Exception;
    }

}