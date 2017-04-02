package nl.gogognome.gogoaccount.reportgenerators;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import net.sf.jooreports.templates.DocumentTemplate;
import net.sf.jooreports.templates.DocumentTemplateException;
import net.sf.jooreports.templates.UnzippedDocumentTemplate;
import net.sf.jooreports.templates.ZippedDocumentTemplate;

/**
 * Helper class for the JodReports library.
 *
 * @author Sander Kooijmans
 */
public class JodReportsUtil {

	private JodReportsUtil() {
	}

	/**
	 * Creates a document based on a template file and a model.
	 * @param templateFile
	 * @param outputFile
	 * @param model
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws DocumentTemplateException
	 */
	public static void createDocument(File templateFile, File outputFile, Object model)
			throws FileNotFoundException, IOException, DocumentTemplateException {
		DocumentTemplate template = null;
		if (templateFile.isDirectory()) {
			template = new UnzippedDocumentTemplate(templateFile);
		} else {
			template = new ZippedDocumentTemplate(templateFile);
		}

		template.createDocument(model, new FileOutputStream(outputFile));
	}

}
