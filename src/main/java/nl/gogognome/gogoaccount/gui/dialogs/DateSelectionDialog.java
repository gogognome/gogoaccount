package nl.gogognome.gogoaccount.gui.dialogs;

import java.awt.Frame;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.OkCancelDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

/**
 * This class implements a date selection dialog.
 *
 * @author Sander Kooijmans
 */
public class DateSelectionDialog extends OkCancelDialog
{
    /**
     * The date selected by the user. <code>null</code> indicates that
     * the user did not select a date (e.g., by pressing the Cancel button).
     */
    private Date date;

    /**
     * The model used in the date spinner.
     */
    private JSpinner.DateEditor dateEditor;

    private Frame parentFrame;

    /**
     * Constructor.
     * @param frame the frame to which this dialog belongs.
     * @param id the identifer of the description shown in this dialog.
     */
    public DateSelectionDialog(Frame frame, String id)
    {
        super(frame, "ds.selectDate");
        this.parentFrame = frame;

        TextResource tr = Factory.getInstance(TextResource.class);

        JComponent component = new JPanel();
        component.add(new JLabel(tr.getString(id)));

        SpinnerDateModel model = new SpinnerDateModel();
        model.setCalendarField(Calendar.DAY_OF_YEAR);
        JSpinner dateSpinner = new JSpinner(model);
        dateEditor = new JSpinner.DateEditor(dateSpinner,
                tr.getString("gen.dateFormat"));
        dateSpinner.setEditor(dateEditor);

        component.add(dateSpinner);

        componentInitialized(component);
    }

    /* (non-Javadoc)
     * @see cf.ui.OkCancelDialog#handleOk()
     */
    @Override
	protected void handleOk()
    {
        try
        {
            dateEditor.commitEdit();
            date = dateEditor.getModel().getDate();
            hideDialog();
        }
        catch (ParseException e)
        {
            // do not hide the dialog. Let the user try to enter a date again.
            MessageDialog.showErrorMessage(parentFrame, e, "ds.parseErrorMessage");
        }
    }

    /**
     * Gets the date selected by the user.
     * @return the date selected by the user. <code>null</code> indicates that
     *         the user did not select a date (e.g., by pressing the Cancel button).
     */
    public Date getDate()
    {
        return date;
    }
}
