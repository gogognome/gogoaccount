package nl.gogognome.gogoaccount.component.email;

public class EmailConfiguration {

    public enum SmtpEncryption {
        NONE,
        SSL,
        STARTTLS
    }

    private String senderEmailAddress;
    private String smtpHost;
    private int smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private SmtpEncryption smtpEncryption = SmtpEncryption.NONE;

    public String getSenderEmailAddress() {
        return senderEmailAddress;
    }

    public void setSenderEmailAddress(String senderEmailAddress) {
        this.senderEmailAddress = senderEmailAddress;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public String getSmtpUsername() {
        return smtpUsername;
    }

    public void setSmtpUsername(String smtpUsername) {
        this.smtpUsername = smtpUsername;
    }

    public String getSmtpPassword() {
        return smtpPassword;
    }

    public void setSmtpPassword(String smtpPassword) {
        this.smtpPassword = smtpPassword;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public SmtpEncryption getSmtpEncryption() {
        return smtpEncryption;
    }

    public void setSmtpEncryption(SmtpEncryption smtpEncryption) {
        this.smtpEncryption = smtpEncryption;
    }
}
