package nl.gogognome.gogoaccount.component.invoice.amountformula;

import nl.gogognome.lib.text.Amount;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.textsearch.criteria.Parser;

import java.text.ParseException;

public class AmountFormulaParser {

    private final AmountFormat amountFormat;
    private Scanner scanner;
    private String currentToken;

    public AmountFormulaParser(AmountFormat amountFormat) {
        this.amountFormat = amountFormat;
    }

    public AmountFormula parse(String text) throws ParseException {
        scanner = new Scanner(text);
        if (!scanner.hasNextToken()) {
            throw new ParseException("Text must not be empty", 0);
        }
        currentToken = scanner.nextToken();
        AmountFormula result = parseExpression();
        if (currentToken != null) {
            throw new ParseException("Expected end of string but found : " + currentToken, 0);
        }
        return result;
    }

    private AmountFormula parseExpression() throws ParseException {
        if ("if".equals(currentToken)) {
            return parseIf();
        } else if ("(".equals(currentToken)) {
            return parseExpressionInsideBrackets();
        } else {
            try {
                return parseAmount();
            } catch (ParseException e) {
                throw new ParseException("Expected amount but found " + (currentToken != null ? currentToken : "end of string"), 0);
            }
        }
    }

    private AmountFormula parseAmount() throws ParseException {
        ConstantAmount result = new ConstantAmount(new Amount(amountFormat.parse(currentToken)));
        currentToken = scanner.nextToken();
        return result;
    }

    private AmountFormula parseExpressionInsideBrackets() throws ParseException {
        if (!"(".equals(currentToken)) {
            throw new ParseException("Expected '('", 0);
        }
        currentToken = scanner.nextToken();
        AmountFormula result = parseExpression();

        if (!")".equals(currentToken)) {
            throw new ParseException("Expected ')'", 0);
        }
        currentToken = scanner.nextToken();

        return result;
    }

    private AmountFormula parseIf() throws ParseException {
        if (!"if".equals(currentToken)) {
            throw new ParseException("Expected 'if'", 0);
        }
        if (!"(".equals(scanner.nextToken())) {
            throw new ParseException("Expected '('", 0);
        }
        StringBuilder tagExpression = new StringBuilder();
        currentToken = scanner.nextToken();
        int openBracketCount = 0;
        while (currentToken != null && (openBracketCount > 0 || !")".equals(currentToken))) {
            tagExpression.append(currentToken).append(' ');
            if ("(".equals(currentToken)) {
                openBracketCount++;
            }
            if (")".equals(currentToken)) {
                openBracketCount--;
            }
            currentToken = scanner.nextToken();
        }
        currentToken = scanner.nextToken();
        AmountFormula thenAmountFormula = parseExpression();

        AmountFormula elseAmountFormula = null;
        if (currentToken != null && "else".equals(currentToken)) {
            currentToken = scanner.nextToken();
            elseAmountFormula = parseExpression();
        }
        return new IfThenElseAmount(new Parser().parse(tagExpression.toString()), thenAmountFormula, elseAmountFormula);
    }
}
