package nl.gogognome.gogoaccount.component.document;

public class AttemptToModifyReadonlyDocumentException extends RuntimeException {

    public AttemptToModifyReadonlyDocumentException() {
        super("An attempt was made to modify a readonly document.");
    }
}
