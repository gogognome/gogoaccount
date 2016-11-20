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
import org.xhtmlrenderer.simple.xhtml.XhtmlNamespaceHandler;

import javax.swing.*;
import java.awt.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static java.awt.BorderLayout.*;

public abstract class SendInvoicesView extends View {

    private final InvoicePreviewTemplate invoicePreviewTemplate;

    private final XHTMLPanel xhtmlPanel = new XHTMLPanel();
    protected final FileModel templateFileModel = new FileModel();
    protected final StringModel templateModel = new StringModel();

    protected List<Invoice> invoicesToSend;
    protected DefaultValueMap<String, List<InvoiceDetail>> invoiceIdToDetails;
    protected DefaultValueMap<String, List<Payment>> invoiceIdToPayments;
    protected Map<String, Party> invoiceIdToParty;

    public SendInvoicesView(InvoicePreviewTemplate invoicePreviewTemplate) {
        this.invoicePreviewTemplate = invoicePreviewTemplate;
    }

    public void setInvoicesToSend(List<Invoice> invoicesToSend, DefaultValueMap<String, List<InvoiceDetail>> invoiceIdToDetails,
                                  DefaultValueMap<String, List<Payment>> invoiceIdToPayments, Map<String, Party> invoiceIdToParty) {
        this.invoicesToSend = invoicesToSend;
        this.invoiceIdToDetails = invoiceIdToDetails;
        this.invoiceIdToPayments = invoiceIdToPayments;
        this.invoiceIdToParty = invoiceIdToParty;
    }

    @Override
    public String getTitle() {
        return textResource.getString(getClass().getSimpleName() + ".title");
    }

    @Override
    public void onInit() {
        try {
            if (invoicesToSend == null || invoiceIdToParty == null) {
                throw new IllegalStateException("Call setInvoicesToSend() before calling onInit()!");
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
        buttonPanel.add(widgetFactory.createButton(getButtonResourceId(), this::onSend));
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    protected abstract String getButtonResourceId();

    private void onSend() {
        try {
            if (send()) {
                close();
            }
        } catch (Exception e) {
            MessageDialog.showErrorMessage(this, "gen.internalError", e);
        }
    }

    protected abstract boolean send() throws Exception;

    private void onUpdatePreview() {
        updatePreview(templateModel.getString(), 0, xhtmlPanel);
    }


    protected void updatePreview(String fileContents, int invoiceIndex, XHTMLPanel xhtmlPanel) {
        Invoice invoice = invoicesToSend.get(invoiceIndex);
        String fileContentsWithValuesFilledIn = fillInParametersInTemplate(fileContents, invoice);
        xhtmlPanel.setDocumentFromString(fileContentsWithValuesFilledIn, templateFileModel.getFile().toURI().toString(), new XhtmlNamespaceHandler());
    }

    protected String fillInParametersInTemplate(String fileContents, Invoice invoice) {
        return invoicePreviewTemplate.fillInParametersInTemplate(fileContents, invoice,
                    invoiceIdToDetails.get(invoice.getId()), invoiceIdToPayments.get(invoice.getId()),
                    invoiceIdToParty.get(invoice.getId()), DateUtil.addMonths(invoice.getIssueDate(), 1));
    }

    @Override
    public void onClose() {
    }

}
