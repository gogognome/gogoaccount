package nl.gogognome.gogoaccount.gui.views;

import com.google.common.base.Strings;
import nl.gogognome.gogoaccount.component.directdebit.PartyDirectDebitSettings;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.components.PartyTagBean;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * This class implements a view to edit a party. Either an existing party or
 * a new party can be edited.
 */
public class EditPartyView extends OkCancelView {

	private static final long serialVersionUID = 1L;

    private final Document document;
    private final ConfigurationService configurationService;
    private final PartyService partyService;
    private final MessageDialog messageDialog;
    private final HandleException handleException;
    private List<String> tags;

    private final StringModel nameModel = new StringModel();
    private final StringModel addressModel = new StringModel();
    private final StringModel zipCodeModel = new StringModel();
    private final StringModel cityModel = new StringModel();
    private final StringModel emailAddressModel = new StringModel();
    private final StringModel sepaDirectDebitNameModel = new StringModel();
    private final StringModel sepaDirectDebitAddressModel = new StringModel();
    private final StringModel sepaDirectDebitZipCodeModel = new StringModel();
    private final StringModel sepaDirectDebitCityModel = new StringModel();
    private final StringModel sepaDirectDebitCountryModel = new StringModel();
    private final StringModel sepaDirectDebitIbanModel = new StringModel();
    private final DateModel sepaDirectDebitMandateDateModel = new DateModel();
    private final List<ListModel<String>> tagListModels = new ArrayList<>();
    private final DateModel birthDateModel = new DateModel();
    private final JTextArea taRemarks = new JTextArea(5, 40);
    private JPanel labelsPanel = new JPanel(new GridBagLayout());
    private final List<PartyTagBean> partyTagBeans = new ArrayList<>();

    private Party initialParty;
    private List<String> initialTags;
    private PartyDirectDebitSettings initialSepaDirectDebitSettings;
    private Party resultParty;
    private List<String> resultTags;
    private PartyDirectDebitSettings resulSepaDirectDebitSettings;

    public EditPartyView(Document document, ConfigurationService configurationService, PartyService partyService) {
        this.document = document;
        this.configurationService = configurationService;
        this.partyService = partyService;
        messageDialog = new MessageDialog(textResource, this);
        handleException = new HandleException(messageDialog);
    }

    public void setInitialParty(Party party, List<String> initialTags,
                                PartyDirectDebitSettings sepaDirectDebitSettings) {
        this.initialParty = party;
        this.initialTags = initialTags;
        this.initialSepaDirectDebitSettings = sepaDirectDebitSettings;
    }

    @Override
	public void onInit() {
        try {
            initModels();
            addComponents();
        } catch (ServiceException e) {
            messageDialog.showErrorMessage(e, "gen.problemOccurred");
            close();
        }
    }

    private void initModels() throws ServiceException {
        tags = new ArrayList<>();
        tags.add("");
    	tags.addAll(partyService.findPartyTags(document));

    	if (initialParty != null) {
            nameModel.setString(initialParty.getName());
            addressModel.setString(initialParty.getAddress());
            zipCodeModel.setString(initialParty.getZipCode());
            cityModel.setString(initialParty.getCity());
            emailAddressModel.setString(initialParty.getEmailAddress());
            taRemarks.setText(initialParty.getRemarks());
            birthDateModel.setDate(initialParty.getBirthDate(), null);

            for (String tag : initialTags) {
                ListModel<String> tagListModel = new ListModel<>(tags);
                tagListModel.setSelectedItem(tag, null);
                tagListModels.add(tagListModel);
            }
        } else {
            tagListModels.add(new ListModel<>(tags)); // add one empty tag input field to start with
        }

        if (initialSepaDirectDebitSettings != null) {
            sepaDirectDebitNameModel.setString(initialSepaDirectDebitSettings.getName());
            sepaDirectDebitAddressModel.setString(initialSepaDirectDebitSettings.getAddress());
            sepaDirectDebitZipCodeModel.setString(initialSepaDirectDebitSettings.getZipCode());
            sepaDirectDebitCityModel.setString(initialSepaDirectDebitSettings.getCity());
            sepaDirectDebitCountryModel.setString(initialSepaDirectDebitSettings.getCountry());
            sepaDirectDebitIbanModel.setString(initialSepaDirectDebitSettings.getIban());
            sepaDirectDebitMandateDateModel.setDate(initialSepaDirectDebitSettings.getMandateDate());
        }
	}

    @Override
    protected JComponent createCenterComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        InputFieldsColumn ifc = new InputFieldsColumn();
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("editPartyView.generalInformation"));
        addCloseable(ifc);

        ifc.addField("editPartyView.name", nameModel);
        ifc.addField("editPartyView.address", addressModel);
        ifc.addField("editPartyView.zipCode", zipCodeModel);
        ifc.addField("editPartyView.city", cityModel);
        ifc.addField("editPartyView.emailAddress", emailAddressModel);
        ifc.addField("editPartyView.birthDate", birthDateModel);
        ifc.addVariableSizeField("editPartyView.tags", labelsPanel);
        initTagsPanel();

        ifc.addVariableSizeField("editPartyView.remarks", taRemarks);
        panel.add(ifc, BorderLayout.NORTH);

        try {
            if (configurationService.getBookkeeping(document).isEnableSepaDirectDebit()) {
                InputFieldsColumn sepaDirectDebitFields = new InputFieldsColumn();
                sepaDirectDebitFields.setBorder(widgetFactory.createTitleBorderWithPadding("editPartyView.directDebitInformation"));
                sepaDirectDebitFields.addField("editPartyView.sepaDirectDebitName", sepaDirectDebitNameModel);
                sepaDirectDebitFields.addField("editPartyView.sepaDirectDebitAddress", sepaDirectDebitAddressModel);
                sepaDirectDebitFields.addField("editPartyView.sepaDirectDebitZipCode", sepaDirectDebitZipCodeModel);
                sepaDirectDebitFields.addField("editPartyView.sepaDirectDebitCity", sepaDirectDebitCityModel);
                sepaDirectDebitFields.addField("editPartyView.sepaDirectDebitCountry", sepaDirectDebitCountryModel);
                sepaDirectDebitFields.addField("editPartyView.sepaDirectDebitIban", sepaDirectDebitIbanModel);
                sepaDirectDebitFields.addField("editPartyView.sepaDirectDebitMandateDate", sepaDirectDebitMandateDateModel);

                panel.add(sepaDirectDebitFields, BorderLayout.SOUTH);
            }
        } catch (ServiceException e) {
            messageDialog.showErrorMessage("gen.internalError", e);
        }

        return panel;
    }

    private void initTagsPanel() {
        labelsPanel.removeAll();
        partyTagBeans.forEach(PartyTagBean::close);
        partyTagBeans.clear();
        int index = 0;
        for (ListModel<String> tagListModel : tagListModels) {
            PartyTagBean partyTagBean = new PartyTagBean(tagListModel, handleException);

            final int finalIndex = index;
            JButton deleteButton = widgetFactory.createIconButton("editPartyView.deleteTag", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tagListModels.remove(finalIndex);
                    initTagsPanel();
                    getViewOwner().invalidateLayout();
                    if (!partyTagBeans.isEmpty()) {
                        SwingUtilities.invokeLater(() -> partyTagBeans.get(Math.min(finalIndex, partyTagBeans.size()-1)).requestFocus());
                    }
                }
            }, 20);
            partyTagBean.add(deleteButton);
            labelsPanel.add(partyTagBean, SwingUtils.createTextFieldGBConstraints(0, index));
            partyTagBeans.add(partyTagBean);
            index++;
        }

        JButton addButton = widgetFactory.createIconButton("editPartyView.addTag", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tagListModels.add(new ListModel<>(tags));
                initTagsPanel();
                getViewOwner().invalidateLayout();
                SwingUtilities.invokeLater(() -> partyTagBeans.get(partyTagBeans.size()-1).requestFocus());
            }
        }, 20);
        labelsPanel.add(addButton, SwingUtils.createGBConstraints(0, index, 1, 1, 0, 0, GridBagConstraints.EAST, 0, 0, 0, 0, 0));
    }

    @Override
    protected void onOk() {
        resultParty = initialParty != null ? initialParty : new Party();
        resultParty.setName(nameModel.getString());
        resultParty.setAddress(addressModel.getString());
        resultParty.setZipCode(zipCodeModel.getString());
        resultParty.setCity(cityModel.getString());
        resultParty.setEmailAddress(emailAddressModel.getString());
        resultParty.setBirthDate(birthDateModel.getDate());
        resultParty.setRemarks(taRemarks.getText());

        resultTags = new ArrayList<>();
        for (ListModel<String> tagListModel : tagListModels) {
            String tag = tagListModel.getSelectedItem();
            if (!Strings.isNullOrEmpty(tag)) {
                resultTags.add(tag);
            }
        }

        resulSepaDirectDebitSettings = new PartyDirectDebitSettings(resultParty.getId());
        resulSepaDirectDebitSettings.setName(sepaDirectDebitNameModel.getString());
        resulSepaDirectDebitSettings.setAddress(sepaDirectDebitAddressModel.getString());
        resulSepaDirectDebitSettings.setZipCode(sepaDirectDebitZipCodeModel.getString());
        resulSepaDirectDebitSettings.setCity(sepaDirectDebitCityModel.getString());
        resulSepaDirectDebitSettings.setCountry(sepaDirectDebitCountryModel.getString());
        resulSepaDirectDebitSettings.setIban(sepaDirectDebitIbanModel.getString());
        resulSepaDirectDebitSettings.setMandateDate(sepaDirectDebitMandateDateModel.getDate());

        requestClose();
    }

    /**
     * Gets the party that was entered by the user.
     * @return the party or <code>null</code> if the user canceled this dialog
     */
    public Party getEnteredParty() {
        return resultParty;
    }

    /**
     * Gets the tags for the party that was entered by the user.
     * @return the tags or <code>null</code> if the user canceled this dialog
     */
    public List<String> getEnteredTags() {
        return resultTags;
    }

    /**
     * Gets the settings for direct debit for a party as entered by the user.
     * @return the direct debit settings or <code>null</code> if the user canceled this dialog
     */
    public PartyDirectDebitSettings getEnteredSepaDirectDebitSettings() {
        return resulSepaDirectDebitSettings;
    }

    /**
     * Gets the title of this view.
     * @return the title of this view
     */
    @Override
	public String getTitle() {
        return textResource.getString(initialParty != null ? "editPartyView.titleEdit" : "editPartyView.titleAdd");
    }

	@Override
	public void onClose() {
        partyTagBeans.forEach(PartyTagBean::close);
    }

}
