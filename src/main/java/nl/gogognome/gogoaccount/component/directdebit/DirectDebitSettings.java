package nl.gogognome.gogoaccount.component.directdebit;

public class DirectDebitSettings {

    private String iban;
    private String bic;
    private String sepaDirectDebitContractNumber;
    private long sequenceNumber;

    public String SepaDirectDebitContractNumber() {
        return sepaDirectDebitContractNumber;
    }

    public void setSepaDirectDebitContractNumber(String sepaDirectDebitContractNumber) {
        this.sepaDirectDebitContractNumber = sepaDirectDebitContractNumber;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public String getBic() {
        return bic;
    }

    public void setBic(String bic) {
        this.bic = bic;
    }

    public long getSequenceNumber() {
        return sequenceNumber;
    }

    public void setSequenceNumber(long sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }
}
