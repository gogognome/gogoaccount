/*
 * $RCSfile: CloseBookkeepingView.java,v $
 * Copyright (c) PharmaPartners BV
 */

package cf.ui.views;

import java.awt.GridBagLayout;

import cf.engine.Account;
import cf.engine.Database;
import cf.ui.components.AccountComboBox;
import java.awt.event.ActionEvent;
import java.util.Calendar;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.SwingConstants;
import nl.gogognome.framework.ValuesEditPanel;
import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.framework.models.StringModel;
import nl.gogognome.swing.ButtonPanel;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;


/**
 * This class implements a view that asks the user to enter some data that is needed
 * to closse the bookkeeping.
 *
 * @author Sander Kooijmans
 */
public class CloseBookkeepingView extends View {

    /** The database whose bookkeeping is to be closed. */
    private Database database;

    /** Date model for the closing date. */
    private DateModel dateModel = new DateModel();

    /** String model for the description. */
    private StringModel descriptionModel = new StringModel();
    
    /** Combo box for the account to which the result of the bookkeeping is added. */
    private AccountComboBox accountComboBox;

    /** The value edit panel containig the input fields. */
    private ValuesEditPanel valuesEditPanel;

    /** Indicates whether the user entered data successfully and selected the Ok action. */
    private boolean dataSuccessfullyEntered;

    /**
     * Constructor.
     * @param database the database whose bookkeeping is to be closed
     */
    public CloseBookkeepingView(Database database) {
        this.database = database;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle() {
        return TextResource.getInstance().getString("closeBookkeepingView.title");
    }

    /** {@inheritDoc} */
    @Override
    public void onClose() {
        valuesEditPanel.deinitialize();
    }

    /** {@inheritDoc} */
    @Override
    public void onInit() {
        dateModel.setDate(DateUtil.addYears(database.getStartOfPeriod(), 1), null);

        valuesEditPanel = new ValuesEditPanel();
        valuesEditPanel.addField("closeBookkeepingView.date", dateModel);
        accountComboBox = new AccountComboBox(database);
        valuesEditPanel.addField("closeBookkeepingView.equityAccount", accountComboBox);

        initDescription();
        valuesEditPanel.addField("closeBookkeepingView.description", descriptionModel);
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.LEFT);
        WidgetFactory wf = WidgetFactory.getInstance();
        buttonPanel.add(wf.createButton("gen.ok", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                onOk();
            }
        }));

        buttonPanel.add(wf.createButton("gen.cancel", closeAction));

        // Add the panels to the view.
        setLayout(new GridBagLayout());
        add(valuesEditPanel, SwingUtils.createPanelGBConstraints(0, 0));
        add(buttonPanel, SwingUtils.createPanelGBConstraints(0, 1));
    }

    private void onOk() {
        if (dateModel.getDate() == null) {
            MessageDialog.showMessage(this, "gen.warning",
                TextResource.getInstance().getString("closeBookkeepingView.enterDate"));
            return;
        }

        if (accountComboBox.getSelectedAccount() == null) {
            MessageDialog.showMessage(this, "gen.warning",
                TextResource.getInstance().getString("closeBookkeepingView.selectAccount"));
            return;
        }

        dataSuccessfullyEntered = true;
        closeAction.actionPerformed(null);
    }

    /**
     * Gets the closing date of the bookkeeping.
     * @return the closing date
     */
    public Date getDate() {
        if (dataSuccessfullyEntered) {
            return dateModel.getDate();
        } else {
            return null;
        }
    }

    /**
     * Gets the account to which the result of the bookkeeping is added.
     * @return the account
     */
    public Account getAccountToAddResultTo() {
        if (dataSuccessfullyEntered) {
            return accountComboBox.getSelectedAccount();
        } else {
            return null;
        }
    }

    /** 
     * Gets the description of the new bookkeeping.
     * @return the description
     */
    public String getDescription() {
        return descriptionModel.getString();
    }
    
    /** Initializes the description model with a default value. */
    private void initDescription() {
        String description = database.getDescription();
        int year = DateUtil.getField(database.getStartOfPeriod(), Calendar.YEAR);
        int nextYear = year+1;
        description = description.replace(Integer.toString(year), Integer.toString(nextYear));
        descriptionModel.setString(description, null);
    }
}
