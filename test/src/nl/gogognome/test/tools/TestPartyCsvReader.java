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
package nl.gogognome.test.tools;

import java.awt.print.PrinterException;
import java.io.IOException;

import nl.gogognome.lib.csv.CsvFileParser;
import nl.gogognome.lib.csv.ui.CsvParseSettingsView;
import nl.gogognome.lib.print.LabelPrinter;
import nl.gogognome.lib.print.TextLabel;
import nl.gogognome.lib.print.ui.SimpleLabelSheetSetupView;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewContainer;
import nl.gogognome.lib.swing.views.ViewFrame;
import nl.gogognome.lib.swing.views.ViewListener;

/**
 * Tests the class <code>PartyCsvReader</code>.
 *
 * @author Sander Kooijmans
 */
public class TestPartyCsvReader {

    static ViewFrame frame;
    static ViewContainer viewContainer;
    static CsvParseSettingsView pcsView;
    static SimpleLabelSheetSetupView labelSheetSetupView;
    static TextLabel[] labels;

    public static void main(String[] args) {
        viewContainer = new ViewContainer("Party label printer");
        frame = new ViewFrame(viewContainer);
        pcsView = new CsvParseSettingsView("gen.continue", "gen.cancel");
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
