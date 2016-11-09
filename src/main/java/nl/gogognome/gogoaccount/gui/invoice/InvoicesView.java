package nl.gogognome.gogoaccount.gui.invoice;

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
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static nl.gogognome.lib.util.StringUtil.isNullOrEmpty;

public class InvoicesView extends View {

    private final Document document;
    private final AmountFormat amountFormat;

    private final InvoiceService invoiceService;
    private final PartyService partyService;
    private final EditInvoiceController editInvoiceController;
    private final ViewFactory viewFactory;

    private StringModel searchCriterionModel = new StringModel();
    private BooleanModel includePaidInvoicesModel = new BooleanModel();
    private ActionWrapper editSelectedInvoiceAction = widgetFactory.createActionWrapper("InvoicesView.edit", this::onEditSelectedInvoice);
    private ActionWrapper printSelectedInvoicesAction = widgetFactory.createActionWrapper("InvoicesView.sendInvoices", this::onPrintSelectedInvoices);
    private DocumentListener documentListener;

    private JTable table;
    private ListTableModel<InvoiceOverview> invoicesTableModel;
    private JButton btSearch;
    private CloseableJPanel invoiceDetailsPanel;

    public InvoicesView(Document document, AmountFormat amountFormat, InvoiceService invoiceService, PartyService partyService, EditInvoiceController editInvoiceController, ViewFactory viewFactory) {
        this.document = document;
        this.amountFormat = amountFormat;
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
        addComponents();
        onSearch();
        addListeners();
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

    private void addComponents() {
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

    private JPanel createSearchResultPanel() {
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
        buttonPanel.addButton(printSelectedInvoicesAction);
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
        ifc.addReadonlyField("gen.amountToBePaid", new StringModel(amountFormat.formatAmountWithoutCurrency(selectedInvoice.getAmountToBePaid().toBigInteger())));
        invoiceDetailsPanel.add(ifc, SwingUtils.createGBConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, 0, 0, 0, 0));

        try {
            JTable detailsTable = Tables.createSortedTable(buildDetailTableModel(invoiceService.findDetails(document, selectedInvoice)));
            JPanel detailsWithHeaderTable = Tables.createNonScrollableTablePanel(detailsTable);
            detailsWithHeaderTable.setBorder(widgetFactory.createTitleBorder("invoicesView.invoiceLines"));
            invoiceDetailsPanel.add(detailsWithHeaderTable,
                    SwingUtils.createGBConstraints(0, 1, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 10, 0, 0, 0));
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, "gen.internalError", e);
        }

        try {
            JTable paymentsTable = Tables.createSortedTable(buildPaymentsTableModel(invoiceService.findPayments(document, selectedInvoice)));
            JPanel detailsWithHeaderTable = Tables.createNonScrollableTablePanel(paymentsTable);
            detailsWithHeaderTable.setBorder(widgetFactory.createTitleBorder("invoicesView.payments"));
            invoiceDetailsPanel.add(detailsWithHeaderTable,
                    SwingUtils.createGBConstraints(0, 2, 1, 1, 1, 0, GridBagConstraints.WEST, GridBagConstraints.NONE, 10, 0, 0, 0));
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
        printSelectedInvoicesAction.setEnabled(Tables.getSelectedRowsConvertedToModel(table).length > 0);
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

    private ListTableModel<InvoiceOverview> buildInvoiceOverviewTableModel() {
        return new ListTableModel<>(asList(
                ColumnDefinition.<InvoiceOverview>builder("gen.id", String.class, 40)
                        .add(Invoice::getId)
                        .build(),
                ColumnDefinition.<InvoiceOverview>builder("gen.description", String.class, 200)
                        .add(Invoice::getDescription)
                        .build(),
                ColumnDefinition.<InvoiceOverview>builder("gen.issueDate", String.class, 100)
                        .add(Invoice::getIssueDate)
                        .build(),
                ColumnDefinition.<InvoiceOverview>builder("gen.party", String.class, 200)
                        .add(row -> row.getPayingPartyId() + " - " + row.getPayingPartyName())
                        .build(),
                ColumnDefinition.<InvoiceOverview>builder("gen.amountToBePaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add(InvoiceOverview::getAmountToBePaid)
                        .build(),
                ColumnDefinition.<InvoiceOverview>builder("gen.amountPaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add(InvoiceOverview::getAmountPaid)
                        .build()));
    }

    private void onEditSelectedInvoice() {
        int rowIndex = Tables.getSelectedRowConvertedToModel(table);
        InvoiceOverview selectedInvoice = invoicesTableModel.getRow(rowIndex);
        editInvoiceController.setOwner(this);
        editInvoiceController.setInvoiceToBeEdited(selectedInvoice);
        editInvoiceController.execute();
    }

    private void onPrintSelectedInvoices() throws ServiceException {
        SendInvoicesView sendInvoicesView = (SendInvoicesView) viewFactory.createView(SendInvoicesView.class);
        List<Invoice> selectedInvoices = getSelectedInvoices();
        Map<String, Party> idToParty = partyService.getIdToParty(document, selectedInvoices.stream().map(Invoice::getConcerningPartyId).collect(toList()));
        Map<String, Party> invoiceIdToParty = selectedInvoices.stream().collect(toMap(i -> i.getId(), i -> idToParty.get(i.getConcerningPartyId())));
        DefaultValueMap<String, List<InvoiceDetail>> invoiceIdToDetails = invoiceService.getIdToInvoiceDetails(document, selectedInvoices.stream().map(i -> i.getId()).collect(toList()));
        DefaultValueMap<String, List<Payment>> invoiceIdToPayments = invoiceService.getIdToPayments(document, selectedInvoices.stream().map(i -> i.getId()).collect(toList()));
        sendInvoicesView.setInvoicesToPrint(selectedInvoices, invoiceIdToDetails, invoiceIdToPayments, invoiceIdToParty);
        Dimension viewOwnerSize = getViewOwner().getWindow().getSize();
        sendInvoicesView.setMinimumSize(new Dimension((int) viewOwnerSize.getWidth() * 90 / 100, (int) viewOwnerSize.getHeight() * 90 / 100));
        new ViewDialog(getViewOwner().getWindow(), sendInvoicesView).showDialog();
    }

    private List<Invoice> getSelectedInvoices() {
        List<Invoice> selectedInvoices = new ArrayList<>();
        for (int rowIndex : Tables.getSelectedRowsConvertedToModel(table)) {
            selectedInvoices.add(invoicesTableModel.getRow(rowIndex));
        }
        return selectedInvoices;
    }

    private ListTableModel<InvoiceDetail> buildDetailTableModel(List<InvoiceDetail> details) {
        ListTableModel<InvoiceDetail> tableModel = new ListTableModel<>(asList(
                ColumnDefinition.<InvoiceDetail>builder("gen.description", String.class, 200)
                        .add(InvoiceDetail::getDescription)
                        .build(),
                ColumnDefinition.<InvoiceDetail>builder("gen.amountToBePaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add(InvoiceDetail::getAmount)
                        .build()
        ));
        tableModel.setRows(details);
        return tableModel;
    }

    private ListTableModel<Payment> buildPaymentsTableModel(List<Payment> payments) {
        ListTableModel<Payment> tableModel = new ListTableModel<>(asList(
                ColumnDefinition.<Payment>builder("gen.description", String.class, 200)
                        .add(Payment::getDescription)
                        .build(),
                ColumnDefinition.<Payment>builder("gen.amountPaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add(Payment::getAmount)
                        .build()
        ));
        tableModel.setRows(payments);
        return tableModel;
    }
}
