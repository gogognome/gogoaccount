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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.SwingConstants;

import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.task.ui.TaskWithProgressDialog;
import nl.gogognome.lib.util.DateUtil;
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

    private FileModel templateFileModel = new FileModel();

    private FileModel odtFileModel = new FileModel();

    private DateModel dateModel = new DateModel(new Date());

    private StringModel concerningModel = new StringModel();

    private StringModel ourReferenceModel = new StringModel();

    private DateModel dueDateModel = new DateModel();

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
    protected void generateInvoices() {
        Date date = dateModel.getDate();
        if (date == null) {
            MessageDialog.showErrorMessage(this, "gen.invalidDate");
            return;
        }

        Date dueDate = dueDateModel.getDate();
        if (dueDate == null) {
            MessageDialog.showErrorMessage(this, "gen.invalidDate");
            return;
        }

        if (odtFileModel.getFile() == null) {
            MessageDialog.showErrorMessage(this, "invoiceToOdtView.noOdtFileNameSpecified");
            return;
        }

        if (templateFileModel.getFile() == null) {
            MessageDialog.showErrorMessage(this, "invoiceToOdtView.noTemplateFileNameSpecified");
            return;
        }

        // Let the user select the invoices that should be added to the ODT file.
        InvoiceEditAndSelectionView invoicesView = new InvoiceEditAndSelectionView(database, true, true);
        ViewDialog dialog = new ViewDialog(getParentWindow(), invoicesView);
        dialog.showDialog();
        if (invoicesView.getSelectedInvoices() != null) {
            InvoiceOdtFileGenerator converter = new InvoiceOdtFileGenerator(
            		templateFileModel.getFile(),
                    odtFileModel.getFile(), invoicesView.getSelectedInvoices(), date,
                    concerningModel.getString(),
                    ourReferenceModel.getString(), dueDate);
            try {
            	TaskWithProgressDialog progressDialog = new TaskWithProgressDialog(this,
            			textResource.getString("invoiceToOdtView.progressDialogTitle"));
            	progressDialog.execute(converter);
            } catch (Exception e) {
                MessageDialog.showErrorMessage(this, e, "invoiceToOdtView.executeTaskException");
                return;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        return textResource.getString("invoiceToOdtView.title");
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
        InputFieldsColumn vep = new InputFieldsColumn();
        addCloseable(vep);

        vep.addField("invoiceToOdtView.templateFilename", templateFileModel);
        vep.addField("invoiceToOdtView.odtFileName", odtFileModel);
        vep.addField("invoiceToOdtView.date", dateModel);
        vep.addField("invoiceToOdtView.concerning", concerningModel);
        vep.addField("invoiceToOdtView.ourReference", ourReferenceModel);
        dueDateModel.setDate(DateUtil.addMonths(new Date(), 1), null);
        vep.addField("invoiceToOdtView.dueDate", dueDateModel);

        // Create button panel
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
        buttonPanel.add(widgetFactory.createButton("invoiceToOdtView.generate", new AbstractAction() {
            @Override
			public void actionPerformed(ActionEvent e) {
                generateInvoices();
            }
        }));

        // Add panels to view
        setLayout(new BorderLayout());
        add(vep, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        setBorder(widgetFactory.createTitleBorderWithMarginAndPadding("invoiceToOdtView.title"));
    }
}
