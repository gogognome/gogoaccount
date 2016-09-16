package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.text.Amount;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public class InvoiceDefinition {

    private final Party party;
    private final InvoiceTemplate.Type type;
    private final String id;
    private final Date issueDate;
    private final String description;
    private final List<InvoiceDefinitionLine> invoiceDefinitionLines;

    public InvoiceDefinition(Party party, InvoiceTemplate.Type type, String id, Date issueDate, String description, List<InvoiceDefinitionLine> invoiceDefinitionLines) {
        this.party = party;
        this.type = type;
        this.id = id;
        this.issueDate = issueDate;
        this.description = description;
        this.invoiceDefinitionLines = invoiceDefinitionLines;
    }

    public Party getParty() {
        return party;
    }

    public InvoiceTemplate.Type getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public Date getIssueDate() {
        return issueDate;
    }

    public String getDescription() {
        return description;
    }

    public List<InvoiceDefinitionLine> getLines() {
        return invoiceDefinitionLines;
    }

    public Amount getTotalAmount() {
        return invoiceDefinitionLines.stream()
                .map(l -> l.getAmount())
                .reduce(new Amount(BigInteger.ZERO), (a,b) -> a.add(b));
    }

}
