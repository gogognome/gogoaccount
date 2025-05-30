package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceDetail;
import nl.gogognome.gogoaccount.component.invoice.InvoicePreviewTemplate;
import nl.gogognome.gogoaccount.component.invoice.Payment;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.settings.SettingsService;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.collections.DefaultValueMap;
import nl.gogognome.lib.gui.beans.Bean;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.action.Actions;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.BooleanModel;
import nl.gogognome.lib.swing.models.FileModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xhtmlrenderer.simple.FSScrollPane;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.simple.xhtml.XhtmlNamespaceHandler;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static java.awt.BorderLayout.CENTER;
import static java.awt.BorderLayout.NORTH;

public abstract class SendInvoicesView extends View {

    private final static String TEMPLATE_FILE_KEY = "SendInvoicesView.templateFile";
    private final static Logger LOGGER = LoggerFactory.getLogger(SendInvoicesView.class);

    private final Document document;
    private final InvoicePreviewTemplate invoicePreviewTemplate;
    private final SettingsService settingsService;
    private final MessageDialog messageDialog;
    private final HandleException handleException;

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

    public SendInvoicesView(Document document, InvoicePreviewTemplate invoicePreviewTemplate, SettingsService settingsService) {
        this.document = document;
        this.invoicePreviewTemplate = invoicePreviewTemplate;
        this.settingsService = settingsService;
        messageDialog = new MessageDialog(textResource, this);
        handleException = new HandleException(messageDialog);
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
        handleException.of(() -> {
            if (invoicesToSend == null || invoiceIdToParty == null) {
                throw new IllegalStateException("Call setInvoicesToSend() before calling onInit()!");
            }
            addComponents();
            addListeners();
            loadLastOpenedTemplateFile();
        });
    }

    private void loadLastOpenedTemplateFile() {
        try {
            String templateFile = settingsService.findValueForSetting(document, TEMPLATE_FILE_KEY);
            if (templateFile != null) {
                templateFileModel.setFile(new File(templateFile), null);
            }
        } catch (ServiceException e) {
            LOGGER.warn("Ignored exception while getting setting " + e.getMessage(), e);
        }
    }

    private void addListeners() {
        templateFileModel.addModelChangeListener(m -> onFileSelectionChanged());
        editTemplateModel.addModelChangeListener(m -> showTemplate());
    }

    private void onFileSelectionChanged() {
        if (templateFileModel.getFile() != null && templateFileModel.getFile().isFile()) {
            try {
                settingsService.save(document, TEMPLATE_FILE_KEY, templateFileModel.getFile().getAbsolutePath());
            } catch (ServiceException e) {
                LOGGER.warn("Ignored exception while saving setting " + TEMPLATE_FILE_KEY, e);
            }
            saveAction.setEnabled(true);
            SwingUtilities.invokeLater(() -> {
                handleException.of(() -> {
                    String fileContents = new String(Files.readAllBytes(templateFileModel.getFile().toPath()), Charset.forName("UTF-8"));
                    templateModel.setString(fileContents);
                    updatePreview(fileContents, 0, xhtmlPanel);
                });
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
        handleException.of(() -> {
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
        handleException.of(() -> save(templateFileModel.getFile()));
    }

    private void onSaveAs() {
        handleException.of(() -> {
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
        Files.write(file.toPath(), templateModel.getString().getBytes(Charset.forName("UTF-8")));
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
