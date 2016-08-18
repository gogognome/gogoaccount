package nl.gogognome.gogoaccount.gui.views;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import nl.gogognome.gogoaccount.component.invoice.Invoice;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.gui.tablecellrenderer.AmountCellRenderer;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.ColumnDefinition;
import nl.gogognome.lib.swing.ListTableModel;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.util.DayOfYearComparator;
import nl.gogognome.lib.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The table model that shows information about the invoices.
 */
class InvoicesTableModel extends ListTableModel<Invoice> {

    private final InvoiceService invoiceService = ObjectFactory.create(InvoiceService.class);
    private final PartyService partyService = ObjectFactory.create(PartyService.class);

    // TODO: Get rid of service calls!
    public InvoicesTableModel(Document document, AmountFormat amountFormat) {
        setColumnDefinitions(
                ColumnDefinition.<Invoice>builder("gen.id", String.class, 40)
                    .add(row -> row.getId())
                    .build(),
                ColumnDefinition.<Invoice>builder("gen.name", String.class, 200)
                    .add(row -> { try { return partyService.getParty(document, row.getPayingPartyId()).getName(); } catch (ServiceException e) { return "???"; }} )
                    .build(),
                ColumnDefinition.<Invoice>builder("gen.saldo", Amount.class, 200)
                    .add(new AmountCellRenderer(amountFormat))
                    .add(row -> { try { return invoiceService.getRemainingAmountToBePaid(document, row.getId(), new Date()); } catch (ServiceException e) { return null; }})
                    .build(),
                ColumnDefinition.<Invoice>builder("gen.date", Date.class, 100)
                    .add(row -> row.getIssueDate())
                    .build()
        );
    }
}
