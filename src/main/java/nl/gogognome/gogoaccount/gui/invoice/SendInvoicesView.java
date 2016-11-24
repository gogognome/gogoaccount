package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceDetail;
import nl.gogognome.gogoaccount.component.invoice.InvoicePreviewTemplate;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.lib.collections.DefaultValueMap;
import nl.gogognome.lib.gui.beans.Bean;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.action.Actions;
import nl.gogognome.lib.swing.models.BooleanModel;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.util.DateUtil;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.xhtml.XhtmlNamespaceHandler;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;

public abstract class SendInvoicesView extends View {

    private final InvoicePreviewTemplate invoicePreviewTemplate;

    private final XHTMLPanel xhtmlPanel = new XHTMLPanel();
    protected final FileModel templateFileModel = new FileModel();
    protected final StringModel templateModel = new StringModel();
    private final BooleanModel editTemplateModel = new BooleanModel();

    protected List<Invoice> invoicesToSend;
    protected DefaultValueMap<String, List<InvoiceDetail>> invoiceIdToDetails;
    protected DefaultValueMap<String, List<Payment>> invoiceIdToPayments;
    protected Map<String, Party> invoiceIdToParty;

    private JPanel editTemplatePanel;
    private JPanel previewPanel;
    private JPanel twoColumns;
    private Action saveAction;

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
            MessageDialog.showErrorMessage(this, e, "gen.internalError");
            close();
        }
    }

    private void addListeners() {
        templateFileModel.addModelChangeListener(m -> onFileSelectionChanged());
        editTemplateModel.addModelChangeListener(m -> showTemplate());
    }

    private void onFileSelectionChanged() {
        if (templateFileModel.getFile() != null && templateFileModel.getFile().isFile()) {
            saveAction.setEnabled(true);
            SwingUtilities.invokeLater(() -> {
                try {
                    String fileContents = new String(Files.readAllBytes(templateFileModel.getFile().toPath()), Charset.forName("UTF-8"));
                    templateModel.setString(fileContents);
                    updatePreview(fileContents, 0, xhtmlPanel);
                } catch (Exception e) {
                    MessageDialog.showErrorMessage(this, e, "SendInvoicesView.templateFileSyntaxError");
                }
            });
        } else {
            saveAction.setEnabled(false);
        }
    }

    private  void showTemplate() {
        updateTemplateAndPreview();
        getViewOwner().invalidateLayout();
    }

    private void addComponents() throws Exception {
        setLayout(new BorderLayout());
        add(createSettingsPanel(), NORTH);

        twoColumns = new JPanel(new GridLayout());
        editTemplatePanel = createEditTemplatePanel();
        previewPanel = createPreviewPanel();
        updateTemplateAndPreview();
        add(twoColumns, CENTER);
    }

    private void updateTemplateAndPreview() {
        twoColumns.removeAll();
        if (editTemplateModel.getBoolean()) {
            twoColumns.add(editTemplatePanel);
        }
        twoColumns.add(previewPanel);
    }

    private JComponent createSettingsPanel() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("SendInvoicesView.settings"));
        ifc.addField("SendInvoicesView.templateFile", templateFileModel);
        ifc.addField("SendInvoicesView.editTemplate", editTemplateModel);
        return ifc;
    }

    private JPanel createEditTemplatePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(widgetFactory.createTitleBorderWithPadding("SendInvoicesView.template"));

        Bean textAreaBean = beanFactory.createTextAreaBean(templateModel, 0, 0);
        JComponent component = textAreaBean.getComponent();
        panel.add(component, BorderLayout.CENTER);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.add(widgetFactory.createButton("SendInvoicesView.updatePreview", this::onUpdatePreview));
        saveAction = Actions.build(this, this::onSave);
        saveAction.setEnabled(false);
        buttonPanel.add(widgetFactory.createButton("SendInvoicesView.save", saveAction));
        buttonPanel.add(widgetFactory.createButton("SendInvoicesView.saveAs", this::onSaveAs));
        panel.add(buttonPanel, BorderLayout.SOUTH);

        panel.setMinimumSize(getPanelSize());
        panel.setPreferredSize(getPanelSize());
        return panel;
    }

    private JPanel createPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(widgetFactory.createTitleBorderWithPadding("SendInvoicesView.preview"));

        panel.add(new FSScrollPane(xhtmlPanel), BorderLayout.CENTER);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.add(widgetFactory.createButton(getButtonResourceId(), this::onSend));
        panel.add(buttonPanel, BorderLayout.SOUTH);

        panel.setMinimumSize(getPanelSize());
        return panel;
    }

    private Dimension getPanelSize() {
        return new Dimension(600, 800);
    }

    protected abstract String getButtonResourceId();

    private void onSend() {
        HandleException.for_(this, () -> {
            if (send()) {
                close();
            }
        });
    }

    protected abstract boolean send() throws Exception;

    private void onUpdatePreview() {
        updatePreview(templateModel.getString(), 0, xhtmlPanel);
    }

    private void onSave() {
        HandleException.for_(this, () -> save(templateFileModel.getFile()));
    }

    private void onSaveAs() {
        HandleException.for_(this, () -> {
            JFileChooser fileChooser = new JFileChooser(templateFileModel.getFile());
            fileChooser.setDialogType(JFileChooser.FILES_ONLY);
            int choice = fileChooser.showSaveDialog(this);
            if (choice == JFileChooser.APPROVE_OPTION) {
                save(fileChooser.getSelectedFile());
                templateFileModel.setFile(fileChooser.getSelectedFile(), null);
            }
        });
    }

    private void save(File file) throws IOException {
        Files.write(file.toPath(), templateModel.getString().getBytes(Charset.forName("UTF-8")), StandardOpenOption.CREATE);
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
