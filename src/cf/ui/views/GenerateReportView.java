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
import java.awt.Component;
import java.io.File;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import nl.gogognome.gogoaccount.businessobjects.ReportType;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.gui.beans.RadioButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.BooleanModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.OkCancelView;

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
    private FileModel templateFileModel;
    private BooleanModel txtModel;
    private BooleanModel odtModel;

    private Date selectedDate;
    private File selectedReportFile;
    private File selectedTemplateFile;
    private ReportType reportType;
    
    private ModelChangeListener odtSelectionListener;

	@Override
	public String getTitle() {
		return textResource.getString("genreport.title");
	}

	@Override
	public void onInit() {
		initModels();
		addComponents();
		addListeners();
		updateTemplateSelectionModel();
	}

	private void initModels() {
		dateModel = new DateModel(new Date());
		reportFileModel = new FileModel();
		templateFileModel = new FileModel();
		txtModel = new BooleanModel();
		txtModel.setBoolean(true);
		odtModel = new BooleanModel();
	}

	@Override
	protected Component createCenterComponent() {
		InputFieldsColumn column = new InputFieldsColumn();
		addCloseable(column);

		column.addField("genreport.reportFile", reportFileModel);
		column.addField("genreport.date", dateModel);
		column.addField("genreport.templateFile", templateFileModel);
		column.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

		JPanel typePanel = createTypePanel();

		JPanel panel = new JPanel(new BorderLayout());

		panel.add(column, BorderLayout.NORTH);
		panel.add(typePanel, BorderLayout.SOUTH);
		return panel;
	}

	private JPanel createTypePanel() {
		RadioButtonPanel panel = new RadioButtonPanel();
		panel.addRadioButton("genreport.txt", txtModel);
		panel.addRadioButton("genreport.odt", odtModel);
		panel.setBorder(widgetFactory.createTitleBorder("genreport.fileType"));
		return panel;
	}

	private void addListeners() {
		odtSelectionListener = new OdtSelectionListener();
		odtModel.addModelChangeListener(odtSelectionListener);
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

        File templateFile = templateFileModel.getFile();
        if (templateFile == null && odtModel.getBoolean()) {
        	MessageDialog.showWarningMessage(this, "genreport.noTemplateFileSelected");
        	return;
        }

        selectedDate = date;
        selectedReportFile = reportFile;
        selectedTemplateFile = templateFile;
        reportType = txtModel.getBoolean() ? ReportType.PLAING_TEXT : ReportType.ODT_DOCUMENT;
        
        requestClose();
	}

	public Date getDate() {
		return selectedDate;
	}

	public File getReportFile() {
		return selectedReportFile;
	}

	public File getTemplateFile() {
		return selectedTemplateFile;
	}

	public ReportType getReportType() {
		return reportType;
	}
	
	private void updateTemplateSelectionModel() {
		templateFileModel.setEnabled(odtModel.getBoolean(), odtSelectionListener);
	}

	@Override
	public void onClose() {
		removeListeners();
	}

	private void removeListeners() {
		odtModel.removeModelChangeListener(odtSelectionListener);
	}

	private class OdtSelectionListener implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
			updateTemplateSelectionModel();
		}
	}
}
