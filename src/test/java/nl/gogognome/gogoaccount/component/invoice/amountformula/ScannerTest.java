package nl.gogognome.gogoaccount.component.invoice.amountformula;

import static java.util.Arrays.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.*;
import org.junit.jupiter.api.*;

public class ScannerTest {

    @Test
    public void testScanner() {
        validateScannerReturnsTokens("");
        validateScannerReturnsTokens("123", "123");
        validateScannerReturnsTokens("123,50", "123,50");
        validateScannerReturnsTokens("-123.50", "-123.50");
        validateScannerReturnsTokens("if (tag1 or tag2) 10", "if", "(", "tag1", "or", "tag2", ")", "10");
        validateScannerReturnsTokens("if (tag1 or tag2) 10 else 20", "if", "(", "tag1", "or", "tag2", ")", "10", "else", "20");
        validateScannerReturnsTokens("a(b[c{d}])e", "a", "(", "b", "[", "c", "{", "d", "}", "]", ")", "e");
    }

    private void validateScannerReturnsTokens(String text, String... expectedTokens) {
        List<String> tokens = getTokens(text);
        assertEquals(asList(expectedTokens), tokens);
    }

    private List<String> getTokens(String text) {
        List<String> tokens = new ArrayList<>();
        Scanner scanner = new Scanner(text);
        while (scanner.hasNextToken()) {
            tokens.add(scanner.nextToken());
        }
        return tokens;
    }
}