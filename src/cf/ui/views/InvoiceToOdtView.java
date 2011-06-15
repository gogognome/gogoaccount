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
package cf.ui.views;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import nl.gogognome.lib.gui.beans.DateSelectionBean;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.StringUtil;
import cf.engine.Database;
import cf.engine.odt.InvoiceOdtFileGenerator;

/**
 * This view allows the user to generate invoices (an ODT file) for debtors.
 *
 * @author Sander Kooijmans
 */
public class InvoiceToOdtView extends View {

    /** The database used to determine the invoices. */
    private Database database;

    private JTextField tfTemplateFileName;

    private JTextField tfOdtFileName;

    private DateModel dateModel;

    private JTextField tfConcerning;

    private JTextField tfOurReference;

    private DateModel dueDateModel;

    /**
     * Constructor.
     * @param database the database used by this view
     */
    public InvoiceToOdtView(Database database) {
        super();
        this.database = database;
    }

    /**
     * Checks whether the user has entered all necessary information.
     * If so, then the ODT file will be generated; if not, then an
     * error message is shown to the user.
     */
    protected void handleOk() {
        Date date = dateModel.getDate();
        if (date == null) {
            MessageDialog.showMessage(this, "gen.error",
                TextResource.getInstance().getString("gen.invalidDate"));
            return;
        }

        Date dueDate = dueDateModel.getDate();
        if (dueDate == null) {
            MessageDialog.showMessage(this, "gen.error",
                TextResource.getInstance().getString("gen.invalidDate"));
            return;
        }

        if (StringUtil.isNullOrEmpty(tfOdtFileName.getText())) {
            MessageDialog.showMessage(this, "gen.error",
                TextResource.getInstance().getString("invoiceToOdtView.noOdtFileNameSpecified"));
            return;
        }

        if (StringUtil.isNullOrEmpty(tfTemplateFileName.getText())) {
            MessageDialog.showMessage(this, "gen.error",
                TextResource.getInstance().getString("invoiceToOdtView.noTemplateFileNameSpecified"));
            return;
        }

        // Let the user select the invoices that should be added to the ODT file.
        InvoiceEditAndSelectionView invoicesView = new InvoiceEditAndSelectionView(database, true, true);
        ViewDialog dialog = new ViewDialog(getParentWindow(), invoicesView);
        dialog.showDialog();
        if (invoicesView.getSelectedInvoices() != null) {
            InvoiceOdtFileGenerator converter = new InvoiceOdtFileGenerator(tfTemplateFileName.getText(),
                    tfOdtFileName.getText(), invoicesView.getSelectedInvoices(), date, tfConcerning.getText(),
                    tfOurReference.getText(), dueDate);
            try {
            	TaskWithProgressDialog progressDialog = new TaskWithProgressDialog(this,
            			TextResource.getInstance().getString("invoiceToOdtView.progressDialogTitle"));
            	progressDialog.execute(converter);
            } catch (Exception e) {
                MessageDialog.showErrorMessage(this, e, "invoiceToOdtView.executeTaskException");
                return;
            }
        }

        closeAction.actionPerformed(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        return TextResource.getInstance().getString("invoiceToOdtView.title");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInit() {
        WidgetFactory wf = WidgetFactory.getInstance();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(wf.createLabel("invoiceToOdtView.templateFilename"),
                SwingUtils.createLabelGBConstraints(0, 0));

        tfTemplateFileName = wf.createTextField(30);
        panel.add(tfTemplateFileName,
                SwingUtils.createTextFieldGBConstraints(1, 0));

        JButton button = wf.createButton("gen.btSelectFile", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser(tfTemplateFileName.getText());
                if (fileChooser.showOpenDialog(getParentWindow()) == JFileChooser.APPROVE_OPTION) {
                    tfTemplateFileName.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }

            }
        });
        panel.add(button, SwingUtils.createGBConstraints(2, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            0, 10, 3, 10));

        panel.add(wf.createLabel("invoiceToOdtView.odtFileName"),
                SwingUtils.createLabelGBConstraints(0, 1));
        tfOdtFileName = wf.createTextField(30);
        panel.add(tfOdtFileName,
            SwingUtils.createTextFieldGBConstraints(1, 1));

        button = wf.createButton("gen.btSelectFile", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser(tfOdtFileName.getText());
                if (fileChooser.showOpenDialog(getParentWindow()) == JFileChooser.APPROVE_OPTION) {
                    tfOdtFileName.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }

            }
        });
        panel.add(button, SwingUtils.createGBConstraints(2, 1, 1, 1, 0.0, 0.0,
            GridBagConstraints.WEST, GridBagConstraints.NONE,
            0, 10, 3, 10));

        panel.add(wf.createLabel("invoiceToOdtView.date"),
                SwingUtils.createLabelGBConstraints(0, 2));
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
        panel.add(new DateSelectionBean(dateModel),
                SwingUtils.createLabelGBConstraints(1, 2));

        panel.add(wf.createLabel("invoiceToOdtView.concerning"),
                SwingUtils.createLabelGBConstraints(0, 3));
        tfConcerning = wf.createTextField(30);
        panel.add(tfConcerning,
                SwingUtils.createLabelGBConstraints(1, 3));

        panel.add(wf.createLabel("invoiceToOdtView.ourReference"),
                SwingUtils.createLabelGBConstraints(0, 4));
        tfOurReference = wf.createTextField(30);
        panel.add(tfOurReference,
                SwingUtils.createLabelGBConstraints(1, 4));

        panel.add(wf.createLabel("invoiceToOdtView.dueDate"),
                SwingUtils.createLabelGBConstraints(0, 5));
        dueDateModel = new DateModel();
        dueDateModel.setDate(DateUtil.addDays(new Date(), 14), null);
        panel.add(new DateSelectionBean(dueDateModel),
                SwingUtils.createLabelGBConstraints(1, 5));

        // Create button panel
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
        buttonPanel.add(WidgetFactory.getInstance().createButton("invoiceToOdtView.generate", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent e) {
                handleOk();
            }
        }));
        buttonPanel.add(WidgetFactory.getInstance().createButton("gen.cancel", closeAction));
        panel.add(buttonPanel, SwingUtils.createPanelGBConstraints(1, 6));

        panel.setBorder(new TitledBorder(getTitle()));
        add(panel);
    }
}
