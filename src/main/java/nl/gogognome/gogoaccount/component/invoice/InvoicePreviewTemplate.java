package nl.gogognome.gogoaccount.component.invoice;

import com.google.common.collect.ImmutableMap;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.text.KeyValueReplacer;
import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;

import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class InvoicePreviewTemplate {

    private final AmountFormat amountFormat;
    private final KeyValueReplacer keyValueReplacer;
    private final TextResource textResource;

    public InvoicePreviewTemplate(AmountFormat amountFormat, KeyValueReplacer keyValueReplacer, TextResource textResource) {
        this.amountFormat = amountFormat;
        this.keyValueReplacer = keyValueReplacer;
        this.textResource = textResource;
    }

    public String fillInParametersInTemplate(String templateContents, Invoice invoice, List<InvoiceDetail> invoiceDetails,
                                             List<Payment> payments, Party party, Date dueDate) {
        Amount amountToBePaid = getAmountToBePaid(invoice, invoiceDetails, payments);
        Map<String, String> replacements = new HashMap<>();
        replacements.put("${date}", textResource.formatDate("gen.dateFormatFull", new Date()));
        replacements.put("${invoice.id}", invoice.getId());
        replacements.put("${invoice.partyReference}", invoice.getPartyReference());
        replacements.put("${invoice.description}", invoice.getDescription());
        replacements.put("${invoice.amount}", amountFormat.formatAmount(amountToBePaid.toBigInteger()));
        replacements.put("${invoice.issueDate}", textResource.formatDate("gen.dateFormatFull", invoice.getIssueDate()));
        replacements.put("${invoice.dueDate}", textResource.formatDate("gen.dateFormatFull", dueDate));
        replacements.put("${party.id}", party.getId());
        replacements.put("${party.name}", party.getName());
        replacements.put("${party.address}", party.getAddress());
        replacements.put("${party.zipCode}", party.getZipCode());
        replacements.put("${party.city}", party.getCity());

        String result = keyValueReplacer.applyReplacements(templateContents, replacements, this::escapeSpecialCharacters);

        int lineStartIndex = result.indexOf("${lineStart}");
        int lineEndIndex = result.indexOf("${lineEnd}");
        if (lineStartIndex != -1 && lineEndIndex != -1) {
            String lineTemplate = result.substring(lineStartIndex + "${lineStart}".length(), lineEndIndex);
            StringBuilder lines = new StringBuilder();
            appendInvoiceDetails(lines, lineTemplate, invoiceDetails, d -> invoice.getIssueDate(), InvoiceDetail::getDescription,
                    d -> getAmountToBePaidForDetailLine(invoice, d));
            appendInvoiceDetails(lines, lineTemplate, payments, Payment::getDate, Payment::getDescription, p -> p.getAmount().negate());
            result = result.substring(0, lineStartIndex) + lines + result.substring(lineEndIndex + "${lineEnd}".length());
        }
        return result;
    }

    private Amount getAmountToBePaid(Invoice invoice, List<InvoiceDetail> invoiceDetails, List<Payment> payments) {
        Amount amountToBePaid = new Amount(BigInteger.ZERO);
        for (InvoiceDetail invoiceDetail : invoiceDetails) {
            amountToBePaid = amountToBePaid.add(getAmountToBePaidForDetailLine(invoice, invoiceDetail));
        }
        for (Payment payment : payments) {
            amountToBePaid = amountToBePaid.subtract(payment.getAmount());
        }
        return amountToBePaid;
    }

    private Amount getAmountToBePaidForDetailLine(Invoice invoice, InvoiceDetail detailLine) {
        return invoice.getAmountToBePaid().isNegative() ? detailLine.getAmount().negate() : detailLine.getAmount();
    }

    private <T> void appendInvoiceDetails(StringBuilder formattedLines,
                                          String lineTemplate, List<T> objects,
                                          Function<T, Date> date,
                                          Function<T, String> description,
                                          Function<T, Amount> amount) {
        for (T object : objects) {
            ImmutableMap<String, String> lineReplacement = ImmutableMap.of(
                    "${line.date}", textResource.formatDate("gen.dateFormat", date.apply(object)),
                    "${line.description}", description.apply(object),
                    "${line.amount}", amountFormat.formatAmount(amount.apply(object).toBigInteger()));
            formattedLines.append(keyValueReplacer.applyReplacements(lineTemplate, lineReplacement, this::escapeSpecialCharacters));
        }
    }

    private String escapeSpecialCharacters(String input) {
        if (input == null) {
            return "";
        }

        StringBuilder output = new StringBuilder(input.length());
        for (char c : input.toCharArray()) {
            if (c < 32 || c > 127 || "&\"'<>".indexOf(c) != -1) {
                output.append("&#").append((int) c).append(";");
            } else {
                output.append(c);
            }
        }
        return output.toString();
    }
}
