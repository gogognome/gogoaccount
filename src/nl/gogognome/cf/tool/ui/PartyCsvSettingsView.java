/*
 * $Id: PartyCsvSettingsView.java,v 1.1 2007-09-04 19:04:27 sanderk Exp $
 *
 * Copyright (C) 2007 Sander Kooijmans
 */
package nl.gogognome.cf.tool.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import nl.gogognome.csv.CsvFileParser;
import nl.gogognome.framework.View;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;

/**
 * This class implements a view that allows the user to change settings
 * for the <code>PartyCsvReader</code>. 
 *
 * @author Sander Kooijmans
 */
public class PartyCsvSettingsView extends View {

    /** The model of the table showing the CSV file. */ 
    private CsvTableModel tableModel;
    
    /** The values of the CSV file. */
    private String[][] values;
    
    private int firstPartyIndex = 0;
    private int lastPartyIndex = Integer.MAX_VALUE;
    
    /** The CSV file. */
    private File file;
    
    private JTextField tfOutputFormat;
    
    private JTextArea taSampleOutput;
    
    /** The parser used to parse the CSV file. */
    private CsvFileParser parser;

    private String idOkButton;
    private String idCancelButton;
    
    /** Contains the identifier of the button that was used to close this view. */
    private String idPressedButton;

    /**
     * @param dialog
     * @param file the CSV file to be read
     * @param idOkButton the identifier of the OK button
     * @param idCancelButton the identifier of the cancel button
     * @throws IOException if a problem occurs while reading the CSV file
     */
    public PartyCsvSettingsView(File file, String idOkButton, String idCancelButton) throws IOException {
        this.file = file;
        this.idOkButton = idOkButton;
        this.idCancelButton = idCancelButton;
        parser = new CsvFileParser(file);
        values = parser.getValues();
        lastPartyIndex = values.length - 1;
    }

    /**
     * Gets the identifier of the button that was used to close this view.
     * @return the identifier of the button
     */
    public String getIdPressedButton() {
        return idPressedButton;
    }

    /**
     * Creates the panel containing the dialog, except for the Ok and Cancel buttons,
     * which are supplied by the super class.
     * @return the panel containing the dialog
     */
    private Component createPanel() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        WidgetFactory wf = WidgetFactory.getInstance();
        
        // Create table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tableModel = new CsvTableModel();
        final JTable table = new JTable(tableModel);
        table.setDefaultRenderer(String.class, new ColoredStringRenderer());
        JScrollPane scrollPane = new JScrollPane(table);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(wf.createButton("partyCsvSettingsDlg.firstPartyBtn", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                firstPartyIndex = table.getSelectedRow();
                tableModel.fireTableDataChanged();
                updateSample();
            }
        }));
        buttonPanel.add(wf.createButton("partyCsvSettingsDlg.lastPartyBtn", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                lastPartyIndex = table.getSelectedRow();
                tableModel.fireTableDataChanged();
                updateSample();
            }
        }));
        tablePanel.add(buttonPanel, BorderLayout.SOUTH);
        tablePanel.setBorder(new CompoundBorder(
                new TitledBorder(TextResource.getInstance().getString("partyCsvSettingsDlg.tableBorderTitle")),
                new EmptyBorder(5, 10, 0, 10)));
        
        // Create output format panel
        JPanel outputFormatPanel = new JPanel(new GridBagLayout());
        outputFormatPanel.add(wf.createLabel("partyCsvSettingsDlg.format"),
                SwingUtils.createLabelGBConstraints(0, 0));
        tfOutputFormat = new JTextField("{0} {1}\\n{2}\\n{3}");
        tfOutputFormat.addFocusListener(new FocusAdapter() {
            public void focusLost(FocusEvent e) {
                updateSample();
            }
        });
        outputFormatPanel.add(tfOutputFormat,
                SwingUtils.createTextFieldGBConstraints(1, 0));
        
        outputFormatPanel.add(wf.createLabel("partyCsvSettingsDlg.sample"),
                SwingUtils.createGBConstraints(0, 1, 1, 1, 0.0, 0.0, 
                        GridBagConstraints.NORTHWEST, GridBagConstraints.NONE,
                        5, 0, 0, 0));
        taSampleOutput = new JTextArea(4, 40);
        taSampleOutput.setEditable(false);
        outputFormatPanel.add(taSampleOutput,
                SwingUtils.createGBConstraints(1, 1, 1, 1, 1.0, 1.0, 
                        GridBagConstraints.WEST, GridBagConstraints.BOTH,
                        5, 0, 0, 0));
        updateSample();
        
        outputFormatPanel.setBorder(new CompoundBorder(
                new TitledBorder(TextResource.getInstance().getString("partyCsvSettingsDlg.outputFormatBorderTitle")),
                new EmptyBorder(5, 10, 0, 10)));

        // Create panel with ok and cancel buttons
        buttonPanel = new JPanel(new FlowLayout());
        JButton okButton = wf.createButton(idOkButton, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                idPressedButton = idOkButton;
                closeAction.actionPerformed(e);
            }
        });
        buttonPanel.add(okButton);
        JButton cancelButton = wf.createButton(idCancelButton, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                idPressedButton = idCancelButton;
                closeAction.actionPerformed(e);
            }
        });
        buttonPanel.add(cancelButton);
        
        // Put all panels on the main panel
        mainPanel.add(tablePanel, 
                SwingUtils.createPanelGBConstraints(0, 0));
        mainPanel.add(outputFormatPanel, 
                SwingUtils.createPanelGBConstraints(0, 1));
        mainPanel.add(buttonPanel, 
            SwingUtils.createPanelGBConstraints(0, 2));

        return mainPanel;
    }
    
    private void updateSample() {
        String format = tfOutputFormat.getText();
        String sample;
        int index = (firstPartyIndex + lastPartyIndex) / 2;
        if (index < values.length) {
            sample = CsvFileParser.composeValue(format, values[index]);
        } else {
            sample = TextResource.getInstance().getString("partyCsvSettingsDlg.emptySelection");
        }
        sample = sample.replaceAll("\\\\n", "\n");
        
        taSampleOutput.setText(sample);
    }
    
    public int getFirstPartyIndex() {
        return firstPartyIndex;
    }
    
    public int getLastPartyIndex() {
        return lastPartyIndex;
    }
    
    public String getOutputFormat() {
        return tfOutputFormat.getText();
    }
    
    /**
     * Gets the parser as it is configured by the user. This method should
     * only be called if the user exited the dialog by pressing the Ok button.
     * @return the parser
     */
    public CsvFileParser getParser() {
        parser.setNrFirstLine(firstPartyIndex);
        parser.setNrLastLine(lastPartyIndex);
        return parser;
    }
    
    private class CsvTableModel extends AbstractTableModel {

        private int nrColumns;
        
        public CsvTableModel() {
            nrColumns = 0;
            for (int i = 0; i < values.length; i++) {
                nrColumns = Math.max(nrColumns, values[i].length);
            }
        }
        
        public Class getColumnClass(int columnIndex) {
            return String.class;
        }
        
        public String getColumnName(int column) {
            return Integer.toString(column);
        }
        
        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getColumnCount()
         */
        public int getColumnCount() {
            return nrColumns;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getRowCount()
         */
        public int getRowCount() {
            return values.length;
        }

        /* (non-Javadoc)
         * @see javax.swing.table.TableModel#getValueAt(int, int)
         */
        public Object getValueAt(int row, int col) {
            if (col < values[row].length) {
                return values[row][col];
            } else {
                return "";
            }
        }
        
    }
    
    private class ColoredStringRenderer extends DefaultTableCellRenderer {

        /* (non-Javadoc)
         * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object, boolean, boolean, int, int)
         */
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            Component result = super.getTableCellRendererComponent(table, value, isSelected, 
                    hasFocus, row, column);
            if (firstPartyIndex <= row && row <= lastPartyIndex) {
                result.setBackground(Color.CYAN);
            } else {
                result.setBackground(Color.WHITE);
            }
            return result;
        }
        
    }

    public String getTitle() {
        return TextResource.getInstance().getString("partyCsvSettingsDlg.title");
    }

    public void onClose() {
        // TODO Auto-generated method stub
        
    }

    public void onInit() {
        add(createPanel());
    }
}
