package nl.gogognome.gogoaccount.gui.configuration;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.email.EmailConfiguration;
import nl.gogognome.gogoaccount.component.email.EmailService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.ButtonPanel;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.models.IntegerModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.TextResource;

import javax.swing.*;
import java.awt.*;

public class EmailConfigurationView extends View {

    private final Document document;
    private final TextResource textResource;
    private final EmailService emailService;

    private EmailConfiguration configuration;
    private final StringModel senderEmailAddressModel = new StringModel();
    private final StringModel smtpHostModel = new StringModel();
    private final IntegerModel smtpPortModel = new IntegerModel();
    private final StringModel smtpUsernameModel = new StringModel();
    private final StringModel smtpPasswordModel = new StringModel();

    public EmailConfigurationView(Document document, TextResource textResource, EmailService emailService) {
        this.document = document;
        this.textResource = textResource;
        this.emailService = emailService;
    }

    @Override
    public String getTitle() {
        return textResource.getString("EmailConfigurationView.title");
    }

    @Override
    public void onInit() {
        try {
            initModels();
            addComponents();
        } catch (Exception e) {
            MessageDialog.showErrorMessage(getViewOwner().getWindow(), "gen.internalError", e);
            close();
        }
    }

    private void initModels() throws ServiceException {
        configuration = emailService.getConfiguration(document);
        senderEmailAddressModel.setString(configuration.getSenderEmailAddress());
        smtpHostModel.setString(configuration.getSmtpHost());
        smtpPortModel.setInteger(configuration.getSmtpPort());
        smtpUsernameModel.setString(configuration.getSmtpUsername());
        smtpPasswordModel.setString(configuration.getSmtpPassword());
    }

    private void addComponents() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setMinimumSize(new Dimension(500, 100));

        InputFieldsColumn ifc = new InputFieldsColumn();
        addCloseable(ifc);
        ifc.addField("EmailConfigurationView.senderEmailAddress", senderEmailAddressModel);
        ifc.addField("EmailConfigurationView.smtpHost", smtpHostModel);
        ifc.addField("EmailConfigurationView.smtpHost", smtpPortModel);
        ifc.addField("EmailConfigurationView.smtpUsername", smtpUsernameModel);
        ifc.addField("EmailConfigurationView.smtpPassword", smtpPasswordModel);
        add(ifc, BorderLayout.CENTER);

        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.addButton("gen.save", this::onSave);
        buttonPanel.addButton("gen.cancel", this::close);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void onSave() {
        configuration.setSenderEmailAddress(senderEmailAddressModel.getString());
        configuration.setSmtpHost(smtpHostModel.getString());
        configuration.setSmtpPort(smtpPortModel.getInteger());
        configuration.setSmtpUsername(smtpUsernameModel.getString());
        configuration.setSmtpPassword(smtpPasswordModel.getString());
        try {
            emailService.saveConfiguration(document, configuration);
            close();
        } catch (ServiceException e) {
            MessageDialog.showErrorMessage(getViewOwner().getWindow(), "gen.saveError", e);
        }
    }

    @Override
    public void onClose() {
    }
}
