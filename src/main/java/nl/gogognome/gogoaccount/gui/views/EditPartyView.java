package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.automaticcollection.PartyAutomaticCollectionSettings;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.components.PartyTypeBean;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.ModelChangeListener;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * This class implements a view to edit a party. Either an existing party or
 * a new party can be edited.
 */
public class EditPartyView extends OkCancelView {

	private static final long serialVersionUID = 1L;

    private final PartyService partyService = ObjectFactory.create(PartyService.class);
    private final Logger logger = LoggerFactory.getLogger(EditPartyView.class);

    private final Document document;

    private final StringModel idModel = new StringModel();
    private final StringModel nameModel = new StringModel();
    private final StringModel addressModel = new StringModel();
    private final StringModel zipCodeModel = new StringModel();
    private final StringModel cityModel = new StringModel();
    private final StringModel automaticCollectionNameModel = new StringModel();
    private final StringModel automaticCollectionAddressModel = new StringModel();
    private final StringModel automaticCollectionZipCodeModel = new StringModel();
    private final StringModel automaticCollectionCityModel = new StringModel();
    private final StringModel automaticCollectionCountryModel = new StringModel();
    private final StringModel automaticCollectionIbanModel = new StringModel();
    private final DateModel automaticCollectionMandateDateModel = new DateModel();
    private final ListModel<String> typeListModel = new ListModel<>();
    private final DateModel birthDateModel = new DateModel();
    private JTextField lbIdRemark = new JTextField(); // text field 'misused' as text label
    private final JTextArea taRemarks = new JTextArea(5, 30);

    private final Party initialParty;
    private final PartyAutomaticCollectionSettings initialAutomaticCollectionSettings;
    private Party resultParty;
    private PartyAutomaticCollectionSettings resulAutomaticCollectionSettings;

    private ModelChangeListener idUpdateListener;

    protected EditPartyView(Document document, Party party, PartyAutomaticCollectionSettings automaticCollectionSettings) {
        super();
        this.document = document;
        this.initialParty = party;
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
            MessageDialog.showErrorMessage(this, e, "gen.problemOccurred");
            close();
        }
    }

    private void initModels() throws ServiceException {
    	List<String> items = new ArrayList<>();
    	items.add("");
    	items.addAll(partyService.findPartyTypes(document));
    	typeListModel.setItems(items);

    	if (initialParty != null) {
            idModel.setString(initialParty.getId());
            idModel.setEnabled(false, null);
            nameModel.setString(initialParty.getName());
            addressModel.setString(initialParty.getAddress());
            zipCodeModel.setString(initialParty.getZipCode());
            cityModel.setString(initialParty.getCity());
            typeListModel.setSelectedItem(initialParty.getType(), null);
            taRemarks.setText(initialParty.getRemarks());
            birthDateModel.setDate(initialParty.getBirthDate(), null);
        } else {
            idModel.setString(suggestNewId());
        }

        if (initialAutomaticCollectionSettings != null) {
            automaticCollectionNameModel.setString(initialAutomaticCollectionSettings.getName());
            automaticCollectionAddressModel.setString(initialAutomaticCollectionSettings.getAddress());
            automaticCollectionZipCodeModel.setString(initialAutomaticCollectionSettings.getZipCode());
            automaticCollectionCityModel.setString(initialAutomaticCollectionSettings.getCity());
            automaticCollectionCountryModel.setString(initialAutomaticCollectionSettings.getCountry());
            automaticCollectionIbanModel.setString(initialAutomaticCollectionSettings.getIban());
            automaticCollectionMandateDateModel.setDate(initialAutomaticCollectionSettings.getMandateDate());
        } else if (initialParty != null) {
            automaticCollectionNameModel.setString(initialParty.getName());
            automaticCollectionAddressModel.setString(initialParty.getAddress());
            automaticCollectionZipCodeModel.setString(initialParty.getZipCode());
            automaticCollectionCityModel.setString(initialParty.getCity());
            automaticCollectionCountryModel.setString(ObjectFactory.create(ConfigurationService.class).getBookkeeping(document).getOrganizationCountry());
            automaticCollectionMandateDateModel.setDate(new Date());
        }
	}

    @Override
    protected JComponent createCenterComponent() {
        InputFieldsColumn ifc = new InputFieldsColumn();
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
        ifc.addField("editPartyView.birthDate", birthDateModel);
        PartyTypeBean typesBean = new PartyTypeBean(typeListModel);
        ifc.addVariableSizeField("editPartyView.type", typesBean);

        ifc.addVariableSizeField("editPartyView.remarks", taRemarks);

        ifc.addField("editPartyView.autoCollectionName", automaticCollectionNameModel);
        ifc.addField("editPartyView.autoCollectionAddress", automaticCollectionAddressModel);
        ifc.addField("editPartyView.autoCollectionZipCode", automaticCollectionZipCodeModel);
        ifc.addField("editPartyView.autoCollectionCity", automaticCollectionCityModel);
        ifc.addField("editPartyView.autoCollectionCountry", automaticCollectionCountryModel);
        ifc.addField("editPartyView.autoCollectionIban", automaticCollectionIbanModel);
        ifc.addField("editPartyView.autoCollectionMandateDate", automaticCollectionMandateDateModel);

        return ifc;
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
        resultParty.setBirthDate(birthDateModel.getDate());
        resultParty.setType(typeListModel.getSelectedItem());
        resultParty.setRemarks(taRemarks.getText());

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
        removeListeners();
    }

	private void removeListeners() {
		idModel.removeModelChangeListener(idUpdateListener);
	}

}
