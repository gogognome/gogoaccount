package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionService;
import nl.gogognome.gogoaccount.component.automaticcollection.PartyAutomaticCollectionSettings;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentListener;
import nl.gogognome.gogoaccount.component.invoice.*;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.tablecellrenderer.AmountCellRenderer;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.collections.DefaultValueMap;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.*;
import nl.gogognome.lib.swing.action.ActionWrapper;
import nl.gogognome.lib.swing.models.BooleanModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.models.Tables;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.criteria.Parser;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static nl.gogognome.lib.util.StringUtil.isNullOrEmpty;

public class InvoicesView extends View {

    private final Document document;
    private final AmountFormat amountFormat;

    private final AutomaticCollectionService automaticCollectionService;
    private final ConfigurationService configurationService;
    private final InvoiceService invoiceService;
    private final PartyService partyService;
    private final EditInvoiceController editInvoiceController;
    private final ViewFactory viewFactory;

    private StringModel searchCriterionModel = new StringModel();
    private BooleanModel includePaidInvoicesModel = new BooleanModel();
    private ActionWrapper editSelectedInvoiceAction = widgetFactory.createActionWrapper("InvoicesView.edit", this::onEditSelectedInvoice);
    private ActionWrapper emailSelectedInvoicesAction = widgetFactory.createActionWrapper("EmailInvoicesView.sendEmail", this::onEmail);
    private ActionWrapper exportPdfsSelectedInvoicesAction = widgetFactory.createActionWrapper("ExportPdfsInvoicesView.exportPdf", this::onExportPdfs);
    private ActionWrapper printSelectedInvoicesAction = widgetFactory.createActionWrapper("gen.print", this::onPrint);
    private ActionWrapper generateAutomaticCollectionFileAction = widgetFactory.createActionWrapper("InvoicesView.generateAutomaticCollectionFile", this::onGenerateAutomaticCollectionFile);

    private DocumentListener documentListener;

    private JTable table;
    private ListTableModel<InvoiceOverview> invoicesTableModel;
    private JButton btSearch;
    private CloseableJPanel invoiceDetailsPanel;

    public InvoicesView(Document document, AmountFormat amountFormat, AutomaticCollectionService automaticCollectionService, ConfigurationService configurationService, InvoiceService invoiceService, PartyService partyService,
                        EditInvoiceController editInvoiceController, ViewFactory viewFactory) {
        this.document = document;
        this.amountFormat = amountFormat;
        this.automaticCollectionService = automaticCollectionService;
        this.configurationService = configurationService;
        this.invoiceService = invoiceService;
        this.partyService = partyService;
        this.editInvoiceController = editInvoiceController;
        this.viewFactory = viewFactory;
    }

    @Override
    public String getTitle() {
        return textResource.getString("invoicesView.title");
    }

    @Override
    public void onInit() {
        try {
            addComponents();
            onSearch();
            addListeners();
        } catch (Exception e) {
            MessageDialog.showErrorMessage(this, e, "gen.internalError");
            close();
        }
    }

    @Override
    public void onClose() {
        removeListeners();
    }

    private void addListeners() {
        documentListener = doc -> onSearch();
        document.addListener(documentListener);
    }

    private void removeListeners() {
        document.removeListener(documentListener);
    }

    private void addComponents() throws ServiceException {
        setLayout(new BorderLayout());
        add(createSearchCriteriaPanel(), BorderLayout.NORTH);
        add(createSearchResultPanel(), BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.EAST);
        setDefaultButton(btSearch);
    }

    private JPanel createSearchCriteriaPanel() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("invoicesView.filter"));

        ifc.addField("gen.filterCriterion", searchCriterionModel);
        ifc.addField("invoicesView.includePaidInvoices", includePaidInvoicesModel);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        btSearch = widgetFactory.createButton("gen.btnSearch", this::onSearch);

        buttonPanel.add(btSearch);
        ifc.add(buttonPanel, SwingUtils.createGBConstraints(0, 10, 2, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, 5, 0, 0, 0));
        return ifc;
    }

    private JPanel createSearchResultPanel() throws ServiceException {
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(new CompoundBorder(new TitledBorder(textResource.getString("invoicesView.foundInvoices")),
                new EmptyBorder(5, 12, 5, 12)));

        invoicesTableModel = buildInvoiceOverviewTableModel();
        table = Tables.createSortedTable(invoicesTableModel);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        Tables.onSelectionChange(table, this::onSelectionChanged);
        resultPanel.add(widgetFactory.createScrollPane(table), BorderLayout.CENTER);

        return resultPanel;
    }

    private Component createButtonPanel() {
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.TOP, SwingConstants.VERTICAL);
        buttonPanel.addButton(editSelectedInvoiceAction);
        buttonPanel.addButton(emailSelectedInvoicesAction);
        buttonPanel.addButton(exportPdfsSelectedInvoicesAction);
        buttonPanel.addButton(printSelectedInvoicesAction);

        try {
            if (configurationService.getBookkeeping(document).isEnableAutomaticCollection()) {
                buttonPanel.addButton(generateAutomaticCollectionFileAction);
            }
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.internalError");
        }
        return buttonPanel;
    }

    private void showDetailResultPanel(InvoiceOverview selectedInvoice) {
        invoiceDetailsPanel = new CloseableJPanel();
        invoiceDetailsPanel.setBorder(widgetFactory.createTitleBorderWithPadding("invoicesView.invoiceDetails"));
        invoiceDetailsPanel.setLayout(new GridBagLayout());
        InputFieldsColumn ifc = new InputFieldsColumn();
        ifc.addReadonlyField("gen.id", new StringModel(selectedInvoice.getId()));
        ifc.addReadonlyField("gen.description", new StringModel(selectedInvoice.getDescription()));
        ifc.addReadonlyField("gen.issueDate", new StringModel(textResource.formatDate("gen.dateFormat", selectedInvoice.getIssueDate())));
        ifc.addReadonlyField("gen.party", new StringModel(selectedInvoice.getPayingPartyId() + " - " + selectedInvoice.getPayingPartyName()));

        Amount amountToBePaidOrReceived;
        Amount amountPaidOrReceived;
        Amount saldo;
        String amountToBePaidOrReceivedId;
        String amountPaidOrReceivedId;
        if (selectedInvoice.getAmountToBePaid().isNegative()) {
            amountToBePaidOrReceived = selectedInvoice.getAmountToBePaid().negate();
            amountPaidOrReceived = selectedInvoice.getAmountPaid().negate();
            saldo = amountToBePaidOrReceived.subtract(amountPaidOrReceived);
            amountToBePaidOrReceivedId = "gen.amountToBePaid";
            amountPaidOrReceivedId = "gen.amountPaid";
        } else {
            amountToBePaidOrReceived = selectedInvoice.getAmountToBePaid();
            amountPaidOrReceived = selectedInvoice.getAmountPaid();
            saldo = amountToBePaidOrReceived.subtract(amountPaidOrReceived);
            amountToBePaidOrReceivedId = "gen.amountToBeReceived";
            amountPaidOrReceivedId = "gen.amountReceived";
        }

        ifc.addReadonlyField(amountToBePaidOrReceivedId, new StringModel(amountFormat.formatAmountWithoutCurrency(amountToBePaidOrReceived.toBigInteger())));
        ifc.addReadonlyField(amountPaidOrReceivedId, new StringModel(amountFormat.formatAmountWithoutCurrency(amountPaidOrReceived.toBigInteger())));
        ifc.addReadonlyField("gen.saldo", new StringModel(amountFormat.formatAmountWithoutCurrency(saldo.toBigInteger())));

        invoiceDetailsPanel.add(ifc, SwingUtils.createGBConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 0, 0));

        try {
            JTable detailsTable = Tables.createSortedTable(buildDetailTableModel(selectedInvoice, invoiceService.findDetails(document, selectedInvoice)));
            JPanel detailsWithHeaderTable = Tables.createNonScrollableTablePanel(detailsTable);
            detailsWithHeaderTable.setBorder(widgetFactory.createTitleBorder("invoicesView.invoiceLines"));
            invoiceDetailsPanel.add(detailsWithHeaderTable,
                    SwingUtils.createGBConstraints(1, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 10, 0, 0, 0));
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, "gen.internalError", e);
        }

        try {
            JTable paymentsTable = Tables.createSortedTable(buildPaymentsTableModel(selectedInvoice, invoiceService.findPayments(document, selectedInvoice)));
            JPanel detailsWithHeaderTable = Tables.createNonScrollableTablePanel(paymentsTable);
            detailsWithHeaderTable.setBorder(widgetFactory.createTitleBorder("invoicesView.payments"));
            invoiceDetailsPanel.add(detailsWithHeaderTable,
                    SwingUtils.createGBConstraints(2, 0, 1, 1, 1, 0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, 10, 0, 0, 0));
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, "gen.internalError", e);
        }

        add(invoiceDetailsPanel, BorderLayout.SOUTH);
    }

    private void hideDetailsResultPanel() {
        if (invoiceDetailsPanel != null) {
            remove(invoiceDetailsPanel);
            invoiceDetailsPanel.close();
            invoiceDetailsPanel = null;
        }
    }

    private void onSelectionChanged() {
        emailSelectedInvoicesAction.setEnabled(Tables.getSelectedRowsConvertedToModel(table).length > 0);
        exportPdfsSelectedInvoicesAction.setEnabled(Tables.getSelectedRowsConvertedToModel(table).length > 0);
        printSelectedInvoicesAction.setEnabled(Tables.getSelectedRowsConvertedToModel(table).length > 0);
        generateAutomaticCollectionFileAction.setEnabled(Tables.getSelectedRowsConvertedToModel(table).length > 0);
        boolean exactlyOneInvoiceSelected = Tables.getSelectedRowsConvertedToModel(table).length == 1;
        editSelectedInvoiceAction.setEnabled(exactlyOneInvoiceSelected);
        hideDetailsResultPanel();
        if (exactlyOneInvoiceSelected) {
            showDetailResultPanel(invoicesTableModel.getRow(Tables.getSelectedRowConvertedToModel(table)));
        }
        repaint();
        revalidate();
    }

    private void onSearch() {
        try {
            Criterion criterion = isNullOrEmpty(searchCriterionModel.getString()) ? null : new Parser().parse(searchCriterionModel.getString());
            List<InvoiceOverview> matchingInvoices = invoiceService.findInvoiceOverviews(document, criterion, includePaidInvoicesModel.getBoolean());
            invoicesTableModel.setRows(matchingInvoices);
            Tables.selectFirstRow(table);
            table.requestFocusInWindow();
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
        }
    }

    private ListTableModel<InvoiceOverview> buildInvoiceOverviewTableModel() throws ServiceException {
        boolean automaticCollectionEnabled = configurationService.getBookkeeping(document).isEnableAutomaticCollection();
        Map<String, PartyAutomaticCollectionSettings> partyIdToPartyAutomaticCollectionsSettings = !automaticCollectionEnabled ? null :
            automaticCollectionService.findSettingsForAllParties(document).stream()
                    .collect(toMap(PartyAutomaticCollectionSettings::getPartyId, settings -> settings));

        List<ColumnDefinition<InvoiceOverview>> columnDefinitions = new ArrayList<>();
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("gen.id", String.class, 40)
                        .add(Invoice::getId).build());
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("gen.description", String.class, 200)
                        .add(Invoice::getDescription)
                        .build());
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("gen.issueDate", Date.class, 80)
                        .add(Invoice::getIssueDate)
                        .build());
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("gen.party", String.class, 200)
                        .add(row -> row.getPayingPartyId() + " - " + row.getPayingPartyName())
                        .build());
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("gen.emailAddress", String.class, 100)
                        .add(InvoiceOverview::getPayingPartyEmailAddress)
                        .build());
        if (automaticCollectionEnabled) {
            columnDefinitions.add(
                    ColumnDefinition.<InvoiceOverview>builder("gen.Iban", String.class, 100)
                            .add(invoice -> partyIdToPartyAutomaticCollectionsSettings.get(invoice.getPayingPartyId()) != null ?
                                    partyIdToPartyAutomaticCollectionsSettings.get(invoice.getPayingPartyId()).getIban() : null)
                            .build());
        }
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("gen.remarks", String.class, 100)
                        .add(InvoiceOverview::getPayingPartyRemarks)
                        .add(new TooltipCellRenderer())
                        .build());
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("gen.amountToBeReceived", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add((invoiceOverview) -> invoiceOverview.isSalesInvoice() ? invoiceOverview.getAmountToBePaid() : null)
                        .build());
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("gen.amountReceived", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add((invoiceOverview) -> invoiceOverview.isSalesInvoice() ? invoiceOverview.getAmountPaid() : null)
                        .build());
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("gen.amountToBePaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add((invoiceOverview) -> !invoiceOverview.isSalesInvoice() ? invoiceOverview.getAmountToBePaid().negate() : null)
                        .build());
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("gen.amountPaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add((invoiceOverview) -> !invoiceOverview.isSalesInvoice() ? invoiceOverview.getAmountPaid().negate() : null)
                        .build());
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("InvoicesView.lastSendingDate", Date.class, 80)
                        .add(invoiceOverview -> invoiceOverview.getLastSending() != null ? invoiceOverview.getLastSending().getDate() : null)
                        .build());
        columnDefinitions.add(
                ColumnDefinition.<InvoiceOverview>builder("InvoicesView.lastSendingType", String.class, 80)
                        .add(invoiceOverview -> invoiceOverview.getLastSending() != null ? textResource.getString(invoiceOverview.getLastSending().getType()) : null)
                        .build());
        return  new ListTableModel<>(columnDefinitions);
    }

    private void onEditSelectedInvoice() {
        int rowIndex = Tables.getSelectedRowConvertedToModel(table);
        InvoiceOverview selectedInvoice = invoicesTableModel.getRow(rowIndex);
        editInvoiceController.setOwner(this);
        editInvoiceController.setInvoiceToBeEdited(selectedInvoice);
        editInvoiceController.execute();
    }

    private void onExportPdfs() {
        sendSelectedInvoices((ExportPdfsInvoicesView) viewFactory.createView(ExportPdfsInvoicesView.class));
    }

    private void onEmail() {
        sendSelectedInvoices((EmailInvoicesView) viewFactory.createView(EmailInvoicesView.class));
    }

    private void onPrint() {
        sendSelectedInvoices((PrintInvoicesView) viewFactory.createView(PrintInvoicesView.class));
    }

    private void sendSelectedInvoices(SendInvoicesView sendInvoicesView) {
        try {
            List<Invoice> selectedInvoices = getSelectedInvoices();
            Map<String, Party> idToParty = partyService.getIdToParty(document, selectedInvoices.stream().map(Invoice::getConcerningPartyId).collect(toList()));
            Map<String, Party> invoiceIdToParty = selectedInvoices.stream().collect(toMap(Invoice::getId, i -> idToParty.get(i.getConcerningPartyId())));
            DefaultValueMap<String, List<InvoiceDetail>> invoiceIdToDetails = invoiceService.getIdToInvoiceDetails(document, selectedInvoices.stream().map(Invoice::getId).collect(toList()));
            DefaultValueMap<String, List<Payment>> invoiceIdToPayments = invoiceService.getIdToPayments(document, selectedInvoices.stream().map(Invoice::getId).collect(toList()));
            sendInvoicesView.setInvoicesToSend(selectedInvoices, invoiceIdToDetails, invoiceIdToPayments, invoiceIdToParty);
            new ViewDialog(getViewOwner().getWindow(), sendInvoicesView).showDialog();
        } catch (Exception e) {
            MessageDialog.showErrorMessage(this, "gen.internalError", e);
        }
    }

    private void onGenerateAutomaticCollectionFile() {
        try {
            GenerateAutomaticCollectionFileView view = (GenerateAutomaticCollectionFileView) viewFactory.createView(GenerateAutomaticCollectionFileView.class);
            List<Invoice> selectedInvoices = getSelectedInvoices();
            view.setSelectedInvoices(selectedInvoices);
            new ViewDialog(getViewOwner().getWindow(), view).showDialog();
        } catch (Exception e) {
            MessageDialog.showErrorMessage(this, "gen.internalError", e);
        }
    }

    private List<Invoice> getSelectedInvoices() {
        List<Invoice> selectedInvoices = new ArrayList<>();
        for (int rowIndex : Tables.getSelectedRowsConvertedToModel(table)) {
            selectedInvoices.add(invoicesTableModel.getRow(rowIndex));
        }
        return selectedInvoices;
    }

    private ListTableModel<InvoiceDetail> buildDetailTableModel(InvoiceOverview invoice, List<InvoiceDetail> details) {
        ListTableModel<InvoiceDetail> tableModel = new ListTableModel<>(asList(
                ColumnDefinition.<InvoiceDetail>builder("gen.date", Date.class, 200)
                        .add(invoiceDetail -> invoice.getIssueDate())
                        .build(),
                ColumnDefinition.<InvoiceDetail>builder("gen.description", String.class, 200)
                        .add(InvoiceDetail::getDescription)
                        .build(),
                ColumnDefinition.<InvoiceDetail>builder("gen.amountToBePaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add(invoiceDetail -> invoice.isSalesInvoice() ? invoiceDetail.getAmount() : invoiceDetail.getAmount().negate())
                        .build()
        ));
        tableModel.setRows(details);
        return tableModel;
    }

    private ListTableModel<Payment> buildPaymentsTableModel(InvoiceOverview invoice, List<Payment> payments) {
        ListTableModel<Payment> tableModel = new ListTableModel<>(asList(
                ColumnDefinition.<Payment>builder("gen.date", Date.class, 200)
                        .add(Payment::getDate)
                        .build(),
                ColumnDefinition.<Payment>builder("gen.description", String.class, 200)
                        .add(Payment::getDescription)
                        .build(),
                ColumnDefinition.<Payment>builder("gen.amountPaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add(payment -> invoice.isSalesInvoice() ? payment.getAmount() : payment.getAmount().negate())
                        .build()
        ));
        tableModel.setRows(payments);
        return tableModel;
    }
}
