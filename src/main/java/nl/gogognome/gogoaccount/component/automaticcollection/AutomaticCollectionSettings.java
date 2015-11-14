package nl.gogognome.gogoaccount.component.automaticcollection;

public class AutomaticCollectionSettings {

    private String iban;
    private String bic;
    private String automaticCollectionContractNumber;

    public String getAutomaticCollectionContractNumber() {
        return automaticCollectionContractNumber;
    }

    public void setAutomaticCollectionContractNumber(String automaticCollectionContractNumber) {
        this.automaticCollectionContractNumber = automaticCollectionContractNumber;
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

}
