/*
 * $RCSfile: CloseBookkeepingView.java,v $
 * Copyright (c) PharmaPartners BV
 */

package cf.ui.views;

import cf.engine.Database;
import nl.gogognome.framework.ValuesEditPanel;
import nl.gogognome.framework.View;
import nl.gogognome.framework.models.DateModel;
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

    private DateModel dateModel = new DateModel();

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
    }

    /** {@inheritDoc} */
    @Override
    public void onInit() {
        dateModel.setDate(DateUtil.addYears(database.getStartOfPeriod(), 1), null);

        ValuesEditPanel vep = new ValuesEditPanel();
        vep.addField("closeBookkeepingView.date", dateModel);
//        vep.addField(labelId, model)
    }

}
