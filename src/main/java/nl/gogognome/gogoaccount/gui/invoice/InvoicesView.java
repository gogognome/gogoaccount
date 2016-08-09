package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceOverview;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.criteria.Parser;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;

import static nl.gogognome.lib.util.StringUtil.isNullOrEmpty;

public class InvoicesView extends View {

    private final Document document;

    private final InvoiceOverviewTableModel invoicesTableModel;
    private final InvoiceService invoiceService;

    private StringModel searchCriterionModel = new StringModel();

    private JTable table;
    private JButton btSearch;

    public InvoicesView(Document document, InvoiceOverviewTableModel invoicesTableModel, InvoiceService invoiceService) {
        this.document = document;
        this.invoicesTableModel = invoicesTableModel;
        this.invoiceService = invoiceService;
    }

    @Override
    public String getTitle() {
        return textResource.getString("invoicesView.title");
    }

    @Override
    public void onInit() {
        addComponents();
    }

    @Override
    public void onClose() {
    }

    private void addComponents() {
        setLayout(new GridBagLayout());
        add(createSearchCriteriaAndResultsPanel(), SwingUtils.createGBConstraints(0, 0, 1, 1, 1.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH, 12, 12, 12, 12));
        setDefaultButton(btSearch);
    }

    private JPanel createSearchCriteriaAndResultsPanel() {
        JPanel result = new JPanel(new BorderLayout());
        result.add(createSearchCriteriaPanel(), BorderLayout.NORTH);
        result.add(createSearchResultPanel(), BorderLayout.CENTER);

        return result;
    }

    private JPanel createSearchCriteriaPanel() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("invoicesView.filter"));

        ifc.addField("gen.filterCriterion", searchCriterionModel);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        btSearch = widgetFactory.createButton("gen.btnSearch", () -> onSearch());

        buttonPanel.add(btSearch);
        ifc.add(buttonPanel, SwingUtils.createGBConstraints(0, 10, 2, 1, 0.0, 0.0,
                GridBagConstraints.EAST, GridBagConstraints.NONE, 5, 0, 0, 0));
        return ifc;
    }

    private JPanel createSearchResultPanel() {
        JPanel resultPanel = new JPanel(new BorderLayout());
        resultPanel.setBorder(new CompoundBorder(new TitledBorder(textResource.getString("invoicesView.foundInvoices")),
                new EmptyBorder(5, 12, 5, 12)));

        table = widgetFactory.createSortedTable(invoicesTableModel);
        table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        resultPanel.add(widgetFactory.createScrollPane(table), BorderLayout.CENTER);

        return resultPanel;
    }

    private void onSearch() {
        try {
            Criterion criterion = isNullOrEmpty(searchCriterionModel.getString()) ? null : new Parser().parse(searchCriterionModel.getString());
            List<InvoiceOverview> matchingInvoices = invoiceService.findInvoiceOverviews(document, criterion, true);
            invoicesTableModel.replaceRows(matchingInvoices);
            SwingUtils.selectFirstRow(table);
            table.requestFocusInWindow();
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
        }
    }
}
