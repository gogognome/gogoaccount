/*
 * $Id: PartyCsvSettingsView.java,v 1.3 2007-12-03 20:31:29 sanderk Exp $
 *
 * Copyright (C) 2007 Sander Kooijmans
 */
package nl.gogognome.cf.tool.ui;

import java.awt.BorderLayout;
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
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;

import nl.gogognome.csv.CsvFileParser;
import nl.gogognome.framework.View;
import nl.gogognome.swing.ButtonPanel;
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
    
    /** The table showing the CSV file. */
    private JTable table;
    
    /** The values of the CSV file. Must never be <code>null</code>. */
    private String[][] values = new String[0][0];

    /** The CSV file. */
    private File file;
    
    private JTextField tfFileName;

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
     */
    public PartyCsvSettingsView(String idOkButton, String idCancelButton) {
        this.idOkButton = idOkButton;
        this.idCancelButton = idCancelButton;
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
        
        // Create file panel
        JPanel filePanel = new JPanel(new GridBagLayout());
        filePanel.add(wf.createLabel("partyCsvSettingsDlg.filename"),
            SwingUtils.createLabelGBConstraints(0, 0));

        tfFileName = wf.createTextField(30);
        filePanel.add(tfFileName, SwingUtils.createTextFieldGBConstraints(1, 0));

        JButton button = wf.createButton("gen.btSelectFile", new AbstractAction() { 
            public void actionPerformed(ActionEvent event) {
                onChoseFile();
            }
        });
        filePanel.add(button, SwingUtils.createGBConstraints(2, 0, 1, 1, 0.0, 0.0, 
            GridBagConstraints.CENTER, GridBagConstraints.NONE, 0, 5, 0, 0));
        
        // Create table panel
        JPanel tablePanel = new JPanel(new BorderLayout());
        tableModel = new CsvTableModel();
        table = new JTable(tableModel);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JScrollPane scrollPane = new JScrollPane(table);
        tablePanel.add(scrollPane, BorderLayout.CENTER);
        tablePanel.setBorder(new CompoundBorder(
                new TitledBorder(TextResource.getInstance().getString("partyCsvSettingsDlg.tableBorderTitle")),
                new EmptyBorder(5, 10, 0, 10)));
        
        // Create output format panel
        JPanel outputFormatPanel = new JPanel(new GridBagLayout());
        outputFormatPanel.add(wf.createLabel("partyCsvSettingsDlg.format"),
                SwingUtils.createLabelGBConstraints(0, 0));
        tfOutputFormat = new JTextField("{2} {1}\\n{3}\\n{4} {5}");
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
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel = new ButtonPanel(SwingConstants.RIGHT);
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
        mainPanel.add(filePanel, 
            SwingUtils.createPanelGBConstraints(0, 0));
        mainPanel.add(tablePanel, 
                SwingUtils.createPanelGBConstraints(0, 1));
        mainPanel.add(outputFormatPanel, 
                SwingUtils.createPanelGBConstraints(0, 2));
        mainPanel.add(buttonPanel, 
            SwingUtils.createPanelGBConstraints(0, 3));

        // Finally, add listeners
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                updateSample();
            }
        });

        return mainPanel;
    }
    
    private void updateSample() {
        String format = tfOutputFormat.getText();
        String sample;
        int[] rows = table.getSelectedRows();
        int index = rows.length / 2;
        if (index < rows.length) {
            sample = CsvFileParser.composeValue(format, values[rows[index]]);
        } else {
            sample = TextResource.getInstance().getString("partyCsvSettingsDlg.emptySelection");
        }
        sample = sample.replaceAll("\\\\n", "\n");
        
        taSampleOutput.setText(sample);
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
        parser.setRowIndices(table.getSelectedRows());
        return parser;
    }
    
    private class CsvTableModel extends AbstractTableModel {

        private int nrColumns;

        public CsvTableModel() {
            initValues();
        }

        public void initValues() {
            nrColumns = 0;
            for (int i = 0; i < values.length; i++) {
                nrColumns = Math.max(nrColumns, values[i].length);
            }
        }
        
        public Class<?> getColumnClass(int columnIndex) {
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

    public String getTitle() {
        return TextResource.getInstance().getString("partyCsvSettingsDlg.title");
    }

    public void onClose() {
    }

    public void onInit() {
        add(createPanel());
        onFileChanged();
    }
    
    /** Opens the file selection dialog to let the user select a file. */
    private void onChoseFile() {
        JFileChooser fileChooser = new JFileChooser(tfFileName.getText());
        fileChooser.setFileFilter(new FileFilter() {

            public boolean accept(File f) {
                return f.isDirectory() || f.getName().endsWith(".csv");
            }

            public String getDescription() {
                return TextResource.getInstance().getString("partyCsvSettingsDlg.csvFile");
            }
            
        });
        if (fileChooser.showOpenDialog(PartyCsvSettingsView.this) == JFileChooser.APPROVE_OPTION) {
            file = fileChooser.getSelectedFile();
            onFileChanged();
        }
    }
    
    private void onFileChanged() {
        tfFileName.setText(file != null ? file.getAbsolutePath() : "");
        parser = new CsvFileParser(file);
        try {
            values = parser.getValues();
            if (tableModel != null) {
                tableModel.initValues();
                tableModel.fireTableStructureChanged();
                table.selectAll();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
