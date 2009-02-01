/*
 * $Id: ReportDialog.java,v 1.8 2009-02-01 19:53:43 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.border.TitledBorder;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.OkCancelDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.text.Report;

/**
 * This class implements the report dialog. This dialog can generate
 * a report consisting of balance, operational result and overviews of
 * debtors and creditors, journals and ledger in different file formats.
 * 
 * @author Sander Kooijmans
 */
public class ReportDialog extends OkCancelDialog { 
    
    private JTextField tfFileName;
    private DateModel dateModel;
    
    private JRadioButton rbTxtFile;
    
    private Frame parentFrame;
    
    /**
     * Constructor.
     * @param frame the parent of this dialog
     */
    public ReportDialog(Frame frame) 
    {
        super(frame, "genreport.title");
        this.parentFrame = frame;
        
        WidgetFactory wf = WidgetFactory.getInstance();
        TextResource tr = TextResource.getInstance();
        
        // Create file name and date panel
        GridBagLayout gbl = new GridBagLayout();
        JPanel fileNamePanel = new JPanel(gbl);
        JLabel lbTemplateFileName = wf.createLabel("genreport.templateFilename");
        
        fileNamePanel.add(lbTemplateFileName, 
                SwingUtils.createLabelGBConstraints(0, 0));
        
        tfFileName = wf.createTextField(30);
        fileNamePanel.add(tfFileName,
                SwingUtils.createTextFieldGBConstraints(1, 0));
        
        JButton button = wf.createButton("gen.btSelectFile", new AbstractAction() { 
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser(tfFileName.getText());
                if (fileChooser.showOpenDialog(parentFrame) == JFileChooser.APPROVE_OPTION) {
                    tfFileName.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }
                
            }
        });
        fileNamePanel.add(button,
                SwingUtils.createLabelGBConstraints(2, 0));
        
        JLabel lbDate = wf.createLabel("genreport.date");
        fileNamePanel.add(lbDate, 
                SwingUtils.createLabelGBConstraints(0, 1));
        
        SpinnerDateModel model = new SpinnerDateModel();
        model.setCalendarField(Calendar.DAY_OF_YEAR);
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
        
        fileNamePanel.add(new DateSelectionBean(dateModel),
                SwingUtils.createTextFieldGBConstraints(1, 1));
        
        // Create file type panel
        gbl = new GridBagLayout();
        JPanel fileTypePanel = new JPanel(gbl);
        fileTypePanel.setBorder(new TitledBorder(tr.getString("genreport.fileType")));
        
        ButtonGroup buttonGroup = new ButtonGroup();
        
        rbTxtFile = new JRadioButton(wf.createAction("genreport.txt"));
        fileTypePanel.add(rbTxtFile,
                SwingUtils.createGBConstraints(0, 2));
        buttonGroup.add(rbTxtFile);
        
        rbTxtFile.setSelected(true);

        // Create top panel
        gbl = new GridBagLayout();
        JPanel topPanel = new JPanel(gbl);
        topPanel.add(fileNamePanel,
                SwingUtils.createGBConstraints(0, 0));
        topPanel.add(fileTypePanel,
                SwingUtils.createGBConstraints(0, 1));
        
        componentInitialized(topPanel);
    }

    /* (non-Javadoc)
     * @see cf.ui.dialogs.OkCancelDialog#handleOk()
     */
    protected void handleOk() {
        int fileType = Report.RP_TXT;
        
        Date date = dateModel.getDate();
        if (date == null) {
            MessageDialog dialog = new MessageDialog(parentFrame, "ds.parseErrorTitle", 
                    TextResource.getInstance().getString("ds.parseErrorMessage"));
            dialog.showDialog();
            return;
        }
        
        generateReport(tfFileName.getText(), fileType, date);
        hideDialog();
    }

    private void generateReport(String fileName, int fileType, Date date) {
        Report report = new Report(Database.getInstance(), date);
        try {
            report.createReportFile(fileName, fileType);
        } catch (IOException e) {
            MessageDialog dialog = new MessageDialog(parentFrame, "gen.titleError", e);
            dialog.showDialog();
        } catch (InterruptedException e) {
            e.printStackTrace();
            MessageDialog.showMessage(parentFrame, "gen.titleError", 
                TextResource.getInstance().getString("genreport.cantConvertTexToPdf",
                new Object[] {fileName}));
        }
    }
}
