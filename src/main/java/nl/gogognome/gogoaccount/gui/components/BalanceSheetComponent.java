package nl.gogognome.gogoaccount.gui.components;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentListener;
import nl.gogognome.gogoaccount.gui.components.BalanceSheet.Row;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.Closeable;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.text.*;
import nl.gogognome.lib.util.Factory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class implements a graphical component that shows a balance sheet.
 */
public class BalanceSheetComponent extends JScrollPane implements Closeable {

	@Serial
	private static final long serialVersionUID = 1L;

    private final Document document;
    private final BookkeepingService bookkeepingService;
    private final DateModel dateModel;
    private final BalanceSheet balanceSheet;

    private Report report;

    private DocumentListener documentListener;
    private ModelChangeListener modelChangeListener;

    private final TextResource textResource = Factory.getInstance(TextResource.class);
    private final MessageDialog messageDialog;

    /**
     * Creates a new <code>BalanceComponent</code>.
     * @param document the datebase used to create the balance
     * @param bookkeepingService the bookkeepingService
     * @param dateModel determines the date of the balance
     */
    public BalanceSheetComponent(Document document, BookkeepingService bookkeepingService, DateModel dateModel) throws ServiceException {
        this.document = document;
        this.bookkeepingService = bookkeepingService;
        this.dateModel = dateModel;
        messageDialog = new MessageDialog(textResource, this);

        balanceSheet = new BalanceSheet(textResource.getString("gen.assets"), textResource.getString("gen.liabilities"));
        balanceSheet.setOpaque(false);
        balanceSheet.setBorder(new EmptyBorder(10, 10, 10, 10));
        setViewportView(balanceSheet);

        initComponents();
        addListeners();
    }

    private void addListeners() {
        documentListener = new DocumentListenerImpl();
        document.addListener(documentListener);

        modelChangeListener = new ModelChangeListenerImpl();
        dateModel.addModelChangeListener(modelChangeListener);
    }

    private void removeListeners() {
    	dateModel.removeModelChangeListener(modelChangeListener);
    	document.removeListener(documentListener);
    }

    private void initComponents() {
        Date date = dateModel.getDate();
        if (date == null) {
            return; // do not change the current balance if the date is invalid
        }

        try {
			report = bookkeepingService.createReport(document, date);

            setBorder(Factory.getInstance(WidgetFactory.class)
                    .createTitleBorder("balanceSheetComponent.title", report.getEndDate()));

            List<Row> leftRows = convertAccountsToRows(report.getAssetsInclLossAccount());
            List<Row> rightRows = convertAccountsToRows(report.getLiabilitiesInclProfitAccount());

            balanceSheet.setLeftRows(leftRows);
            balanceSheet.setRightRows(rightRows);
            balanceSheet.update();

            int row = 5 + Math.max(leftRows.size(), rightRows.size());

			if (report.totalDebitAccountsDiffersFromTotalDebtors() || report.totalDebitAccountsDiffersFromTotalCreditors()) {
				for (int i=1; i<=2; i++) {
					balanceSheet.add(new JLabel(textResource.getString("balanceSheetComponent.differenceFoundBetweenBalanceSheetAndInvoices_" + i)),
							SwingUtils.createGBConstraints(0, row, 4, 1, 0.0, 0.0,
									GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 0, 0, 0));
					row++;
				}
			}

			if (report.totalDebitAccountsDiffersFromTotalDebtors()) {
				balanceSheet.add(new JLabel(textResource.getString("balanceSheetComponent.totalDebtors")),
						SwingUtils.createGBConstraints(0, row, 2, 1, 0.0, 0.0,
								GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 0, 0, 0));
				balanceSheet.add(new JLabel(Factory.getInstance(AmountFormat.class).formatAmount(report.getTotalDebtors().toBigInteger())),
						SwingUtils.createGBConstraints(2, row, 2, 1, 0.0, 0.0,
								GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 0, 0, 0));
				row++;
			}

			if (report.totalDebitAccountsDiffersFromTotalCreditors()) {
				balanceSheet.add(new JLabel(textResource.getString("balanceSheetComponent.totalCreditors")),
						SwingUtils.createGBConstraints(0, row, 2, 1, 1.0, 0.0,
								GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 0, 0, 0));
				balanceSheet.add(new JLabel(Factory.getInstance(AmountFormat.class).formatAmount(report.getTotalCreditors().toBigInteger())),
						SwingUtils.createGBConstraints(2, row, 2, 1, 1.0, 0.0,
								GridBagConstraints.WEST, GridBagConstraints.BOTH, 0, 0, 0, 0));
				row++;
			}
		} catch (ServiceException e) {
			messageDialog.showErrorMessage(e, "gen.internalError");
            close();
		}
    }

    private List<Row> convertAccountsToRows(List<Account> accounts) {
        List<Row> rows = new ArrayList<>();

        for (Account a : accounts) {
        	Row row = new Row();
        	row.description = a.isResultOfOperations() ? a.getName() : a.getId() + ' ' + a.getName();
            row.amount = report.getAmount(a);
            rows.add(row);
        }

        return rows;
    }

    /**
     * Sets the background color.
     * @param color the background color
     */
    @Override
    public void setBackground(Color color) {
        super.setBackground(color);
        getViewport().setBackground(color);
        if (balanceSheet != null) {
            balanceSheet.setBackground(color);
        }
    }

    @Override
    public void close() {
    	removeListeners();
    }

    private final class ModelChangeListenerImpl implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
		    if (((DateModel)(model)).getDate() != null) {
		        initComponents();
		        validate();
		    }
		}
	}

	private final class DocumentListenerImpl implements DocumentListener {
		@Override
		public void documentChanged(Document document) {
		    initComponents();
		    validate();
		}
	}
}
