/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package cf.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import nl.gogognome.lib.swing.DialogWithButtons;
import nl.gogognome.lib.swing.models.DateModel;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import cf.engine.Database;
import cf.ui.components.PartiesOverviewTableModel;

/**
 * This class implements a dialog which shows the overview of all parties
 * at a specified date.
 *
 * @author Sander Kooijmans
 */
public class ViewPartiesOverviewDialog extends DialogWithButtons
{

    /**
     * Constructor.
     * @param parent the parent frame of this dialog
     */
    public ViewPartiesOverviewDialog(Frame parent, Date date)
    {
        super(parent, "vpos.title", DialogWithButtons.BT_OK);

        JPanel panel = new JPanel(new BorderLayout());

        TextResource tr = Factory.getInstance(TextResource.class);
        String s = tr.getString("vpos.overviewOfPartiesAt",
                new Object[] {tr.formatDate("gen.dateFormat", date)});
        panel.add(new JLabel(s), BorderLayout.NORTH);

        // TODO: Move datemodel and database to parameters of this constructor.
        DateModel dateModel = new DateModel();
        dateModel.setDate(date, null);
        JTable table = new JTable(new PartiesOverviewTableModel(Database.getInstance(), dateModel));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        componentInitialized(panel);
		setResizable(true);
    }
}
