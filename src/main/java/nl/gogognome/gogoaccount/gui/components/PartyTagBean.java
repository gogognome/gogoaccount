package nl.gogognome.gogoaccount.gui.components;

import nl.gogognome.gogoaccount.gui.views.HandleException;
import nl.gogognome.lib.gui.beans.Bean;
import nl.gogognome.lib.gui.beans.BeanFactory;
import nl.gogognome.lib.gui.beans.InputFieldsColumn;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.models.ListModel;
import nl.gogognome.lib.swing.models.StringModel;
import nl.gogognome.lib.swing.views.OkCancelView;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.util.Factory;
import nl.gogognome.lib.util.StringUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * Bean for selecting the type of a party. It also contains a button that allows the user
 * to add a new party type.
 */
public class PartyTagBean extends JPanel implements Bean {

    private static final long serialVersionUID = 1L;

    private final HandleException handleException;
    private final ListModel<String> types;
    private Bean comboboxBean;

    public PartyTagBean(ListModel<String> types, HandleException handleException) {
        this.types = types;
        this.handleException = handleException;
        initBean();
    }

    public void initBean() {
        setLayout(new GridBagLayout());
        BeanFactory beanFactory = Factory.getInstance(BeanFactory.class);
        comboboxBean = beanFactory.createComboBoxBean(types);

        add(comboboxBean.getComponent(), SwingUtils.createTextFieldGBConstraints(0, 0));
        WidgetFactory wf = Factory.getInstance(WidgetFactory.class);
        JButton button = wf.createIconButton("gen.btnNew", new NewPartyTypeAction(), 21);
        add(button);
    }

    @Override
    public JComponent getComponent() {
        return this;
    }

    @Override
    public void close() {
        comboboxBean.close();
    }

    @Override
    public boolean requestFocus(boolean temporary) {
        return comboboxBean.getComponent().requestFocus(temporary);
    }

    @Override
    public void requestFocus() {
        comboboxBean.getComponent().requestFocus();
    }

    @Override
    public boolean requestFocusInWindow() {
        return comboboxBean.getComponent().requestFocusInWindow();
    }

    private void showNewPartyTypeDialog() {
        handleException.of(() -> {
            NewPartyView view = new NewPartyView();
            ViewDialog dialog = new ViewDialog(this, view);
            dialog.showDialog();

            String newParty = view.getEnteredType();
            if (newParty != null) {
                types.addItem(newParty, null);
                types.setSelectedItem(newParty, null);
            }
        });
    }

    private final class NewPartyTypeAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            showNewPartyTypeDialog();
        }
    }

    private class NewPartyView extends OkCancelView {

        private StringModel newPartyTypeModel = new StringModel();

        private String enteredType;

        @Override
        public String getTitle() {
            return null;
        }

        @Override
        public void onClose() {
        }

        @Override
        public void onInit() {
            addComponents();
        }

        @Override
        protected JComponent createCenterComponent() {
            InputFieldsColumn ifc = new InputFieldsColumn();
            addCloseable(ifc);
            ifc.addField("PartyTypeBean.newPartyLabel", newPartyTypeModel, 20);
            return ifc;
        }

        @Override
        protected void onOk() {
            String newParty = newPartyTypeModel.getString();
            if (!StringUtil.isNullOrEmpty(newParty)) {
                enteredType = newParty;
                requestClose();
            }
        }

        public String getEnteredType() {
            return enteredType;
        }
    }
}
