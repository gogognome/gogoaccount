package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.text.Amount;

import java.math.BigInteger;
import java.util.Date;
import java.util.List;

public class InvoiceDefinition {

    private final Party party;
    private final InvoiceTemplate.Type type;
    private final String partyReference;
    private final Date issueDate;
    private final String description;
    private final List<InvoiceDefinitionLine> invoiceDefinitionLines;

    InvoiceDefinition(Party party, InvoiceTemplate.Type type, String partyReference,
                      Date issueDate, String description, List<InvoiceDefinitionLine> invoiceDefinitionLines) {
        this.party = party;
        this.type = type;
        this.partyReference = partyReference;
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

    public String getPartyReference() {
        return partyReference;
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
                .map(InvoiceDefinitionLine::getAmount)
                .reduce(new Amount(BigInteger.ZERO), (a,b) -> a.add(b));
    }

}
