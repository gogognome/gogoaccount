/*
 * $Id: TestPartyCsvReader.java,v 1.2 2007-09-04 19:05:39 sanderk Exp $
 *
 * Copyright (C) 2007 Sander Kooijmans
 */
package nl.gogognome.test.tools;

import java.awt.print.PrinterException;
import java.io.File;
import java.io.IOException;

import nl.gogognome.cf.tool.ui.PartyCsvSettingsView;
import nl.gogognome.csv.CsvFileParser;
import nl.gogognome.framework.View;
import nl.gogognome.framework.ViewContainer;
import nl.gogognome.framework.ViewFrame;
import nl.gogognome.framework.ViewListener;
import nl.gogognome.print.LabelPrinter;
import nl.gogognome.print.TextLabel;
import nl.gogognome.print.ui.SimpleLabelSheetSetupView;

/**
 * Tests the class <code>PartyCsvReader</code>.
 *
 * @author Sander Kooijmans
 */
public class TestPartyCsvReader {

    static ViewFrame frame;
    static ViewContainer viewContainer;
    static PartyCsvSettingsView pcsView;
    static SimpleLabelSheetSetupView labelSheetSetupView;
    static TextLabel[] labels;
    
    public static void main(String[] args) throws IOException, PrinterException {
        String fileName = args[0];
        viewContainer = new ViewContainer("Party label printer");
        frame = new ViewFrame(viewContainer);
        pcsView = new PartyCsvSettingsView(new File(fileName),
            "gen.continue", "gen.cancel");
        pcsView.addViewListener(new ViewListener() {
            public void onViewClosed(View view) {
                onClosePartyCsvSettingsView();
            }
        });
        viewContainer.setView(pcsView);
        frame.showFrame();
    }
    
    private static void onClosePartyCsvSettingsView() {
        if ("gen.cancel".equals(pcsView.getIdPressedButton())) {
            frame.dispose();
            return;
        }
        
        CsvFileParser parser = pcsView.getParser();
        String[] labelTexts;
        try {
            labelTexts = parser.getFormattedValues(pcsView.getOutputFormat());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        labels = new TextLabel[labelTexts.length];
        for (int i = 0; i < labels.length; i++) {
            labels[i] = new TextLabel(labelTexts[i]);
        }
        labelSheetSetupView = new SimpleLabelSheetSetupView(labels, "gen.finish", "gen.cancel");

        labelSheetSetupView.addViewListener(new ViewListener() {
            public void onViewClosed(View view) {
                onCloseLabelSheetSetupView();
            }
        });
        viewContainer.setView(labelSheetSetupView);
    }
    
    private static void onCloseLabelSheetSetupView() {
        frame.dispose();
        if ("gen.cancel".equals(labelSheetSetupView.getIdPressedButton())) {
            return;
        }
        try {
            LabelPrinter.printLabels(labels, labelSheetSetupView.getResultingLabelSheets());
        } catch (PrinterException e) {
            e.printStackTrace();
        }
    }
}
