package nl.gogognome.gogoaccount.reportgenerators;

import java.io.File;
import java.util.Map;

import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.task.Task;
import nl.gogognome.lib.task.TaskProgressListener;

/**
 * Generates invoices as an ODT document based on a template file.
 */
public class OdtInvoiceGeneratorTask implements Task {

	private final OdtInvoiceParameters parameters;

	private final File reportFile;
	private final File templateFile;

    private Map<String, Object> model;

	public OdtInvoiceGeneratorTask(OdtInvoiceParameters parameters,
			File reportFile, File templateFile) {
		super();
		this.parameters = parameters;
		this.reportFile = reportFile;
		this.templateFile = templateFile;
	}

	@Override
	public Object execute(TaskProgressListener progressListener) throws Exception {
    	progressListener.onProgressUpdate(0);

    	createModel();
    	progressListener.onProgressUpdate(50);

    	JodReportsUtil.createDocument(templateFile, reportFile, model);
    	progressListener.onProgressUpdate(100);

		return null;
	}

	private void createModel() throws ServiceException {
		InvoicesToModelConverter converter = new InvoicesToModelConverter(parameters);
		model = converter.getModel();
	}
}
