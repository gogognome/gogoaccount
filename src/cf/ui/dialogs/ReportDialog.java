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
package cf.ui.dialogs;

import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.border.TitledBorder;

import nl.gogognome.lib.gui.beans.BeanFactory;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.OkCancelDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Database;
import cf.text.ReportTask;

/**
 * This class implements the report dialog. This dialog can generate
 * a report consisting of balance, operational result and overviews of
 * debtors and creditors, journals and ledger in different file formats.
 *
 * @author Sander Kooijmans
 */
public class ReportDialog extends OkCancelDialog {

    private JTextField tfFileName;
    private DateModel dateModel;

    private JRadioButton rbTxtFile;

    private Frame parentFrame;

    private Database database;

    /**
     * Constructor.
     * @param frame the parent of this dialog
     * @param database the database from which the report is generated
     */
    public ReportDialog(Frame frame, Database database) {
        super(frame, "genreport.title");
        this.parentFrame = frame;
        this.database = database;

        WidgetFactory wf = WidgetFactory.getInstance();
        TextResource tr = TextResource.getInstance();

        // Create file name and date panel
        GridBagLayout gbl = new GridBagLayout();
        JPanel fileNamePanel = new JPanel(gbl);
        JLabel lbTemplateFileName = wf.createLabel("genreport.templateFilename");

        fileNamePanel.add(lbTemplateFileName,
                SwingUtils.createLabelGBConstraints(0, 0));

        tfFileName = wf.createTextField(30);
        fileNamePanel.add(tfFileName,
                SwingUtils.createTextFieldGBConstraints(1, 0));

        JButton button = wf.createButton("gen.btSelectFile", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser(tfFileName.getText());
                if (fileChooser.showOpenDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
                    tfFileName.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }

            }
        });
        fileNamePanel.add(button,
                SwingUtils.createLabelGBConstraints(2, 0));

        JLabel lbDate = wf.createLabel("genreport.date");
        fileNamePanel.add(lbDate,
                SwingUtils.createLabelGBConstraints(0, 1));

        SpinnerDateModel model = new SpinnerDateModel();
        model.setCalendarField(Calendar.DAY_OF_YEAR);
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);

        fileNamePanel.add(BeanFactory.getInstance().createDateSelectionBean(dateModel),
                SwingUtils.createTextFieldGBConstraints(1, 1));

        // Create file type panel
        gbl = new GridBagLayout();
        JPanel fileTypePanel = new JPanel(gbl);
        fileTypePanel.setBorder(new TitledBorder(tr.getString("genreport.fileType")));

        ButtonGroup buttonGroup = new ButtonGroup();

        rbTxtFile = new JRadioButton(wf.createAction("genreport.txt"));
        fileTypePanel.add(rbTxtFile,
                SwingUtils.createGBConstraints(0, 2));
        buttonGroup.add(rbTxtFile);

        rbTxtFile.setSelected(true);

        // Create top panel
        gbl = new GridBagLayout();
        JPanel topPanel = new JPanel(gbl);
        topPanel.add(fileNamePanel,
                SwingUtils.createGBConstraints(0, 0));
        topPanel.add(fileTypePanel,
                SwingUtils.createGBConstraints(0, 1));

        componentInitialized(topPanel);
    }


    /* (non-Javadoc)
     * @see nl.gogognome.swing.OkCancelDialog#handleOk()
     */
    @Override
	protected void handleOk() {
        int fileType = ReportTask.RP_TXT;

        Date date = dateModel.getDate();
        if (date == null) {
            MessageDialog.showErrorMessage(parentFrame, "ds.parseErrorMessage");
            return;
        }

        hideDialog();
        TaskWithProgressDialog taskWithProgressDialog =
        	new TaskWithProgressDialog(parentFrame, TextResource.getInstance().getString("genreport.progress"));
        taskWithProgressDialog.execute(new ReportTask(database, date, tfFileName.getText(), fileType));
    }
}
