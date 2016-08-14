package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceDetail;
import nl.gogognome.gogoaccount.component.invoice.InvoiceOverview;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.gui.tablecellrenderer.AmountCellRenderer;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.*;
import nl.gogognome.lib.swing.models.BooleanModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.models.Tables;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.criteria.Parser;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.TableModel;
import java.awt.*;
import java.util.Date;
import java.util.List;

import static nl.gogognome.lib.util.StringUtil.isNullOrEmpty;

public class InvoicesView extends View {

    private final Document document;
    private final AmountFormat amountFormat;

    private final InvoiceService invoiceService;

    private StringModel searchCriterionModel = new StringModel();
    private BooleanModel includePaidInvoicesModel = new BooleanModel();

    private JTable table;
    private ListTableModel<InvoiceOverview> invoicesTableModel;
    private JButton btSearch;
    private CloseableJPanel invoiceDetailsPanel;

    public InvoicesView(Document document, AmountFormat amountFormat, InvoiceService invoiceService) {
        this.document = document;
        this.amountFormat = amountFormat;
        this.invoiceService = invoiceService;
    }

    @Override
    public String getTitle() {
        return textResource.getString("invoicesView.title");
    }

    @Override
    public void onInit() {
        addComponents();
        onSearch();
    }

    @Override
    public void onClose() {
    }

    private void addComponents() {
        setLayout(new BorderLayout());
        add(createSearchCriteriaPanel(), BorderLayout.NORTH);
        add(createSearchResultPanel(), BorderLayout.CENTER);
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
        Tables.onSelectionChange(table, () -> onSelectionChanged());
        resultPanel.add(widgetFactory.createScrollPane(table), BorderLayout.CENTER);

        return resultPanel;
    }

    private void showDetailResultPanel(InvoiceOverview selectedInvoice) {
        invoiceDetailsPanel = new CloseableJPanel();
        invoiceDetailsPanel.setBorder(widgetFactory.createTitleBorderWithPadding("invoicesView.invoiceDetails"));
        invoiceDetailsPanel.setLayout(new GridBagLayout());
        InputFieldsColumn ifc = new InputFieldsColumn();
        ifc.addReadonlyField("gen.id", new StringModel(selectedInvoice.getId()));
        ifc.addReadonlyField("gen.description", new StringModel(selectedInvoice.getDescription()));
        ifc.addReadonlyField("gen.issueDate", new StringModel(textResource.formatDate("gen.dateFormat", selectedInvoice.getIssueDate())));
        ifc.addReadonlyField("gen.party", new StringModel(selectedInvoice.getPartyId() + " - " + selectedInvoice.getPartyName()));
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
        hideDetailsResultPanel();
        int rowIndex = Tables.getSelectedRowConvertedToModel(table);
        if (rowIndex != -1) {
            InvoiceOverview selectedInvoice = invoicesTableModel.getRow(rowIndex);
            showDetailResultPanel(selectedInvoice);
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
        return new ListTableModel<InvoiceOverview>(
                ColumnDefinition.<InvoiceOverview>builder("gen.id", String.class, 40)
                        .add(row -> row.getId())
                        .build(),
                ColumnDefinition.<InvoiceOverview>builder("gen.description", String.class, 200)
                        .add(row -> row.getDescription())
                        .build(),
                ColumnDefinition.<InvoiceOverview>builder("gen.issueDate", String.class, 100)
                        .add(row -> row.getIssueDate())
                        .build(),
                ColumnDefinition.<InvoiceOverview>builder("gen.party", String.class, 200)
                        .add(row -> row.getPartyId() + " - " + row.getPartyName())
                        .build(),
                ColumnDefinition.<InvoiceOverview>builder("gen.amountToBePaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add(row -> row.getAmountToBePaid())
                        .build(),
                ColumnDefinition.<InvoiceOverview>builder("gen.amountePaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add(row -> row.getAmountPaid())
                        .build());
    }

    private ListTableModel<InvoiceDetail> buildDetailTableModel(List<InvoiceDetail> details) {
        ListTableModel<InvoiceDetail> tableModel = new ListTableModel<InvoiceDetail>(
                ColumnDefinition.<InvoiceDetail>builder("gen.description", String.class, 200)
                        .add(row -> row.getDescription())
                        .build(),
                ColumnDefinition.<InvoiceDetail>builder("gen.amountToBePaid", Amount.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add(row -> row.getAmount())
                        .build()
        );
        tableModel.setRows(details);
        return tableModel;
    }
}
