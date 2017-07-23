package nl.gogognome.gogoaccount.component.invoice;

import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.lib.text.Amount;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InvoiceTemplate {

    public enum Type {
        PURCHASE,
        SALE
    }

    private final Type type;
    private final String id;
    private final String partyReference;
    private final Date issueDate;
    private String description;
    private final List<InvoiceTemplateLine> invoiceTemplateLines;

    public InvoiceTemplate(Type type, String id, String partyReference, Date issueDate, String description, List<InvoiceTemplateLine> invoiceTemplateLines) {
        this.type = type;
        this.id = id;
        this.partyReference = partyReference;
        this.issueDate = issueDate;
        this.description = description;
        this.invoiceTemplateLines = invoiceTemplateLines;
    }

    public Type getType() {
        return type;
    }

    public String getId() {
        return id;
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

    public List<InvoiceTemplateLine> getLines() {
        return invoiceTemplateLines;
    }

    public InvoiceDefinition getInvoiceDefinitionFor(Party party, List<String> partyTags) {
        List<InvoiceDefinitionLine> resultLineDefinitions = new ArrayList<>();

        for (InvoiceTemplateLine line : invoiceTemplateLines) {
            Amount amount =  line.getAmountFormula().getAmount(partyTags);
            if (amount != null) {
                resultLineDefinitions.add(new InvoiceDefinitionLine(amount, line.getDescription(), line.getAccount()));
            }
        }
        return new InvoiceDefinition(party, type, replaceKeywords(getId(), party), getPartyReference(), issueDate, replaceKeywords(getDescription(), party), resultLineDefinitions);
    }

    /**
     * Replaces the keywords <code>{id}</code> and <code>{name}</code> with the corresponding
     * attributes of the specified party.
     * @param someString the string in which the replacement has to be made
     * @param party the party
     * @return the string after the replacements have taken place
     */
    private String replaceKeywords(String someString, Party party) {
        StringBuilder sb = new StringBuilder(someString);
        String[] keywords = new String[] { "{id}", "{name}" };
        String[] values = new String[] {
                party.getId(), party.getName()
        };

        for (int k=0; k<keywords.length; k++) {
            String keyword = keywords[k];
            String value = values[k];
            for (int index=sb.indexOf(keyword); index != -1; index=sb.indexOf(keyword)) {
                sb.replace(index, index+keyword.length(), value);
            }
        }
        return sb.toString();
    }

}
