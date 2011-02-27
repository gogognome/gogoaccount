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
package cf.engine.odt;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import nl.gogognome.task.Task;
import nl.gogognome.task.TaskProgressListener;
import nl.gogognome.text.Amount;
import nl.gogognome.text.AmountFormat;
import nl.gogognome.text.TextResource;
import nl.gogognome.xml.GNodeList;
import nl.gogognome.xml.XmlUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import cf.engine.Invoice;
import cf.engine.Payment;


/**
 * This class creates an ODT file based on invoices.
 *
 * @author SanderK
 */
public class InvoiceOdtFileGenerator implements Task {

    /** The inputstream of the template. */
    private ZipInputStream zipInputStream;

    /** The outputstream of the ODT file. */
    private ZipOutputStream zipOutputStream;

    private String templateFileName;
    private String odtFileName;
    private Invoice[] invoices;
    private Date date;
    private String concerning;
    private String ourReference;
    private Date dueDate;

    /**
     * Constructor.
     * @param templateFileName the file name of the template
     * @param odtFileName the file name of the ODT file to be created
     * @param invoices the invoices to be added to the PDF file
     * @param date
     * @param concerning
     * @param ourReference
     * @param dueDate
     */
    public InvoiceOdtFileGenerator(String templateFileName, String odtFileName,
            Invoice[] invoices, Date date, String concerning, String ourReference,
            Date dueDate) {
    	this.templateFileName = templateFileName;
    	this.odtFileName = odtFileName;
    	this.invoices = invoices;
    	this.date = date;
    	this.concerning = concerning;
    	this.ourReference = ourReference;
    	this.dueDate = dueDate;
    }

    /**
     * Generates invoices.
     * @param progressListener the progress listener for this task
     * @return <code>null</code>
     * @throws IOException if a problem occurs while reading the template or
     *         while creating the ODT file
     */
	public Object execute(TaskProgressListener progressListener) throws IOException {
        zipOutputStream = new ZipOutputStream(new BufferedOutputStream(
            new FileOutputStream(odtFileName)));
        progressListener.onProgressUpdate(0);
        long totalFileSize = new File(templateFileName).length();
        long bytesHandled = 0;
        try {
            zipInputStream = new ZipInputStream(new BufferedInputStream(
                new FileInputStream(templateFileName)));
            ZipEntry zipEntry = zipInputStream.getNextEntry();
            while (zipEntry != null) {
                if ("content.xml".equals(zipEntry.getName())) {
                    copyContentXml(zipEntry, invoices, dueDate, concerning);
                } else {
                    copyZipEntry(zipEntry);
                }
                bytesHandled += zipEntry.getCompressedSize();
                progressListener.onProgressUpdate((int)(100 * bytesHandled / totalFileSize));
                zipEntry = zipInputStream.getNextEntry();
            }
        } finally {
            if (zipInputStream != null) {
                try {
                    zipInputStream.close();
                } catch (IOException e) {
                    // ignore this exception
                }
            }
            if (zipOutputStream != null) {
                try {
                    zipOutputStream.close();
                } catch (IOException e) {
                    // ignore this exception
                }
            }
        }
        return null;
    }

    /**
     * Copies a zip entry from the input stream to the output stream.
     * No bytes must have been read from the zip entry in the input stream.
     * A new zip entry is created in the output stream.
     * @param zipEntry the zip entry of the input stream
     * @throws IOException if an I/O problem occurs
     */
    private void copyZipEntry(ZipEntry zipEntry) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(zipEntry));
        byte[] buffer = new byte[16384];
        for (int bytesRead = zipInputStream.read(buffer); bytesRead != -1; bytesRead = zipInputStream.read(buffer)) {
            zipOutputStream.write(buffer, 0, bytesRead);
        }
    }

    /**
     * Copies the content.xml entry from the input stream to the output stream.
     * No bytes must have been read from the zip entry in the input stream.
     * A new zip entry is created in the output stream.
     * The content.xml stream will be modified so that it contains all invoices
     * @param zipEntry the zip entry of the input stream
     * @param invoices the invoices
     * @param dueDate the due date
     * @param concerning specifies what the invoice is about
     * @throws IOException if an I/O problem occurs
     */
    private void copyContentXml(ZipEntry zipEntry, Invoice[] invoices, Date dueDate,
            String concerning) throws IOException {
        zipOutputStream.putNextEntry(new ZipEntry(zipEntry));

        DocumentBuilderFactory docBuilderFac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = docBuilderFac.newDocumentBuilder();
            Document doc = docBuilder.parse(new UncloseableInputStream(zipInputStream));
            Element rootElement = doc.getDocumentElement();
            List<Element> textElements = XmlUtil.getChildElementByTagNames(rootElement, "office:body/office:text");
            if (textElements.size() != 1) {
                throw new IOException("Expected 1 text element but found " + textElements.size() + " elements");
            }

            // Determine the nodes that together form the template of a single invoice.
            ArrayList<Node> invoiceNodes = new ArrayList<Node>(100);
            Element textElement = textElements.get(0);
            for (Node node : new GNodeList(textElement.getChildNodes())) {
                if (node.getNodeName().startsWith("text:") && !("text:sequence-decls".equals(node.getNodeName()))) {
                    invoiceNodes.add(node);
                }
            }

            for (Node node : invoiceNodes) {
                textElement.removeChild(node);
            }

            // Generate the nodes for each invoice.
            for (Invoice invoice : invoices) {
                for (Node node : invoiceNodes) {
                    if (isInvoiceItemnode(node)) {
                        List<Node> invoiceItemNodes = getInvoiceItemNodes(node, invoice);
                        for (Node invoiceItemNode : invoiceItemNodes) {
                            textElement.appendChild(invoiceItemNode);
                        }
                    } else {
                        textElement.appendChild(applySubstitutions(node, invoice, concerning, dueDate));
                    }
                }
            }

            // Write modified XML file to the output stream
            TransformerFactory tFactory = TransformerFactory.newInstance();
            Transformer transformer;
            try {
                transformer = tFactory.newTransformer();
            } catch (TransformerConfigurationException e) {
                throw new IOException("Failed to obtain a transformer: " + e.getMessage());
            }
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(zipOutputStream);
            try {
                transformer.transform(source, result);
            } catch (TransformerException e) {
                throw new IOException("Failed to write XML file: " + e.getMessage());
            }

        } catch (ParserConfigurationException e) {
            throw new IOException("Could not parse content.xml: " + e.getMessage());
        } catch (SAXException e) {
            throw new IOException("Could not parse content.xml: " + e.getMessage());
        }
    }

    /**
     * Replace keywords in the node's texts by attributes of the specified invoice.
     * @param node the node
     * @param invoice the invoice
     * @param concerning TODO
     * @param dueDate  TODO
     * @return the new node that is created by the substition process
     */
    private Node applySubstitutions(Node node, Invoice invoice, String concerning, Date dueDate) {
        TextResource tr = TextResource.getInstance();
        Node result = node.cloneNode(true);
        LinkedList<Node> nodesToProcess = new LinkedList<Node>();
        nodesToProcess.add(result);
        while (!nodesToProcess.isEmpty()) {
            Node curNode = nodesToProcess.removeFirst();
            nodesToProcess.addAll(new GNodeList(curNode.getChildNodes()));
            if (curNode instanceof Text) {
                Text textNode = (Text) curNode;
                String data = textNode.getData();

                StringBuilder sb = new StringBuilder(data.length() * 2);
                sb.append(data);
                replace(sb, "$date$", tr.formatDate("gen.dateFormat", new Date()));
                replace(sb, "$party-name$", invoice.getConcerningParty().getName());
                replace(sb, "$party-id$", invoice.getConcerningParty().getId());
                replace(sb, "$party-address$", invoice.getConcerningParty().getAddress());
                replace(sb, "$party-zip$", invoice.getConcerningParty().getZipCode());
                replace(sb, "$party-city$", invoice.getConcerningParty().getCity());
                replace(sb, "$concerning$", concerning);
                replace(sb, "$our-reference$", invoice.getId());
                replace(sb, "$due-date$", tr.formatDate("gen.dateFormat", dueDate));
                replace(sb, "$total-amount$", formatAmountForOdt(invoice.getRemainingAmountToBePaid(new Date())));

                String newData = sb.toString();
                if (!newData.equals(data)) {
                    textNode.setData(newData);
                }
            }

        }
        return result;
    }

    /**
     * Creates a list of nodes. For each item or payment of the invoice a node
     * is added to the list.
     * @param invoiceItemNode the template node that is used to create the invoice nodes
     * @param invoice the invoice
     * @return the list of nodes
     */
    private List<Node> getInvoiceItemNodes(Node invoiceItemNode, Invoice invoice) {
        TextResource tr = TextResource.getInstance();
        List<Node> result = new ArrayList<Node>(10);

        String[] descriptions = invoice.getDescriptions();
        Amount[] amounts = invoice.getAmounts();
        for (int i=0; i<invoice.getDescriptions().length; i++) {
            String formattedDate = tr.formatDate("gen.dateFormat", invoice.getIssueDate());
            String formattedAmount = amounts[i] != null ? formatAmountForOdt(amounts[i]) : "";
            result.add(applyInvoiceItemSubstitutions(invoiceItemNode, formattedDate, descriptions[i], formattedAmount));
        }

        List<Payment> payments = invoice.getPayments();
        for (Payment payment : payments) {
            String formattedDate = tr.formatDate("gen.dateFormat", payment.getDate());
            String formattedAmount = formatAmountForOdt(payment.getAmount().negate());
            result.add(applyInvoiceItemSubstitutions(invoiceItemNode, formattedDate, payment.getDescription(), formattedAmount));
        }
        return result;
    }

    /**
     * Replace keywords in the node's texts by attributes of the specified invoice item.
     * @param node the node
     * @param date the date
     * @param description the description
     * @param amount the amount to be paid
     * @return the new node that is created by the substition process
     */
    private Node applyInvoiceItemSubstitutions(Node node, String date, String description, String amount) {
        Node result = node.cloneNode(true);
        LinkedList<Node> nodesToProcess = new LinkedList<Node>();
        nodesToProcess.add(result);
        while (!nodesToProcess.isEmpty()) {
            Node curNode = nodesToProcess.removeFirst();
            nodesToProcess.addAll(new GNodeList(curNode.getChildNodes()));
            if (curNode instanceof Text) {
                Text textNode = (Text) curNode;
                String data = textNode.getData();

                StringBuilder sb = new StringBuilder(data.length() * 2);
                sb.append(data);
                replace(sb, "$item-line$", "");
                replace(sb, "$item-date$", date);
                replace(sb, "$item-description$", description);
                replace(sb, "$item-amount$", amount);

                String newData = sb.toString();
                if (!newData.equals(data)) {
                    textNode.setData(newData);
                }
            }
        }
        return result;
    }

    /**
     * Determines whether the specified node is the invoice item node, i.e. whether
     * the node contains the <code>$item-line$</code> keyword.
     * @param node the node
     * @return <code>true</code> if the node is the invoice item node;
     *         <code>false</code> otherwise
     */
    private boolean isInvoiceItemnode(Node node) {
        boolean result = false;
        if (node instanceof Text) {
            result = result || ((Text)node).getData().contains("$item-line$");
        }
        for (Node childNode : new GNodeList(node.getChildNodes())) {
            result = result || isInvoiceItemnode(childNode);
        }
        return result;
    }

    /**
     * Formats an amount for Odt. E.g., EUR is replaced by the
     * euro sign.
     *
     * @param amount the amount
     * @return the amount for Odt
     */
    private String formatAmountForOdt(Amount amount) {
        AmountFormat af = TextResource.getInstance().getAmountFormat();
        String currency = amount.getCurrency().getSymbol();
        if (currency.equals("EUR")) {
            currency = "\u20AC";
        }
        return af.formatAmount(amount, currency);
    }

    /**
     * Replaces all occurrences of <code>oldValue</code> with <code>newValue</code>
     * in the specified string buffer.
     * @param sb the string buffer
     * @param oldValue the old value
     * @param newValue the new value
     */
    private void replace(StringBuilder sb, String oldValue, String newValue) {
        if (newValue == null) {
            newValue = "";
        }
        for (int index = sb.indexOf(oldValue); index != -1; index = sb.indexOf(oldValue)) {
            sb.replace(index, index + oldValue.length(), newValue);
        }
    }

    private static class UncloseableInputStream extends InputStream {

        private InputStream wrappedStream;

        public UncloseableInputStream(InputStream wrappedStream) {
            this.wrappedStream = wrappedStream;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int available() throws IOException {
            return wrappedStream.available();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void close() throws IOException {
            // does nothing. This is the reason why this class was created.
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void mark(int readlimit) {
            wrappedStream.mark(readlimit);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean markSupported() {
            return wrappedStream.markSupported();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read() throws IOException {
            return wrappedStream.read();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(byte[] b) throws IOException {
            return wrappedStream.read(b);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return wrappedStream.read(b, off, len);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void reset() throws IOException {
            wrappedStream.reset();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public long skip(long n) throws IOException {
            return wrappedStream.skip(n);
        }
    }
}
