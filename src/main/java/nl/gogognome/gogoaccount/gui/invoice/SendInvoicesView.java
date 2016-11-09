package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.invoice.*;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.collections.DefaultValueMap;
import nl.gogognome.lib.gui.beans.Bean;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.util.DateUtil;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.XHTMLPrintable;
import org.xhtmlrenderer.simple.xhtml.XhtmlNamespaceHandler;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static java.awt.BorderLayout.*;

public class SendInvoicesView extends View {

    private final InvoicePreviewTemplate invoicePreviewTemplate;

    private final XHTMLPanel xhtmlPanel = new XHTMLPanel();
    private final FileModel templateFileModel = new FileModel();
    private final StringModel templateModel = new StringModel();

    private List<Invoice> invoicesToPrint;
    private DefaultValueMap<String, List<InvoiceDetail>> invoiceIdToDetails;
    private DefaultValueMap<String, List<Payment>> invoiceIdToPayments;
    private Map<String, Party> invoiceIdToParty;

    public SendInvoicesView(InvoicePreviewTemplate invoicePreviewTemplate) {
        this.invoicePreviewTemplate = invoicePreviewTemplate;
    }

    @Override
    public String getTitle() {
        return textResource.getString("SendInvoicesView.title");
    }

    public void setInvoicesToPrint(List<Invoice> invoicesToPrint, DefaultValueMap<String, List<InvoiceDetail>> invoiceIdToDetails,
                                   DefaultValueMap<String, List<Payment>> invoiceIdToPayments, Map<String, Party> invoiceIdToParty) {
        this.invoicesToPrint = invoicesToPrint;
        this.invoiceIdToDetails = invoiceIdToDetails;
        this.invoiceIdToPayments = invoiceIdToPayments;
        this.invoiceIdToParty = invoiceIdToParty;
    }

    @Override
    public void onInit() {
        try {
            if (invoicesToPrint == null || invoiceIdToParty == null) {
                throw new IllegalStateException("Call setInvoicesToPrint() before calling onInit()!");
            }
            addComponents();
            addListeners();
        } catch (Exception e) {
            MessageDialog.showErrorMessage(this, "gen.internalError", e);
            close();
        }
    }

    private void addListeners() {
        templateFileModel.addModelChangeListener(m -> onFileSelectionChanged());
    }

    private void onFileSelectionChanged() {
        if (templateFileModel.getFile() != null && templateFileModel.getFile().isFile()) {
            SwingUtilities.invokeLater(() -> {
                try {
                    String fileContents = new String(Files.readAllBytes(templateFileModel.getFile().toPath()), Charset.forName("UTF-8"));
                    templateModel.setString(fileContents);
                    updatePreview(fileContents, 0, xhtmlPanel);
                } catch (Exception e) {
                    MessageDialog.showErrorMessage(this, "SendInvoicesView.templateFileSyntaxError", e);
                }
            });
        }
    }

    private void addComponents() throws Exception {
        setLayout(new BorderLayout());
        add(createSettingsPanel(), NORTH);

        JPanel twoColumns = new JPanel(new GridLayout());
        twoColumns.add(createTemplatePanel());
        twoColumns.add(createPreviewPanel());
        add(twoColumns, CENTER);
    }

    private JComponent createSettingsPanel() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("SendInvoicesView.settings"));
        ifc.addField("SendInvoicesView.templateFile", templateFileModel);
        return ifc;
    }

    private JComponent createTemplatePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(widgetFactory.createTitleBorderWithPadding("SendInvoicesView.template"));

        Bean textAreaBean = beanFactory.createTextAreaBean(templateModel, 0, 0);
        JComponent component = textAreaBean.getComponent();
        panel.add(component, BorderLayout.CENTER);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.add(widgetFactory.createButton("SendInvoicesView.updatePreview", this::onUpdatePreview));
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent createPreviewPanel() throws Exception {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(widgetFactory.createTitleBorderWithPadding("SendInvoicesView.preview"));

        panel.add(new FSScrollPane(xhtmlPanel), BorderLayout.CENTER);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.add(widgetFactory.createButton("gen.print", this::onPrint));
        buttonPanel.add(widgetFactory.createButton("SendInvoicesView.exportPdf", this::onExportPdf));
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void onUpdatePreview() {
        updatePreview(templateModel.getString(), 0, xhtmlPanel);
    }

    private void onPrint() {
        PrinterJob printJob = PrinterJob.getPrinterJob();
        printJob.setPrintable(new SelectedInvoicesPrintable());
        if (printJob.printDialog()) {
            try {
                printJob.print();
            } catch (PrinterException e) {
                MessageDialog.showErrorMessage(this, "gen.internalError", e);
            }
        }
    }

    private class SelectedInvoicesPrintable implements Printable {

        @Override
        public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
            if (pageIndex >= invoicesToPrint.size()) {
                return Printable.NO_SUCH_PAGE;
            }
            XHTMLPanel pagePanel = new XHTMLPanel();
            updatePreview(templateModel.getString(), pageIndex, pagePanel);
            return new XHTMLPrintable(pagePanel).print(graphics, pageFormat, 0);
        }
    }

    private void onExportPdf() {

    }

    private void updatePreview(String fileContents, int invoiceIndex, XHTMLPanel xhtmlPanel) {
        Invoice invoice = invoicesToPrint.get(invoiceIndex);
        String fileContentsWithValuesFilledIn = invoicePreviewTemplate.fillInParametersInTemplate(fileContents, invoice,
                invoiceIdToDetails.get(invoice.getId()), invoiceIdToPayments.get(invoice.getId()),
                invoiceIdToParty.get(invoice.getId()), DateUtil.addMonths(invoice.getIssueDate(), 1));
        xhtmlPanel.setDocumentFromString(fileContentsWithValuesFilledIn, templateFileModel.getFile().toURI().toString(), new XhtmlNamespaceHandler());
    }

    @Override
    public void onClose() {

    }

}
