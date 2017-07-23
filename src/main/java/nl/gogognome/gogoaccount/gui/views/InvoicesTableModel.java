package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.tablecellrenderer.AmountCellRenderer;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.ListTableModel;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;

import java.util.Date;

/**
 * The table model that shows information about the invoices.
 */
class InvoicesTableModel extends ListTableModel<Invoice> {

    private final InvoiceService invoiceService;
    private final PartyService partyService;

    // TODO: Get rid of service calls!
    public InvoicesTableModel(Document document, AmountFormat amountFormat, InvoiceService invoiceService, PartyService partyService) {
        this.invoiceService = invoiceService;
        this.partyService = partyService;
        setColumnDefinitions(
                ColumnDefinition.<Invoice>builder("gen.id", String.class, 40)
                    .add(row -> row.getId())
                    .build(),
                ColumnDefinition.<Invoice>builder("gen.name", String.class, 200)
                    .add(row -> { try { return this.partyService.getParty(document, row.getPartyId()).getName(); } catch (ServiceException e) { return "???"; }} )
                    .build(),
                ColumnDefinition.<Invoice>builder("gen.saldo", Amount.class, 200)
                    .add(new AmountCellRenderer(amountFormat))
                    .add(row -> { try { return this.invoiceService.getRemainingAmountToBePaid(document, row.getId(), new Date()); } catch (ServiceException e) { return null; }})
                    .build(),
                ColumnDefinition.<Invoice>builder("gen.date", Date.class, 100)
                    .add(row -> row.getIssueDate())
                    .build()
        );
    }
}
