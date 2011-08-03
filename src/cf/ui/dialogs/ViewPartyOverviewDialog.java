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
import java.awt.Dimension;
import java.awt.Frame;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import nl.gogognome.lib.swing.DialogWithButtons;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.text.TextResource;
import cf.engine.Database;
import cf.engine.Party;
import cf.ui.components.PartyOverviewTableModel;

/**
 * This class implements a dialog that shows the overview of a party
 * at a specific date.
 *
 * @author Sander Kooijmans
 */
public class ViewPartyOverviewDialog extends DialogWithButtons
{

    /**
	 * Creates a "Party overview" dialog.
	 * @param frame the frame that owns this dialog.
	 * @param party the party to be shown.
	 * @param date the date.
     */
    public ViewPartyOverviewDialog(Frame frame, Database database, Party party, Date date)
    {
		super(frame, "vpo.title", BT_OK);

		PartyOverviewTableModel model = new PartyOverviewTableModel(database, party, date);
		JTable table = WidgetFactory.getInstance().createTable(model);

		// Create panel with date and name of party.
		JLabel label = new JLabel();
		TextResource tr = TextResource.getInstance();
		label.setText(tr.getString("vpo.partyAtDate",
		        party.getId() + " - " + party.getName(),
		        tr.formatDate("gen.dateFormat", date)));

		// Create panel with label and table.
		JPanel panel = new JPanel(new BorderLayout());
		panel.add(label, BorderLayout.NORTH);
		panel.add(new JScrollPane(table), BorderLayout.CENTER);
		panel.setPreferredSize(new Dimension(800,600));

		componentInitialized(panel);
		setResizable(true);
    }

	/**
	 * Handles the cancel event. This method should not be called, since the cancel
	 * button is disabled.
	 */
	@Override
    protected void handleCancel()
	{
		// release resources
		handleButton(0);
	}

	/**
	 * Handles the OK event. This method hides the dialog and frees resources.
	 * @param index the index of the button.
	 */
	@Override
    protected void handleButton( int index )
	{
		// release resources
		hideDialog();
	}
}
