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
package nl.gogognome.gogoaccount.tools;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.party.Party;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.XMLFileReader;
import nl.gogognome.gogoaccount.util.ObjectFactory;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.DateUtil;
import nl.gogognome.lib.util.Factory;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class implements a small application that prints a birthday calendar of all parties.
 */
public class BirthdayCalendarOfParties {

    /**
     * @param args should have size 1. The argument contains the XML file containing the
     *        parties.
     */
    public static void main(String[] args) {
        String xmlFileName = null;

        // First set the language so that the error messages caused by parsing the other
        // arguments are shown in the specified language.
        initFactory(Locale.getDefault());
        TextResource tr = Factory.getInstance(TextResource.class);
        for (String arg : args) {
            if (arg.startsWith("-lang=")) {
                initFactory(new Locale(arg.substring(6)));
            }
        }

        for (String arg : args) {
            if (arg.startsWith("-xml-file=")) {
                if (xmlFileName != null) {
                    System.out.println(tr.getString("printAge.moreThanOneXmlFile"));
                    printUsage();
                    return;
                }
                xmlFileName = arg.substring(10);
            }
        }

        if (xmlFileName == null) {
            System.out.println(tr.getString("printAge.noXmlFile"));
            printUsage();
            return;
        }

        List<Party> partiesWithBirthdate = new ArrayList<>();
        try {
            Document document = new XMLFileReader(new File(xmlFileName)).createDatabaseFromFile();
            List<Party> parties = ObjectFactory.create(PartyService.class).findAllParties(document);
            for (Party party : parties) {
                if (party.getBirthDate() != null) {
                    partiesWithBirthdate.add(party);
                }
            }
        } catch (ServiceException e) {
            System.out.println(tr.getString("gen.problemOccurred", e.getMessage()));
        }
        Collections.sort(partiesWithBirthdate, (p1, p2) -> {
            int m1 = DateUtil.getField(p1.getBirthDate(), Calendar.MONTH);
            int d1 = DateUtil.getField(p1.getBirthDate(), Calendar.DATE);
            int m2 = DateUtil.getField(p2.getBirthDate(), Calendar.MONTH);
            int d2 = DateUtil.getField(p2.getBirthDate(), Calendar.DATE);
            if (m1 != m2) {
                return m1 - m2;
            } else {
                return d1 - d2;
            }
        });

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMMMMM", new Locale("nl"));
        SimpleDateFormat dayOfMonthFormat = new SimpleDateFormat("d", new Locale("nl"));
        int prevMonth = -1;
        for (Party party : partiesWithBirthdate) {
            int month = DateUtil.getField(party.getBirthDate(), Calendar.MONTH);
            if (prevMonth != month) {
                System.out.println("\t\t\t\t<tr><td></td><td><br><b>" + monthFormat.format(party.getBirthDate()) + "</b></td></tr>");
            }
            System.out.print("\t\t\t\t<tr><td>");
            System.out.print(dayOfMonthFormat.format(party.getBirthDate()) + "</td><td>" + party.getName());
            System.out.println("</td></tr>");
            prevMonth = month;
        }
    }

	private static void initFactory(Locale locale) {
		TextResource tr = new TextResource(locale);
		tr.loadResourceBundle("stringresources");

		Factory.bindSingleton(TextResource.class, tr);
		Factory.bindSingleton(WidgetFactory.class, new WidgetFactory(tr));
	}

    private static void printUsage() {
        TextResource tr = Factory.getInstance(TextResource.class);
        for (int i=0; tr.getString("printAge.usage" + i) != null; i++) {
            System.out.println(tr.getString("printAge.usage" + i));
        }
    }
}
