package nl.gogognome.gogoaccount.gui.invoice;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.nio.charset.*;
import org.junit.jupiter.api.*;

class PdfGeneratorTest {

	private static final String template = """
			<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en-us">
			<head>
			    <meta http-equiv="content-type" content="text/html; charset=utf-8"/>
			    <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1"/>
			    <title>Invoice</title>
			</head>

			<body>
				<p>This is a test invoice.</p>
			</body>
			</html>
			""";

	@Test
	void testWritingPdfToStream() throws Exception {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		new PdfGenerator().writePdfToStream(template, "https://gogognome.nl", outputStream);

		String headerOfPdf = new String(outputStream.toByteArray(), 0, 4, StandardCharsets.UTF_8);
		assertEquals("%PDF", headerOfPdf);
	}

}