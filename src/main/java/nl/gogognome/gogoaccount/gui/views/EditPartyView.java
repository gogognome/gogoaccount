package nl.gogognome.gogoaccount.gui.views;

import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.document.Document;
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
    private final ListModel<String> typeListModel = new ListModel<>();
    private final DateModel birthDateModel = new DateModel();
    private JTextField lbIdRemark = new JTextField(); // text field 'misused' as text label
    private final JTextArea taRemarks = new JTextArea(5, 30);

    private final Party initialParty;
    private Party resultParty;

    private ModelChangeListener idUpdateListener;

    /**
     * Constructor.
     * @param party the party used to initialize the view
     */
    protected EditPartyView(Document document, Party party) {
        super();
        this.document = document;
        this.initialParty = party;
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
