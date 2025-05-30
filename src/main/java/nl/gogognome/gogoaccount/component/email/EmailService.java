package nl.gogognome.gogoaccount.component.email;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.settings.SettingsService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.ServiceTransaction;
import nl.gogognome.lib.text.TextResource;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Properties;

public class EmailService {

    private final static int DEFAULT_PORT = 25;
    private final static String MAIL_FROM = "EmailService.MailFrom";
    private final static String MAIL_SMTP_HOST = "EmailService.MailSmtpHost";
    private final static String MAIL_SMTP_PORT = "EmailService.MailSmtpPort";
    private final static String MAIL_SMTP_USERNAME = "EmailService.MailUserName";
    private final static String MAIL_SMTP_PASSWORD = "EmailService.MailPassword";
    private final static String MAIL_SMTP_ENCRYPTION = "EmailService.MailEncryption";

    private final SettingsService settingsService;
    private final TextResource textResource;

    public EmailService(SettingsService settingsService, TextResource textResource) {
        this.settingsService = settingsService;
        this.textResource = textResource;
    }

    public EmailConfiguration getConfiguration(Document document) throws ServiceException {
        return ServiceTransaction.withResult(() -> {
            EmailConfiguration emailConfiguration = new EmailConfiguration();
            emailConfiguration.setSenderEmailAddress(settingsService.findValueForSetting(document, MAIL_FROM));
            emailConfiguration.setSmtpHost(settingsService.findValueForSetting(document, MAIL_SMTP_HOST));
            String portString = settingsService.findValueForSetting(document, MAIL_SMTP_PORT);
            emailConfiguration.setSmtpPort(portString != null ? Integer.parseInt(portString) : DEFAULT_PORT);
            emailConfiguration.setSmtpUsername(settingsService.findValueForSetting(document, MAIL_SMTP_USERNAME));
            emailConfiguration.setSmtpPassword(settingsService.findValueForSetting(document, MAIL_SMTP_PASSWORD));

            String encryptionString = settingsService.findValueForSetting(document, MAIL_SMTP_ENCRYPTION);
            emailConfiguration.setSmtpEncryption(encryptionString != null ? EmailConfiguration.SmtpEncryption.valueOf(encryptionString) : EmailConfiguration.SmtpEncryption.NONE);
            return emailConfiguration;
        });
    }

    public void saveConfiguration(Document document, EmailConfiguration configuration) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            document.ensureDocumentIsWriteable();
            settingsService.save(document, MAIL_FROM, configuration.getSenderEmailAddress());
            settingsService.save(document, MAIL_SMTP_HOST, configuration.getSmtpHost());
            settingsService.save(document, MAIL_SMTP_PORT, Integer.toString(configuration.getSmtpPort()));
            settingsService.save(document, MAIL_SMTP_USERNAME, configuration.getSmtpUsername());
            settingsService.save(document, MAIL_SMTP_PASSWORD, configuration.getSmtpPassword());
            settingsService.save(document, MAIL_SMTP_ENCRYPTION, configuration.getSmtpEncryption().toString());
        });
    }

    public void sendEmail(Document document, String recipient, String subject, String contents,
                          String charset, String subtype) throws ServiceException {
        ServiceTransaction.withoutResult(() -> {
            document.ensureDocumentIsWriteable();
            EmailConfiguration configuration = getConfiguration(document);
            sendEmail(recipient, subject, contents, charset, subtype, configuration);
        });
    }

    // Package scope for tests.
    void sendEmail(String recipient, String subject, String contents, String charset, String subtype, EmailConfiguration configuration) throws ServiceException {
        if (configuration.getSenderEmailAddress() == null || configuration.getSmtpHost() == null
                || configuration.getSmtpUsername() == null || configuration.getSmtpPassword() == null) {
            throw new ServiceException(textResource.getString("EmailService.configurationIsIncomplete"));
        }

        Properties props = buildPropertiesFromConfiguration(configuration);

        try {
            Session session = Session.getInstance(props, null);
            String senderEmailAddress = configuration.getSenderEmailAddress();
            MimeMessage msg = buildMimeMessage(session, recipient, subject, contents, charset, subtype, senderEmailAddress);
            Transport.send(msg, configuration.getSmtpUsername(), configuration.getSmtpPassword());
        } catch (Exception e) {
            throw new ServiceException(e);
        }
    }

    private Properties buildPropertiesFromConfiguration(EmailConfiguration configuration) {
        Properties props = new Properties();
        props.put("mail.smtp.host", configuration.getSmtpHost());
        props.put("mail.smtp.port", configuration.getSmtpPort());

        switch (configuration.getSmtpEncryption()) {
            case NONE:
                break;
            case STARTTLS:
                props.put("mail.smtp.starttls.enable", "true");
                break;
            case SSL:
                props.put("mail.smtp.ssl.enable", "true");
                break;
            default:
                throw new IllegalArgumentException("The encryption type " + configuration.getSmtpEncryption() + " has not been implemented yet.");
        }
        return props;
    }

    private MimeMessage buildMimeMessage(Session session, String recipient, String subject, String contents, String charset, String subtype, String senderEmailAddress) throws MessagingException {
        MimeMessage msg = new MimeMessage(session);
        msg.setFrom(senderEmailAddress);
        msg.setRecipients(Message.RecipientType.TO, recipient);
        msg.setRecipients(Message.RecipientType.BCC, senderEmailAddress);
        msg.setSubject(subject);
        msg.setSentDate(new Date());
        msg.setText(contents, charset, subtype);
        return msg;
    }
}
