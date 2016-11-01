package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.lib.gui.beans.Bean;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.xhtml.XhtmlNamespaceHandler;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static java.awt.BorderLayout.*;

public class PrintInvoicesView extends View {

    private final InvoiceService invoiceService;

    private final XHTMLPanel xhtmlPanel = new XHTMLPanel();
    private final FileModel templateFileModel = new FileModel();
    private final StringModel templateModel = new StringModel();

    private List<Invoice> invoicesToPrint;
    private Map<Invoice, Party> invoiceToParty;

    public PrintInvoicesView(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @Override
    public String getTitle() {
        return textResource.getString("PrintInvoicesView.title");
    }

    public void setInvoicesToPrint(List<Invoice> invoicesToPrint, Map<Invoice, Party> invoiceToParty) {
        this.invoicesToPrint = invoicesToPrint;
        this.invoiceToParty = invoiceToParty;
    }

    @Override
    public void onInit() {
        try {
            if (invoicesToPrint == null || invoiceToParty == null) {
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
                    updatePreview(fileContents);
                } catch (Exception e) {
                    MessageDialog.showErrorMessage(this, "PrintInvoicesView.templateFileSyntaxError", e);
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
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("PrintInvoicesView.settings"));
        ifc.addField("PrintInvoicesView.templateFile", templateFileModel);
        return ifc;
    }

    private JComponent createTemplatePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(widgetFactory.createTitleBorderWithPadding("PrintInvoicesView.template"));

        Bean textAreaBean = beanFactory.createTextAreaBean(templateModel, 0, 0);
        JComponent component = textAreaBean.getComponent();
        panel.add(component, BorderLayout.CENTER);

        JButton updatePreviewButton = widgetFactory.createButton("PrintInvoicesView.updatePreview", this::onUpdatePreview);
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.add(updatePreviewButton);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JComponent createPreviewPanel() throws Exception {
        FSScrollPane scrollPane = new FSScrollPane(xhtmlPanel);
        scrollPane.setBorder(widgetFactory.createTitleBorderWithPadding("PrintInvoicesView.preview"));
        return scrollPane;
    }

    private void onUpdatePreview() {
        updatePreview(templateModel.getString());
    }

    private void updatePreview(String fileContents) {
        Invoice invoice = invoicesToPrint.get(0);
        String fileContentsWithValuesFilledIn = invoiceService.fillInParametersInTemplate(fileContents, invoice, invoiceToParty.get(invoice));
        xhtmlPanel.setDocumentFromString(fileContentsWithValuesFilledIn, templateFileModel.getFile().toURI().toString(), new XhtmlNamespaceHandler());
    }

    @Override
    public void onClose() {

    }

}
