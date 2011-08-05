/**
 *
 */
package cf.ui.views;

import java.awt.Component;
import java.io.File;
import java.util.Date;

import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.FileModel;

/**
 * This view lets the user fill in parameters to generate a report.
 * A report consists of balance sheet, operational result and overviews of
 * debtors and creditors, journals and ledger.
 *
 * @author Sander Kooijmans
 */
public class GenerateReportView extends OkCancelView {

	private static final long serialVersionUID = 1L;

	private DateModel dateModel;
    private FileModel reportFileModel;

	@Override
	public String getTitle() {
		return textResource.getString("genreport.title");
	}

	@Override
	public void onInit() {
		initModels();
		addComponents();
	}

	private void initModels() {
		dateModel = new DateModel(new Date());
		reportFileModel = new FileModel();
	}

	@Override
	protected Component createCenterComponent() {
		InputFieldsColumn column = new InputFieldsColumn();
		addCloseable(column);

		column.addField("genreport.reportFilename", reportFileModel);
		column.addField("genreport.date", dateModel);

		return column;
	}

	@Override
	protected void onOk() {
        Date date = dateModel.getDate();
        if (date == null) {
            MessageDialog.showWarningMessage(this, "ds.parseErrorMessage");
            return;
        }

        File reportFile = reportFileModel.getFile();
        if (reportFile == null) {
        	MessageDialog.showWarningMessage(this, "genreport.noReportFileSelected");
        	return;
        }

        requestClose();
	}

	public Date getDate() {
		return dateModel.getDate();
	}

	public File getReportFile() {
		return reportFileModel.getFile();
	}

	@Override
	public void onClose() {
	}
}
