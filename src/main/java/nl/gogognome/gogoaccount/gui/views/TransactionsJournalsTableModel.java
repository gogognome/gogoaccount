package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.ledger.JournalEntry;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.tablecellrenderer.AmountCellRenderer;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.ListTableModel;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

class TransactionsJournalsTableModel extends ListTableModel<Transaction> {

    private final Logger logger = LoggerFactory.getLogger(TransactionsJournalsTableModel.class);
    private final InvoiceService invoiceService;
    private final PartyService partyService;

	private final Document document;

    TransactionsJournalsTableModel(AmountFormat amountFormat, InvoiceService invoiceService, PartyService partyService, Document document) {
        this.invoiceService = invoiceService;
        this.partyService = partyService;
        setColumnDefinitions(
                ColumnDefinition.<Transaction>builder("importBankStatementView.date", Date.class, 70)
                    .add(row -> row.getImportedTransaction().getDate())
                    .build(),
                ColumnDefinition.<Transaction>builder("importBankStatementView.toAccount", String.class, 100)
                    .add(row -> row.getImportedTransaction().getToAccount())
                    .build(),
                ColumnDefinition.<Transaction>builder("importBankStatementView.fromAccount", String.class, 100)
                    .add(row -> row.getImportedTransaction().getFromAccount())
                    .build(),
                ColumnDefinition.<Transaction>builder("importBankStatementView.amount", Amount.class, 100)
                    .add(new AmountCellRenderer(amountFormat))
                    .add(row -> row.getImportedTransaction().getAmount())
                    .build(),
                ColumnDefinition.<Transaction>builder("importBankStatementView.description1", String.class, 200)
                    .add(row -> row.getImportedTransaction().getDescription())
                    .build(),
                ColumnDefinition.<Transaction>builder("importBankStatementView.id", String.class, 50)
                    .add(row -> row.getJournalEntry() != null ? row.getJournalEntry().getId() : null)
                    .build(),
                ColumnDefinition.<Transaction>builder("importBankStatementView.description2", String.class, 200)
                    .add(row -> row.getJournalEntry() != null ? row.getJournalEntry().getDescription() : null)
                    .build(),
                ColumnDefinition.<Transaction>builder("importBankStatementView.invoice", String.class, 200)
                    .add(row -> getInvoice(row))
                    .build()
        );
        this.document = document;
    }


    private String getInvoice(Transaction row) {
        JournalEntry journalEntry = row.getJournalEntry();
        if (journalEntry != null && journalEntry.getIdOfCreatedInvoice() != null) {
            try {
                Invoice invoice = invoiceService.getInvoice(document, journalEntry.getIdOfCreatedInvoice());
                Party party = partyService.getParty(document, invoice.getPartyId());
                return invoice.getId() + " (" + party.getName() + ")";
            } catch (ServiceException e) {
                logger.warn("Ignored exception: " + e.getMessage(), e);
                return "???";
            }
        }
        return null;
    }
}