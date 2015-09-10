package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.businessobjects.Report;
import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.components.document.DocumentListener;
import nl.gogognome.gogoaccount.gui.components.AccountFormatter;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.gui.beans.InputFieldsRow;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.AbstractModel;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.views.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Date;

/**
 * This view shows all mutations for an account.
 */
public class AccountMutationsView extends View {

	private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(AccountMutationsView.class);

	private Document document;

	private JScrollPane tableScrollPane;
	private AccountOverviewTableModel tableModel;

	private DateModel dateModel = new DateModel(new Date());
	private ListModel<Account> accountListModel = new ListModel<>();

	private ModelChangeListener modelListener;
	private DocumentListener documentListener;

	private Report report;

	public AccountMutationsView(Document document) {
		super();
		this.document = document;
	}

	@Override
	public String getTitle() {
		return textResource.getString("AccountMutationsView.title");
	}

	@Override
	public void onInit() {
		try {
            initModels();
            addComponents();
            addListeners();
            updateTableModel();

            if (!accountListModel.getItems().isEmpty()) {
                accountListModel.setSelectedIndex(0, null);
            }
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            close();
        }
	}

	private void initModels() {
		tableModel = new AccountOverviewTableModel();
		setAccountsInListModel();
	}

	private void addComponents() {
		JPanel northPanel = createInputFieldsPanel();
		northPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0));

		JTable table = widgetFactory.createSortedTable(tableModel);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		tableScrollPane = widgetFactory.createScrollPane(table);

		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BorderLayout());
		add(northPanel, BorderLayout.NORTH);
		add(tableScrollPane, BorderLayout.CENTER);
	}

	private JPanel createInputFieldsPanel() {
		InputFieldsRow row = new InputFieldsRow();
		addCloseable(row);

		row.addComboBoxField("AccountMutationsView.account", accountListModel,
				new AccountFormatter());
		row.addField("AccountMutationsView.date", dateModel);

		return row;
	}

	@Override
	public void onClose() {
		removeListeners();
	}

	private void addListeners() {
		modelListener = new ModelChangeListenerImpl();
		dateModel.addModelChangeListener(modelListener);
		accountListModel.addModelChangeListener(modelListener);

		documentListener = new DocumentListenerImpl();
		document.addListener(documentListener);
	}

	private void removeListeners() {
		document.removeListener(documentListener);
		dateModel.removeModelChangeListener(modelListener);
		accountListModel.removeModelChangeListener(modelListener);
	}

	private void updateReportAndTableModel() throws ServiceException {
		Date date = dateModel.getDate();
		if (date != null) {
			updateReport(date);
		} else {
			report = null;
		}

		updateTableModel();
	}

	private void updateReport(Date date) {
		try {
			report = new BookkeepingService().createReport(document, date);
		} catch (ServiceException e) {
			report = null;
			MessageDialog.showErrorMessage(this, e, "gen.internalError");
		}
	}

	private void updateTableModel() throws ServiceException {
		Account account = accountListModel.getSelectedItem();
		tableModel.setAccountAndDate(document, report, account);

		if (account != null && report != null) {
			tableScrollPane.setBorder(widgetFactory.createTitleBorder("vao.accountAtDate",
			        account.getId() + " - " + account.getName(),
			        textResource.formatDate("gen.dateFormat", report.getEndDate())));
		} else {
			tableModel.clear();
			tableScrollPane.setBorder(widgetFactory.createTitleBorder("AccountMutationsView.initialTableTitle"));
		}
	}

	private void setAccountsInListModel() {
        try {
            accountListModel.setItems(ObjectFactory.create(ConfigurationService.class).findAllAccounts(document));
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
        }
    }

	private final class ModelChangeListenerImpl implements ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
            try {
                if (model == accountListModel && report != null) {
                    updateTableModel();
                } else {
                    updateReportAndTableModel();
                }
            } catch (ServiceException e ) {
                logger.warn("ignored exception: " + e.getMessage(), e);
            }
		}
	}

	private final class DocumentListenerImpl implements DocumentListener {
		@Override
		public void documentChanged(Document document) {
            try {
                setAccountsInListModel();
                updateReportAndTableModel();
            } catch (ServiceException e) {
                logger.warn("ignored exception: " + e.getMessage(), e);
            }
		}
	}
}
