/*
 * $Id: ReportDialog.java,v 1.9 2010-01-10 21:16:42 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.dialogs;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerDateModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import com.sun.org.apache.bcel.internal.classfile.PMGClass;

import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.OkCancelDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import cf.engine.Database;
import cf.text.Report;
import cf.text.ReportProgressListener;

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
    
    private Database database;
    
    /**
     * Constructor.
     * @param frame the parent of this dialog
     * @param database the database from which the report is generated
     */
    public ReportDialog(Frame frame, Database database) {
        super(frame, "genreport.title");
        this.parentFrame = frame;
        this.database = database;
        
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
     * @see nl.gogognome.swing.OkCancelDialog#handleOk()
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
        
        hideDialog();
        new ReportTask(tfFileName.getText(), fileType, date).execute();
    }

    private class ReportTask implements ReportProgressListener {
	    
	    private String fileName;
	    private int fileType;
	    private Date date;
	    private JProgressBar progressBar;
	    private JDialog progressDialog;
	    
	    public ReportTask(String fileName, int fileType, Date date) {
	    	this.fileName = fileName;
	    	this.fileType = fileType;
	    	this.date = date;
	    }
	    
	    public void execute() {
	    	ReportGeneratorThread thread = new ReportGeneratorThread(fileName, fileType, date, this);
	    	thread.start();
	    	progressDialog = new JDialog(parentFrame);
	    	progressBar = new JProgressBar(0, 100);
	    	JPanel panel = new JPanel(new BorderLayout());
	    	progressDialog.setLayout(new BorderLayout());
	    	panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	    	panel.add(new JLabel(TextResource.getInstance().getString("genreport.progress")), BorderLayout.NORTH);
	    	panel.add(progressBar, BorderLayout.CENTER);
	    	progressDialog.add(panel);
	    	progressDialog.pack();
	    	progressDialog.setModal(true);
	    	progressDialog.setResizable(false);
	    	Dimension d = parentFrame.getSize();
	    	Point location = parentFrame.getLocation();
	    	location.translate((int)(d.getWidth() / 2), (int)(d.getHeight() / 2));
	    	progressDialog.setLocation(location);
	    	progressDialog.setVisible(true);
	    }
	    
	    public void onFinished(final Throwable t) {
	    	SwingUtilities.invokeLater(new Runnable() {
	    		public void run() {
	    			progressDialog.setVisible(false);
			    	if (t != null) {
			            MessageDialog dialog = new MessageDialog(parentFrame, "gen.titleError", t);
			            dialog.showDialog();
			    	}
	    		}
	    	});
	    }
	    
	    public void onProgressUpdate(final int percentageCompleted) {
	    	SwingUtilities.invokeLater(new Runnable() {
	    		public void run() {
	    			progressBar.setValue(percentageCompleted);
	    		}
	    	});
	    }
    }
    
    private class ReportGeneratorThread extends Thread {
    	private String fileName;
    	private int fileType;
    	private Date date;
    	private ReportProgressListener rpl;
    	
		public ReportGeneratorThread(String fileName, int fileType, Date date, ReportProgressListener rpl) {
			super("report generator");
			this.fileName = fileName;
			this.fileType = fileType;
			this.date = date;
			this.rpl = rpl;
		}
    	
		public void run() {
	        Report report = new Report(database, date);
	        try {
	            report.createReportFile(fileName, fileType, rpl);
	        } catch (Throwable t) {
	        	rpl.onFinished(t);
	        }
		}
    }
}
