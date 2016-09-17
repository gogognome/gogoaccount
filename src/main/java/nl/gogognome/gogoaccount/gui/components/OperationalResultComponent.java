package nl.gogognome.gogoaccount.gui.components;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentListener;
import nl.gogognome.gogoaccount.gui.components.BalanceSheet.Row;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.Closeable;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class implements a graphical component that shows an operational result.
 */
public class OperationalResultComponent extends JScrollPane implements Closeable {

	private static final long serialVersionUID = 1L;

    private final Document document;
    private final BookkeepingService bookkeepingService;
    private final DateModel dateModel;
    private final BalanceSheet balanceSheet;

    private Report report;

    private DocumentListener documentListener;
    private ModelChangeListener modelChangeListener;

    private final WidgetFactory widgetFactory = Factory.getInstance(WidgetFactory.class);

    public OperationalResultComponent(Document document, BookkeepingService bookkeepingService, DateModel dateModel) throws ServiceException {
        super();
        this.document = document;
        this.bookkeepingService = bookkeepingService;
        this.dateModel = dateModel;

        TextResource textResource = Factory.getInstance(TextResource.class);
        balanceSheet = new BalanceSheet(textResource.getString("gen.expenses"), textResource.getString("gen.revenues"));
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

    @Override
    public void close() {
    	removeListeners();
    }

    private void removeListeners() {
    	dateModel.removeModelChangeListener(modelChangeListener);
    	document.removeListener(documentListener);
    }

    private void initComponents() {
        Date date = dateModel.getDate();
        if (date == null) {
            return; // do not change the current operational result if the date is invalid
        }

        try {
			report = bookkeepingService.createReport(document, date);

            setBorder(widgetFactory.createTitleBorder("operationalResultComponent.title",
                    report.getEndDate()));

            List<Row> leftRows = convertAccountsToRows(report.getExpenses());
            List<Row> rightRows = convertAccountsToRows(report.getRevenues());

            balanceSheet.setLeftRows(leftRows);
            balanceSheet.setRightRows(rightRows);
            balanceSheet.update();
		} catch (ServiceException e) {
			MessageDialog.showErrorMessage(this, e, "gen.internalError");
            close();
		}
    }

    private List<Row> convertAccountsToRows(List<Account> accounts) {
        List<Row> rows = new ArrayList<>();

        for (Account a : accounts) {
        	Row row = new Row();
        	row.description = a.getId() + ' ' + a.getName();
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
