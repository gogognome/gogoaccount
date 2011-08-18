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
package nl.gogognome.gogoaccount.tools;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jooreports.templates.DocumentTemplate;
import net.sf.jooreports.templates.UnzippedDocumentTemplate;
import net.sf.jooreports.templates.ZippedDocumentTemplate;

/**
 * Command line tool to create a document from a template and a data file and
 * convert it to the specified format.
 *
 * The data file can be in XML format or a simple .properties file.
 */
public class CreateAndConvertDocument {

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.err.println("USAGE: "
					+ CreateAndConvertDocument.class.getName() + " ");
			System.exit(0);
		}
		File templateFile = new File(args[0]);
		File outputFile = new File(args[1]);

		DocumentTemplate template = null;
		if (templateFile.isDirectory()) {
			template = new UnzippedDocumentTemplate(templateFile);
		} else {
			template = new ZippedDocumentTemplate(templateFile);
		}

		Map<String, Object> model = new HashMap<String, Object>();
		model.put("date", "31 december 2011");
		model.put("balance", createBalanceLines());
		model.put("operationalResult", createBalanceLines());
		model.put("debtors", createDebtors());
		model.put("creditors", createDebtors());
		model.put("accounts", createAccounts());

		template.createDocument(model, new FileOutputStream(outputFile));
	}

	private static Object createBalanceLines() {
		List<Map<String, Object>> lines = new ArrayList<Map<String,Object>>();
		lines.add(createLine("100 Kas", "EUR 0,00", "200 Eigen vermogen", "231,95"));
		lines.add(createLine("101 Betaalrekening", "EUR 1231,53", "290 Crediteuren", "EUR 21,10"));
		lines.add(createLine("", "", "", ""));
		lines.add(createLine("TOTAAL", "EUR 1231,53", "TOTAAL", "1221,10"));
		return lines;
	}

	private static Map<String, Object> createLine(String name1,
			String amount1, String name2, String amount2) {
		Map<String,Object> line = new HashMap<String, Object>();
		line.put("name1", name1);
		line.put("amount1", amount1);
		line.put("name2", name2);
		line.put("amount2", amount2);
		return line;
	}

	private static Object createDebtors() {
		List<Map<String, Object>> lines = new ArrayList<Map<String,Object>>();
		lines.add(createLine("Pietje Puk", "EUR\u00a023,00"));
		lines.add(createLine("Hans Anders", "EUR\u00a0316,00"));
		return lines;
	}



	private static Map<String, Object> createLine(String name, String amount) {
		Map<String,Object> line = new HashMap<String, Object>();
		line.put("name", name);
		line.put("amount", amount);
		return line;
	}

	private static Object createAccounts() {
		List<Map<String, Object>> accounts = new ArrayList<Map<String,Object>>();
		accounts.add(createAccount("100 Kas"));
		accounts.add(createAccount("101 Betaalrekening"));
		return accounts;
	}

	private static Map<String, Object> createAccount(String title) {
		Map<String,Object> account = new HashMap<String, Object>();
		account.put("title", title);
		account.put("lines", createAccountLines());
		return account;
	}

	private static Object createAccountLines() {
		List<Map<String, Object>> lines = new ArrayList<Map<String,Object>>();
		lines.add(createLine("01-06-2011", "decl21-001 - Declaratie ALV", "23,00", "", "Hans Anders"));
		lines.add(createLine("03-08-2011", "decl21-001 - Declaratie ALV", "", "23,00", "Pietje Puk"));
		return lines;
	}

	private static Map<String, Object> createLine(String date,
			String description, String debet, String credit, String invoice) {
		Map<String,Object> line = new HashMap<String, Object>();
		line.put("date", date);
		line.put("description", description);
		line.put("debet", debet);
		line.put("credit", credit);
		line.put("invoice", invoice);
		return line;
	}

	public static class Line {
		public String one;
		public String two;
		public String three;
		public Line(String one, String two, String three) {
			super();
			this.one = one;
			this.two = two;
			this.three = three;
		}
	}
}