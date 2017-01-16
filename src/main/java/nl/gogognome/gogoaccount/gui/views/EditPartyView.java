package nl.gogognome.gogoaccount.gui.views;

import com.google.common.base.Strings;
import nl.gogognome.gogoaccount.component.automaticcollection.PartyAutomaticCollectionSettings;
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
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private final Logger logger = LoggerFactory.getLogger(EditPartyView.class);

    private final Document document;
    private final ConfigurationService configurationService;
    private final PartyService partyService;
    private final MessageDialog messageDialog;
    private final HandleException handleException;
    private List<String> tags;

    private final StringModel idModel = new StringModel();
    private final StringModel nameModel = new StringModel();
    private final StringModel addressModel = new StringModel();
    private final StringModel zipCodeModel = new StringModel();
    private final StringModel cityModel = new StringModel();
    private final StringModel emailAddressModel = new StringModel();
    private final StringModel automaticCollectionNameModel = new StringModel();
    private final StringModel automaticCollectionAddressModel = new StringModel();
    private final StringModel automaticCollectionZipCodeModel = new StringModel();
    private final StringModel automaticCollectionCityModel = new StringModel();
    private final StringModel automaticCollectionCountryModel = new StringModel();
    private final StringModel automaticCollectionIbanModel = new StringModel();
    private final DateModel automaticCollectionMandateDateModel = new DateModel();
    private final List<ListModel<String>> tagListModels = new ArrayList<>();
    private final DateModel birthDateModel = new DateModel();
    private JTextField lbIdRemark = new JTextField(); // text field 'misused' as text label
    private final JTextArea taRemarks = new JTextArea(5, 40);
    private JPanel labelsPanel = new JPanel(new GridBagLayout());
    private final List<PartyTagBean> partyTagBeans = new ArrayList<>();

    private Party initialParty;
    private List<String> initialTags;
    private PartyAutomaticCollectionSettings initialAutomaticCollectionSettings;
    private Party resultParty;
    private List<String> resultTags;
    private PartyAutomaticCollectionSettings resulAutomaticCollectionSettings;

    private ModelChangeListener idUpdateListener;

    public EditPartyView(Document document, ConfigurationService configurationService, PartyService partyService) {
        this.document = document;
        this.configurationService = configurationService;
        this.partyService = partyService;
        messageDialog = new MessageDialog(textResource, this);
        handleException = new HandleException(messageDialog);
    }

    public void setInitialParty(Party party, List<String> initialTags,
                                PartyAutomaticCollectionSettings automaticCollectionSettings) {
        this.initialParty = party;
        this.initialTags = initialTags;
        this.initialAutomaticCollectionSettings = automaticCollectionSettings;
    }

    @Override
	public void onInit() {
        try {
            initModels();
            addComponents();
            addListeners();
            updateIdMessage();
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
            idModel.setString(initialParty.getId());
            idModel.setEnabled(false, null);
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
            idModel.setString(suggestNewId());
            tagListModels.add(new ListModel<>(tags)); // add one empty tag input field to start with
        }

        if (initialAutomaticCollectionSettings != null) {
            automaticCollectionNameModel.setString(initialAutomaticCollectionSettings.getName());
            automaticCollectionAddressModel.setString(initialAutomaticCollectionSettings.getAddress());
            automaticCollectionZipCodeModel.setString(initialAutomaticCollectionSettings.getZipCode());
            automaticCollectionCityModel.setString(initialAutomaticCollectionSettings.getCity());
            automaticCollectionCountryModel.setString(initialAutomaticCollectionSettings.getCountry());
            automaticCollectionIbanModel.setString(initialAutomaticCollectionSettings.getIban());
            automaticCollectionMandateDateModel.setDate(initialAutomaticCollectionSettings.getMandateDate());
        }
	}

    @Override
    protected JComponent createCenterComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        InputFieldsColumn ifc = new InputFieldsColumn();
        ifc.setBorder(widgetFactory.createTitleBorderWithPadding("editPartyView.generalInformation"));
        addCloseable(ifc);

        ifc.addField("editPartyView.id", idModel);
        lbIdRemark = new JTextField(20);
        lbIdRemark.setEditable(false);
        lbIdRemark.setEnabled(false);
        lbIdRemark.setBorder(null);
        ifc.add(lbIdRemark, SwingUtils.createTextFieldGBConstraints(2, 0));

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
            if (configurationService.getBookkeeping(document).isEnableAutomaticCollection()) {
                InputFieldsColumn automaticCollectionFields = new InputFieldsColumn();
                automaticCollectionFields.setBorder(widgetFactory.createTitleBorderWithPadding("editPartyView.automaticCollectionInformation"));
                automaticCollectionFields.addField("editPartyView.autoCollectionName", automaticCollectionNameModel);
                automaticCollectionFields.addField("editPartyView.autoCollectionAddress", automaticCollectionAddressModel);
                automaticCollectionFields.addField("editPartyView.autoCollectionZipCode", automaticCollectionZipCodeModel);
                automaticCollectionFields.addField("editPartyView.autoCollectionCity", automaticCollectionCityModel);
                automaticCollectionFields.addField("editPartyView.autoCollectionCountry", automaticCollectionCountryModel);
                automaticCollectionFields.addField("editPartyView.autoCollectionIban", automaticCollectionIbanModel);
                automaticCollectionFields.addField("editPartyView.autoCollectionMandateDate", automaticCollectionMandateDateModel);

                panel.add(automaticCollectionFields, BorderLayout.SOUTH);
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

    private void addListeners() {
        idUpdateListener = model -> updateIdMessage();
        idModel.addModelChangeListener(idUpdateListener);
    }

    @Override
    protected void onOk() {
        resultParty = new Party(idModel.getString());
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

        resulAutomaticCollectionSettings = new PartyAutomaticCollectionSettings(resultParty.getId());
        resulAutomaticCollectionSettings.setName(automaticCollectionNameModel.getString());
        resulAutomaticCollectionSettings.setAddress(automaticCollectionAddressModel.getString());
        resulAutomaticCollectionSettings.setZipCode(automaticCollectionZipCodeModel.getString());
        resulAutomaticCollectionSettings.setCity(automaticCollectionCityModel.getString());
        resulAutomaticCollectionSettings.setCountry(automaticCollectionCountryModel.getString());
        resulAutomaticCollectionSettings.setIban(automaticCollectionIbanModel.getString());
        resulAutomaticCollectionSettings.setMandateDate(automaticCollectionMandateDateModel.getDate());

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
     * Gets the settings for automatic collection for a party as entered by the user.
     * @return the automatic settings or <code>null</code> if the user canceled this dialog
     */
    public PartyAutomaticCollectionSettings getEnteredAutomaticCollectionSettings() {
        return resulAutomaticCollectionSettings;
    }

    /**
     * Gets the title of this view.
     * @return the title of this view
     */
    @Override
	public String getTitle() {
        return textResource.getString(initialParty != null ? "editPartyView.titleEdit" : "editPartyView.titleAdd");
    }

    private void updateIdMessage() {
        try {
            String remark = "";
            String id = idModel.getString();
            if (initialParty == null && partyService.existsParty(document, id)) {
                remark = textResource.getString("editPartyView.idExistsAlready");
            } else if (id.length() == 0) {
                remark = textResource.getString("editPartyView.idIsEmpty");
            }
            lbIdRemark.setText(remark);
        } catch (ServiceException e) {
            logger.warn("Ignored exception", e);
        }
    }

    private String suggestNewId() throws ServiceException {
        List<Party> parties = partyService.findAllParties(document);

        String suggestion = null;
        for (Party party : parties) {
            if (suggestion == null) {
                suggestion = party.getId();
            } else {
                if (suggestion.compareTo(party.getId()) < 0) {
                    suggestion = party.getId();
                }
            }
        }
        // If suggestion != null, then it contains the largest ID according to the
        // lexicographically order. Increase the ID to get a unique ID.
        if (suggestion != null) {
            StringBuilder sb = new StringBuilder(suggestion);
            int index = sb.length() - 1;
            boolean done = false;
            while (!done && index >= 0) {
                char c = (char) (sb.charAt(index) + 1);
                if (c == (char)('9' + 1)) {
                    c = '0';
                } else if (c == (char)('z' + 1)) {
                    c = 'a';
                } else if (c == (char)('Z' + 1)) {
                    c = 'A';
                } else {
                    done = true;
                }
                sb.setCharAt(index, c);
                index--;
            }
            if (!done) {
                sb.insert(0, '1');
            }
            suggestion = sb.toString();
        } else {
            suggestion = "001";
        }
        return suggestion;
    }

	@Override
	public void onClose() {
        partyTagBeans.forEach(PartyTagBean::close);
        removeListeners();
    }

	private void removeListeners() {
		idModel.removeModelChangeListener(idUpdateListener);
	}

}
