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
package nl.gogognome.cf.tool;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;
import cf.engine.Database;
import cf.engine.Party;
import cf.engine.XMLFileReader;
import cf.engine.XMLParseException;

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
        TextResource tr = TextResource.getInstance();
        for (int i=0; i<args.length; i++) {
            if (args[i].startsWith("-lang=")) {
                tr.setLocale(new Locale(args[i].substring(6)));
            }
        }

        for (int i=0; i<args.length; i++) {
            if (args[i].startsWith("-xml-file=")) {
                if (xmlFileName != null) {
                    System.out.println(tr.getString("printAge.moreThanOneXmlFile"));
                    printUsage();
                    return;
                }
                xmlFileName = args[i].substring(10);
            }
        }

        if (xmlFileName == null) {
            System.out.println(tr.getString("printAge.noXmlFile"));
            printUsage();
            return;
        }

        ArrayList<Party> partiesWithBirthdate = new ArrayList<Party>();
        try {
            Database db = XMLFileReader.createDatabaseFromFile(xmlFileName);
            Party[] parties = db.getParties();
            for (int i = 0; i < parties.length; i++) {
                if (parties[i].getBirthDate() != null && !"oud-lid".equals(parties[i].getType())) {
                    partiesWithBirthdate.add(parties[i]);
                }
            }
        } catch (XMLParseException e) {
            System.out.println(tr.getString("printAge.syntaxError", e.getMessage()));
        } catch (IOException e) {
            System.out.println(tr.getString("printAge.ioError", e.getMessage()));
        }
        Collections.sort(partiesWithBirthdate, new Comparator<Party>() {
            public int compare(Party p1, Party p2) {
                int m1 = DateUtil.getField(p1.getBirthDate(), Calendar.MONTH);
                int d1 = DateUtil.getField(p1.getBirthDate(), Calendar.DATE);
                int m2 = DateUtil.getField(p2.getBirthDate(), Calendar.MONTH);
                int d2 = DateUtil.getField(p2.getBirthDate(), Calendar.DATE);
                if (m1 != m2) {
                    return m1 - m2;
                } else {
                    return d1 - d2;
                }
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

    private static void printUsage() {
        TextResource tr = TextResource.getInstance();
        for (int i=0; tr.getString("printAge.usage" + i) != null; i++) {
            System.out.println(tr.getString("printAge.usage" + i));
        }
    }
}
