package nl.gogognome.gogoaccount.gui.invoice;

import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.beans.PartyBean;
import nl.gogognome.gogoaccount.gui.tablecellrenderer.AmountCellRenderer;
import nl.gogognome.gogoaccount.gui.views.EditDescriptionAndAmountView;
import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.gogoaccount.models.PartyModel;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.ListTableModel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.*;
import nl.gogognome.lib.swing.views.OkCancelView;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.lib.util.StringUtil;
import nl.gogognome.lib.util.Tuple;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * This class lets the user edit an existing invoice.
 */
public class EditInvoiceView extends OkCancelView {

	private static final long serialVersionUID = 1L;

    private final ConfigurationService configurationService = ObjectFactory.create(ConfigurationService.class);
    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

    private Document document;
    private Currency currency;

    private String titleId;

    private Invoice initialInvoice;

    private StringModel idModel = new StringModel();
    private StringModel amountModel = new StringModel();
    private DateModel dateModel = new DateModel();
    private StringModel descriptionModel = new StringModel();
    private PartyModel concerningPartyModel = new PartyModel();
    private PartyModel payingPartyModel = new PartyModel();

    private ModelChangeListener concerningPartyListener;

    private JTable table;
    private DescriptionAndAmountTableModel tableModel;

    private Invoice editedInvoice;
    private List<String> editedDescriptions;
    private List<Amount> editedAmounts;

    private AmountFormat amountFormat = Factory.getInstance(AmountFormat.class);

    /**
     * Constructor. To edit an existing invoice, give <code>invoice</code> a non-<code>null</code> value.
     * To create a new journal, set <code>invoice</code> to <code>null</code>.
     *
     * @param document the database to which the journal must be added
     * @param titleId the id of the title
     * @param invoice the invoice used to initialize the elements of the view. Must be <code>null</code>
     *        to edit a new invoice
     */
    public EditInvoiceView(Document document, String titleId, Invoice invoice) {
        this.document = document;
        this.titleId = titleId;
        this.initialInvoice = invoice;
    }

    @Override
    public String getTitle() {
        return textResource.getString(titleId);
    }

    @Override
    public void onClose() {
        concerningPartyModel.removeModelChangeListener(concerningPartyListener);
    }

    @Override
    public void onInit() {
        try {
            initModels();
            addComponents();
            addListeners();
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            close();
        }
    }

	private void initModels() throws ServiceException {
        Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
        currency = bookkeeping.getCurrency();

        if (initialInvoice != null) {
            idModel.setString(initialInvoice.getId());
            idModel.setEnabled(false, null);
            dateModel.setDate(initialInvoice.getIssueDate());
            descriptionModel.setString(initialInvoice.getDescription());
            concerningPartyModel.setParty(initialInvoice.getConcerningPartyId() != null ? partyService.getParty(document, initialInvoice.getConcerningPartyId()) : null);
            payingPartyModel.setParty(initialInvoice.getPayingPartyId() != null ? partyService.getParty(document, initialInvoice.getPayingPartyId()): null);
            amountModel.setString(amountFormat.formatAmountWithoutCurrency(
                initialInvoice.getAmountToBePaid().toBigInteger()));
        } else {
            dateModel.setDate(new Date());
            idModel.setString(invoiceService.suggestNewInvoiceId(document,
                    textResource.formatDate("editInvoiceView.dateFormatForNewId", dateModel.getDate())));
        }

	}

	private void addListeners() {
        concerningPartyListener = new CopyConceringPartyToPayingPartyListener();
        concerningPartyModel.addModelChangeListener(concerningPartyListener);
	}

    @Override
    protected JComponent createNorthComponent() {
        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);

        ifc.addField("gen.id", idModel);
        ifc.addField("gen.issueDate", dateModel);
        ifc.addField("gen.description", descriptionModel);
        ifc.addVariableSizeField("editInvoiceView.concerningParty", new PartyBean(document, concerningPartyModel));
        ifc.addVariableSizeField("editInvoiceView.payingParty", new PartyBean(document, payingPartyModel));
        ifc.addField("gen.amount", amountModel);

    	return ifc;
    }

    @Override
	protected JComponent createCenterComponent() {
        try {
            // Create panel with descriptions and amounts table.
            JPanel middlePanel = new JPanel(new BorderLayout());
            // TODO: replace by InvoiceDetail
            List<Tuple<String, Amount>> tuples = new ArrayList<>();
            if (initialInvoice != null) {
                List<String> descriptions = invoiceService.findDescriptions(document, initialInvoice);
                List<Amount> amounts = invoiceService.findAmounts(document, initialInvoice);
                for (int i = 0; i < descriptions.size(); i++) {
                    tuples.add(new Tuple<>(descriptions.get(i), amounts.get(i)));
                }
            }
            tableModel = new DescriptionAndAmountTableModel(amountFormat, tuples);
            table = Tables.createTable(tableModel);
            JScrollPane scrollPane = widgetFactory.createScrollPane(table);
            middlePanel.add(scrollPane, BorderLayout.CENTER);

            ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.TOP, SwingConstants.VERTICAL);
            buttonPanel.addButton("editInvoiceView.addRow", new AddAction());
            buttonPanel.addButton("editInvoiceView.editRow", new EditAction());
            buttonPanel.addButton("editInvoiceView.deleteRow", new DeleteAction());

            middlePanel.add(buttonPanel, BorderLayout.EAST);

            return middlePanel;
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the invoice as entered by the user.
     * @return the invoice as entered by the user or <code>null</code> if the user cancelled the view
     */
    public Invoice getEditedInvoice() {
        return editedInvoice;
    }

    public List<String> getEditedDescriptions() {
        return editedDescriptions;
    }

    public List<Amount> getEditedAmounts() {
        return editedAmounts;
    }

    /**
     * This method is called when the user wants to add a new row.
     */
    private void onAddRow() {
        HandleException.for_(this, () -> {
            EditDescriptionAndAmountView editDescriptionAndAmountView = new EditDescriptionAndAmountView(
                    "editInvoiceView.addRowTileId", currency);
            ViewDialog dialog = new ViewDialog(getParentWindow(), editDescriptionAndAmountView);
            dialog.showDialog();
            if (editDescriptionAndAmountView.getEditedDescription() != null) {
                tableModel.addRow(editDescriptionAndAmountView.getEditedDescription(),
                        editDescriptionAndAmountView.getEditedAmount());
            }
        });
    }

    /**
     * This method is called when the user wants to edit an existing row.
     */
    private void onEditRow() {
        HandleException.for_(this, () -> {
            int[] indices = table.getSelectedRows();
            if (indices.length == 0) {
                MessageDialog.showInfoMessage(this, "editInvoiceView.noRowsSelectedToEdit");
            } else if (indices.length > 1) {
                MessageDialog.showInfoMessage(this, "editInvoiceView.multipleRowsSelectedToEdit");
            } else {
                Tuple<String, Amount> tuple = tableModel.getRow(indices[0]);
                EditDescriptionAndAmountView editDescriptionAndAmountView = new EditDescriptionAndAmountView(
                        "editInvoiceView.editRowTileId",
                        tuple.getFirst(),
                        tuple.getSecond(),
                        currency);
                ViewDialog dialog = new ViewDialog(getParentWindow(), editDescriptionAndAmountView);
                dialog.showDialog();
                if (editDescriptionAndAmountView.getEditedDescription() != null) {
                    tableModel.updateRow(indices[0], editDescriptionAndAmountView.getEditedDescription(),
                            editDescriptionAndAmountView.getEditedAmount());
                }
            }
        });
    }

    /**
     * This method is called when the user wants to delete an existing row.
     */
    private void onDeleteRow() {
        int[] indices = table.getSelectedRows();
        if (indices.length == 0) {
            MessageDialog.showInfoMessage(this, "editInvoiceView.noRowsSelectedForDeletion");
        } else {
            tableModel.removeRows(indices);
        }
    }

    @Override
	protected void onOk() {
        String id = idModel.getString();
        if (id.length() == 0) {
            MessageDialog.showWarningMessage(this, "editInvoiceView.noIdEntered");
            return;
        }
        Date issueDate = dateModel.getDate();
        if (issueDate == null) {
            MessageDialog.showWarningMessage(this, "editInvoiceView.noDateEntered");
            return;
        }

        String description = descriptionModel.getString();
        if (StringUtil.isNullOrEmpty(description)) {
            MessageDialog.showWarningMessage(this, "editInvoiceView.noDescriptionEntered");
            return;
        }

        Party concerningParty = concerningPartyModel.getParty();
        if (concerningParty == null) {
            MessageDialog.showWarningMessage(this, "editInvoiceView.noConcerningPartyEntered");
            return;
        }

        Party payingParty = payingPartyModel.getParty();
        if (payingParty == null) {
            MessageDialog.showWarningMessage(this, "editInvoiceView.noPayingPartyEntered");
            return;
        }

        Amount amount;
        try {
             amount = new Amount(amountFormat.parse(amountModel.getString()));
        } catch (ParseException e) {
            MessageDialog.showWarningMessage(this, "gen.invalidAmount");
            return;
        }

        List<Tuple<String, Amount>> tuples = tableModel.getRows();
        editedDescriptions = new ArrayList<>();
        editedAmounts = new ArrayList<>();
        for (Tuple<String, Amount> tuple : tuples) {
        	editedDescriptions.add(tuple.getFirst());
        	editedAmounts.add(tuple.getSecond() == null || tuple.getSecond().isZero() ? null : tuple.getSecond());
        }

        editedInvoice = new Invoice(id);
        editedInvoice.setDescription(description);
        editedInvoice.setIssueDate(issueDate);
        editedInvoice.setPayingPartyId(payingParty.getId());
        editedInvoice.setConcerningPartyId(concerningParty.getId());
        editedInvoice.setAmountToBePaid(amount);
        closeAction.actionPerformed(null);
    }

    private final class DeleteAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    onDeleteRow();
		}
	}

	private final class EditAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    onEditRow();
		}
	}

	private final class AddAction extends AbstractAction {
		@Override
		public void actionPerformed(ActionEvent e) {
		    onAddRow();
		}
	}

	private final class CopyConceringPartyToPayingPartyListener implements
			ModelChangeListener {
		@Override
		public void modelChanged(AbstractModel model) {
		    if (payingPartyModel.getParty() == null) {
		        payingPartyModel.setParty(concerningPartyModel.getParty());
		    }
		}
	}

	/**
     * Table model for the table containing descriptions and models.
     */
    private static class DescriptionAndAmountTableModel extends ListTableModel<Tuple<String, Amount>> {

        public DescriptionAndAmountTableModel(AmountFormat amountFormat, List<Tuple<String, Amount>> tuples) {
        	super(asList(
                    ColumnDefinition.<Tuple<String, Amount>>builder("editInvoiceView.tableHeader.descriptions", String.class, 300)
                        .add(Tuple::getFirst)
                        .build(),
                    ColumnDefinition.<Tuple<String, Amount>>builder("editInvoiceView.tableHeader.amounts", String.class, 100)
                        .add(new AmountCellRenderer(amountFormat))
                        .add(row -> row.getSecond() == null || row.getSecond().isZero() ? null : row.getSecond())
                        .build()
            ));
            setRows(tuples);
        }

        /**
         * Adds a row to the end of the table.
         * @param description the description of the row
         * @param amount the amount of the row; can be <code>null</code>
         */
        public void addRow(String description, Amount amount) {
            addRow(new Tuple<>(description, amount));
        }

        /**
         * Updates a row.
         * @param index the index of the row
         * @param description the new description of the row
         * @param amount the new amount of the row; can be <code>null</code>
         */
        public void updateRow(int index, String description, Amount amount) {
            updateRow(index, new Tuple<>(description, amount));
        }

    }
}
