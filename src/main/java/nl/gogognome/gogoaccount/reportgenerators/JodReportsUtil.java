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
