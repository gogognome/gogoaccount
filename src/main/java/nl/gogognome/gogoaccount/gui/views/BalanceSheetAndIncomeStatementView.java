package nl.gogognome.gogoaccount.gui.views;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.gui.components.BalanceSheetComponent;
import nl.gogognome.gogoaccount.gui.components.IncomeStatementComponent;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsRow;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.dialogs.MessageDialog;

/**
 * This class implements a view the shows a balance sheet and income statement.
 */
public class BalanceSheetAndIncomeStatementView extends View {

	private static final long serialVersionUID = 1L;

	private final static Color BACKGROUND_COLOR = new Color(255, 255, 209);

    private final Document document;
    private final BookkeepingService bookkeepingService;
    private final MessageDialog messageDialog;
    private DateModel dateModel;

    public BalanceSheetAndIncomeStatementView(Document document, BookkeepingService bookkeepingService) {
        this.document = document;
        this.bookkeepingService = bookkeepingService;
        messageDialog = new MessageDialog(textResource, this);
    }

    @Override
	public String getTitle() {
        return textResource.getString("balanceSheetAndIncomeStatementView.title");
    }

    @Override
	public void onInit() {
        try {
            initModels();
            addComponents();
        } catch (ServiceException e) {
            messageDialog.showErrorMessage(e, "gen.problemOccurred");
            close();
        }
    }

    @Override
    public void onClose() {
    }

	private void initModels() {
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
	}

	private void addComponents() throws ServiceException {
		setLayout(new BorderLayout());
        setBackground(BACKGROUND_COLOR);
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel northPanel = createInputFieldsPanel();
		northPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 10, 0));

        add(northPanel, BorderLayout.NORTH);
        add(createBalanceSheetAndIncomeStatementPanel(), BorderLayout.CENTER);
	}

    private JPanel createInputFieldsPanel() {
		InputFieldsRow row = new InputFieldsRow();
		addCloseable(row);
		row.addField("balanceSheetAndIncomeStatementView.selectDate", dateModel);
		return row;
	}

    private JPanel createBalanceSheetAndIncomeStatementPanel() throws ServiceException {
        JPanel panel = new JPanel(new GridBagLayout());

        panel.setBackground(BACKGROUND_COLOR);
        BalanceSheetComponent balanceSheetComponent = new BalanceSheetComponent(document, bookkeepingService, dateModel);
        addCloseable(balanceSheetComponent);
        balanceSheetComponent.setBackground(BACKGROUND_COLOR);
        panel.add(balanceSheetComponent, createConstraints(0, 0));

        IncomeStatementComponent incomeStatementComponent =
            new IncomeStatementComponent(document, bookkeepingService, dateModel);
        addCloseable(incomeStatementComponent);
        incomeStatementComponent.setBackground(BACKGROUND_COLOR);
        panel.add(incomeStatementComponent, createConstraints(0, 1));

        return panel;
    }

    public static GridBagConstraints createConstraints( int gridx, int gridy) {
    	return SwingUtils.createGBConstraints(gridx, gridy, 1, 1, 1.0, 1.0,
    			GridBagConstraints.WEST, GridBagConstraints.BOTH,
    			gridy == 0 ? 0 : 10, 0, 0, 0);
    }
}
