/*
 * $Id: PrintAgeOfParties.java,v 1.3 2008-01-10 19:18:08 sanderk Exp $
 *
 * Copyright (C) 2005 Sander Kooijmans
 *
 */

package nl.gogognome.cf.tool;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;

import cf.engine.Database;
import cf.engine.XMLParseException;
import cf.engine.Party;
import cf.engine.XMLFileReader;

/**
 * This class implements a small application that prints the age of all parties on a specified date.
 * This is useful information when determining the amount of contribution each party has to pay to
 * the club.
 */
public class PrintAgeOfParties {

    /**
     * @param args should have size 1. The argument contains the XML file containing the
     *        parties.
     */
    public static void main(String[] args) {
        String xmlFileName = null;
        String dateString = null;
        
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
            } else if (args[i].startsWith("-date=")) {
                if (dateString != null) {
                    System.out.println(tr.getString("printAge.moreThanOneDate"));
                    printUsage();
                    return;
                }
                dateString = args[i].substring(6);
            }
        }

        if (xmlFileName == null) {
            System.out.println(tr.getString("printAge.noXmlFile"));
            printUsage();
            return;
        } else if (dateString == null) {
            System.out.println(tr.getString("printAge.noDate"));
            printUsage();
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat(TextResource.getInstance().getString("gen.dateFormat"));
        Date date;
        try {
            date = sdf.parse(dateString);
        } catch (java.text.ParseException e1) {
            System.out.println("Invalid date: " + dateString);
            printUsage();
            return;
        }
        
        try {
            Database db = XMLFileReader.createDatabaseFromFile(xmlFileName);
            Party[] parties = db.getParties();
            for (int i = 0; i < parties.length; i++) {
                Date birthDay = parties[i].getBirthDate();
                System.out.print(parties[i].getId() + " - " + parties[i].getName() + ": ");
                if (birthDay != null) {
                    System.out.println(DateUtil.getDifferenceInYears(date, birthDay));
                } else {
                    System.out.println(tr.getString("printAge.unknownBirthDay"));
                }
            }
        } catch (XMLParseException e) {
            System.out.println(tr.getString("printAge.syntaxError", e.getMessage()));
        } catch (IOException e) {
            System.out.println(tr.getString("printAge.ioError", e.getMessage()));
        } 
    }

    private static void printUsage() {
        TextResource tr = TextResource.getInstance();
        for (int i=0; tr.getString("printAge.usage" + i) != null; i++) {
            System.out.println(tr.getString("printAge.usage" + i));
        }
    }
}
