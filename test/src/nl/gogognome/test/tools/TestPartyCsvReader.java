/*
 * $Id: TestPartyCsvReader.java,v 1.1 2007-07-24 18:38:57 sanderk Exp $
 *
 * Copyright (C) 2007 Sander Kooijmans
 */
package nl.gogognome.test.tools;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JFrame;

import nl.gogognome.cf.tool.PartyCsvReader;
import nl.gogognome.cf.tool.ui.PartyCsvSettingsDialog;
import nl.gogognome.csv.CsvFileParser;
import cf.engine.Party;

/**
 * Tests the class <code>PartyCsvReader</code>.
 *
 * @author Sander Kooijmans
 */
public class TestPartyCsvReader {

    public static void main(String[] args) throws IOException {
        String fileName = args[0];
        final JFrame frame = new JFrame(TestPartyCsvReader.class.getName());
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
               frame.dispose(); 
            }
        });
        frame.show();
        PartyCsvSettingsDialog pcsDialog = new PartyCsvSettingsDialog(frame, new File(fileName));
        pcsDialog.showDialog();
        
        CsvFileParser parser = new CsvFileParser(new File(fileName));
        PartyCsvReader pcr = new  PartyCsvReader(parser.getValues());
        pcr.setNrFirstLine(2);
        pcr.setNrLastLine(36);
        Party[] parties = pcr.getParties();
        for (int i = 0; i < parties.length; i++) {
            System.out.println(parties[i]);
        }
    }
}
