package nl.gogognome.gogoaccount.component.invoice.amountformula;

public class Scanner {

    private final String text;
    private int index = 0;
    private String nextToken;

    public Scanner(String text) {
        this.text = text;
        determineNextToken();
    }

    public boolean hasNextToken() {
        return nextToken != null;
    }

    public String nextToken() {
        String result = nextToken;
        determineNextToken();
        return result;
    }

    private void determineNextToken() {
        nextToken = null;
        while (index < text.length()) {
            char c = text.charAt(index);
            if (Character.isWhitespace(c)) {
                index++;
                continue;
            }

            if (isSymbol(c)) {
                nextToken = Character.toString(c);
                index++;
                return;
            }

            int startIndex = index;
            index++;
            while (index < text.length() && !Character.isWhitespace(text.charAt(index)) && !isSymbol(text.charAt(index))) {
                index++;
            }
            nextToken = text.substring(startIndex, index);
            return;
        }
    }

    private boolean isSymbol(char c) {
        return "()[]{}".indexOf(c) != -1;
    }

}
