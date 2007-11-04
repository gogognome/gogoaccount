/*
 * $Id: MainFrame.java,v 1.36 2007-11-04 19:26:03 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui;

import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import nl.gogognome.framework.View;
import nl.gogognome.framework.ViewListener;
import nl.gogognome.framework.ViewTabbedPane;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.swing.plaf.DefaultLookAndFeel;
import nl.gogognome.text.TextResource;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.engine.Journal;
import cf.engine.XMLParseException;
import cf.engine.Party;
import cf.engine.XMLFileReader;
import cf.engine.XMLFileWriter;
import cf.print.AddressLabelPrinter;
import cf.ui.dialogs.AccountSelectionDialog;
import cf.ui.dialogs.DateSelectionDialog;
import cf.ui.dialogs.EditJournalDialog;
import cf.ui.dialogs.EditJournalsDialog;
import cf.ui.dialogs.InvoiceDialog;
import cf.ui.dialogs.InvoiceGeneratorDialog;
import cf.ui.dialogs.PartySelectionDialog;
import cf.ui.dialogs.ReportDialog;
import cf.ui.dialogs.ViewAccountOverviewDialog;
import cf.ui.dialogs.ViewPartiesOverviewDialog;
import cf.ui.dialogs.ViewPartyOverviewDialog;
import cf.ui.views.BalanceView;
import cf.ui.views.OperationalResultView;
import cf.ui.views.PartiesView;

/**
 * This class implements the main frame of the application. 
 * 
 * @author Sander Kooijmans
 */
public class MainFrame extends JFrame implements ActionListener, DatabaseListener {
    private static final long serialVersionUID = 1L;

    /** The menu bar of the application. */
	private JMenuBar menuBar = new JMenuBar();

	/** The tabbed pane containing the views. */
	private ViewTabbedPane viewTabbedPane;
	
	/** The balance view. */
	private BalanceView balanceView;

	/** The operational result view. */
	private OperationalResultView operationalResultView;
	
    /** THe view for editing parties. */
    private PartiesView partiesView;
    
	/** Creates the main frame. */
	public MainFrame() 
	{
		super(createTitle());
		Database.getInstance().addListener(this);
		createMenuBar();
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		viewTabbedPane = new ViewTabbedPane(this);
		getContentPane().add(viewTabbedPane);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) { handleExit(); } }
		);
	}
	
	/**
	 * Creates the title to be shown in the title bar of the main frame.
	 * @return the title
	 */
	private static String createTitle()
	{
	    String result = TextResource.getInstance().getString("mf.title");
	    Database db = Database.getInstance();
	    String description = db.getDescription();
	    if (description != null)
	    {
	        result += " - " + description;
	        if (db.hasUnsavedChanges())
	        {
	            result += "*";
	        }
	    }
	    return result;
	}
	/** Creates the menu bar. */
	private void createMenuBar() {
		// the menus
		WidgetFactory wf = WidgetFactory.getInstance();
		JMenu fileMenu = wf.createMenu("mi.file");
		JMenu editMenu = wf.createMenu("mi.edit");
		JMenu viewMenu = wf.createMenu("mi.view");
		JMenu reportingMenu = wf.createMenu("mi.reporting");
		JMenu helpMenu = wf.createMenu("mi.help");

		// the file menu
		JMenuItem miNewEdition = wf.createMenuItem("mi.newBookkeeping", this);
		JMenuItem miOpenEdition = wf.createMenuItem("mi.openBookkeeping", this);
		JMenuItem miSaveEdition = wf.createMenuItem("mi.saveBookkeeping", this);
		JMenuItem miSaveEditionAs = wf.createMenuItem("mi.saveBookkeepingAs", this);
		JMenuItem miExit = wf.createMenuItem("mi.exit", this);

		// the edit menu
		JMenuItem miAddJournal = wf.createMenuItem("mi.addJournal", this);
		JMenuItem miEditJournals = wf.createMenuItem("mi.editJournals", this);
		JMenuItem miAddInvoices = wf.createMenuItem("mi.addInvoices", this);
        JMenuItem miEditParties = wf.createMenuItem("mi.editParties", this);
		JMenuItem miCleanUp = wf.createMenuItem("mi.cleanUp", this);

		// the view menu
		JMenuItem miViewBalance = wf.createMenuItem("mi.viewBalance", this);
		JMenuItem miViewOperationalResult = wf.createMenuItem("mi.viewOperationalResult", this);
		JMenuItem miViewAccountOverview = wf.createMenuItem("mi.viewAccountOverview", this);
		JMenuItem miViewPartyOverview = wf.createMenuItem("mi.viewPartyOverview", this);
		JMenuItem miViewPartiesOverview = wf.createMenuItem("mi.viewPartiesOverview", this);

		// the reporting menu
		JMenuItem miGenerateInvoices = wf.createMenuItem("mi.generateInvoices", this);
		JMenuItem miGenerateReport = wf.createMenuItem("mi.generateReport", this);
		JMenuItem miPrintAddressLabels = wf.createMenuItem("mi.printAddressLabels", this);
		
		// the help menu
		JMenuItem miAbout = wf.createMenuItem("mi.about", this);

		fileMenu.add(miNewEdition);
		fileMenu.add(miOpenEdition);
		fileMenu.add(miSaveEdition);
		fileMenu.add(miSaveEditionAs);
		fileMenu.add(miExit);
		
		editMenu.add(miAddJournal);
		editMenu.add(miEditJournals);
		editMenu.add(miAddInvoices);
        editMenu.add(miEditParties);
		editMenu.addSeparator();
		editMenu.add(miCleanUp);
		
		viewMenu.add(miViewBalance);
		viewMenu.add(miViewOperationalResult);
		viewMenu.add(miViewAccountOverview);
		viewMenu.add(miViewPartyOverview);
		viewMenu.add(miViewPartiesOverview);
		
		reportingMenu.add(miGenerateInvoices);
		reportingMenu.add(miGenerateReport);
		reportingMenu.add(miPrintAddressLabels);
		
		helpMenu.add(miAbout);
		
		menuBar.add(fileMenu);
		menuBar.add(editMenu);
		menuBar.add(viewMenu);
		menuBar.add(reportingMenu);
		menuBar.add(helpMenu);
		
		setJMenuBar(menuBar);
	}
	
	/**
	 * This method is invoked when an action occurs.
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) 
	{
		String command = e.getActionCommand();
		if ("mi.openBookkeeping".equals(command)) { handleOpenBookkeeping(); }
		if ("mi.saveBookkeeping".equals(command)) { handleSaveBookkeeping(); }
		if ("mi.saveBookkeepingAs".equals(command)) { handleSaveBookeepingAs(); }
		if ("mi.exit".equals(command)) { handleExit(); }
		if ("mi.viewBalance".equals(command)) { handleViewBalance(); }
		if ("mi.viewOperationalResult".equals(command)) { handleViewOperationalResult(); }
		if ("mi.viewAccountOverview".equals(command)) { handleViewAccountOverview(); }
		if ("mi.viewPartyOverview".equals(command)) { handleViewPartyOverview(); }
		if ("mi.viewPartiesOverview".equals(command)) { handleViewPartiesOverview(); }
		if ("mi.addJournal".equals(command)) { handleAddJournal(); }
		if ("mi.editJournals".equals(command)) { handleEditJournals(); }
		if ("mi.cleanUp".equals(command)) { handleCleanUp(); }
		if ("mi.addInvoices".equals(command)) { handleAddInvoices(); }
        if ("mi.editParties".equals(command)) { handleEditParties(); }
		if ("mi.generateInvoices".equals(command)) { handleGenerateInvoices(); }
		if ("mi.generateReport".equals(command)) { handleGenerateReport(); }
		if ("mi.printAddressLabels".equals(command)) { handlePrintAddressLabels(); }
	}

	/**
	 * Starts the application.
	 * @param args command line arguments; if one argument is passed, then
	 *        it is used as file name of an edition that is loaded.
	 *        Further, if the argument <tt>-lang=X</tt> is used, then
	 *        the language is set to </tt>X</tt>. </tt>X</tt> should be a valid
	 *        ISO 639 language code.
	 */
	public static void main(String[] args) 
	{
		// parse arguments: language must be set before creating main frame
		String fileName = null;
		for (int i=0; i<args.length; i++) 
		{
			if (args[i].startsWith("-lang=")) 
			{
				TextResource.getInstance().setLocale(new Locale(args[i].substring(6)));
			} 
			else 
			{
				fileName = args[i];
			}
		}

        DefaultLookAndFeel.useDefaultLookAndFeel();
            
        // Create and show main frame.
		MainFrame mf = new MainFrame();
        mf.setVisible(true);
		mf.setExtendedState(MAXIMIZED_BOTH);

		if (fileName != null) {
		    mf.loadFile(fileName);
		}
	}

	/**
	 * Checks whether the database may be destroyed. 
	 * 
	 * <p>If the database has been
	 * changed since the last load or save, then the user is asked whether these
	 * changes must be saved. If the user wants these changes to be saved, then
	 * changes are saved before this method returns.
	 * 
	 * <p>If the user does not wants to save the changes, then this method simply
	 * returns.
	 * 
	 * @return <tt>true</tt> if the database may be destroyed; <tt>false</tt> otherwise.
	 */
	private boolean mayCurrentDatabaseBeDestroyed() 
	{
		boolean result;
		if (Database.getInstance().hasUnsavedChanges()) 
		{
		    TextResource tr = TextResource.getInstance();
			MessageDialog dialog = new MessageDialog(this, 
				"gen.titleWarning",
				tr.getString("mf.saveChangesBeforeExit"),
				new String[] {"gen.yes", "gen.no", "gen.cancel"});
			switch(dialog.getSelectedButton()) 
			{
				case 0: // yes
					handleSaveBookkeeping();
					result = true;
					break;
				case 1: // no; continue without saving
					result = true;
					break;
				case -1: // user pressed escape or closed dialog
				case 2: // cancel
					result = false;
					break;
				default:
					throw new IllegalStateException("Unknown button pressed. Index: " 
						+ dialog.getSelectedButton());
			}
		} 
		else 
		{ // database has not been changed since last save/load
			result = true;
		}
		
		if (result) {
		    
		}
		
		return result;
	}
	
	/** Handles the new edition event. */
/*	private void handleNewEdition() 
	{
		if (mayCurrentDatabaseBeDestroyed()) {
			Database db = new Database();
			Database.setCurrentDatabase(db);
			handleEditEdition();
		}
	}
	
	/** Handles the open bookkeeping event. */
	private void handleOpenBookkeeping() 
	{
		if (mayCurrentDatabaseBeDestroyed()) 
		{
		    TextResource tr = TextResource.getInstance();		    
			FileDialog fileDialog = new FileDialog(this, tr.getString("mf.titleOpenBookkeeping"), FileDialog.LOAD);
			fileDialog.setFilenameFilter(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					File compositeName = new File(dir,name);  
					return compositeName.isDirectory() || name.toLowerCase().endsWith(".xml"); } } 
				);
			fileDialog.setVisible(true);
			String directory = fileDialog.getDirectory();
			String filename  = fileDialog.getFile();
			if (directory != null && filename != null) 
			{
				loadFile(directory + filename);
			}
			requestFocus();
		}
	}

	/** Handles the save bookkeeping event. */
	private void handleSaveBookkeeping() 
	{
		String fileName = Database.getInstance().getFileName(); 
		if (fileName != null) 
		{
		    saveBookkeeping(fileName);
		} 
		else 
		{
			handleSaveBookeepingAs(); 
		}
	}
	
	/** Handles the save edition as event. */
	private void handleSaveBookeepingAs() 
	{
	    TextResource tr = TextResource.getInstance();		    
		FileDialog fileDialog = new FileDialog(this, tr.getString("mf.titleSaveAs"), FileDialog.SAVE);
		fileDialog.setFilenameFilter(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				File compositeName = new File(dir,name);  
				return compositeName.isDirectory() || name.toLowerCase().endsWith(".xml"); } } 
			);
		fileDialog.setFile(Database.getInstance().getFileName());
		fileDialog.setVisible(true);
		String directory = fileDialog.getDirectory();
		String filename  = fileDialog.getFile();
		if (directory != null && filename != null) 
		{
			saveBookkeeping(directory + filename);
		}
		requestFocus();
	}

	/**
	 * Loads a bookkeeping from an XML file.
	 * @param fileName the name of the file.
	 */
	private void loadFile(String fileName) {	
		try {
			Database db = XMLFileReader.createDatabaseFromFile(fileName);
//			db.startMultipleUpdates();
			Database.getInstance().removeListener(this);
			Database.setInstance(db);
			db.addListener(this);
			db.databaseConsistentWithFile();
//			db.endMultipleUpdates();
			viewTabbedPane.closeAllViews();
			balanceView = null;
			operationalResultView = null;
			
			handleViewBalance();
			handleViewOperationalResult();
		} catch (XMLParseException e) {
			new MessageDialog(this, "mf.errorOpeningFile", e);
		} catch (IOException e) {
            new MessageDialog(this, "mf.errorOpeningFile", e);
        }
		databaseChanged(Database.getInstance());
	}
	
	/**
	 * Saves the current bookkeeping to an XML file.
	 * @param fileName the name of the file.
	 */
	private void saveBookkeeping(String fileName) {
		try 
		{
			XMLFileWriter.writeDatabaseToFile(Database.getInstance(), fileName);
			Database db = Database.getInstance(); 
			db.setFileName(fileName);
			db.databaseConsistentWithFile();
		} 
		catch (RuntimeException e) 
		{
			String message = e.getMessage(); 
			new MessageDialog(this, "gen.error", 
				message != null ? message : e.toString());
		}
	}
	
	/** Handles the exit event. */
	private void handleExit() 
	{
		if (mayCurrentDatabaseBeDestroyed()) 
		{
			Database.getInstance().removeListener(this);
			dispose();
		}
	}

	private void handleViewBalance() {
	    if (balanceView == null) {
	        balanceView = new BalanceView(Database.getInstance());
            balanceView.addViewListener(new ViewListener() {
                public void onViewClosed(View view) {
                    view.removeViewListener(this);
                    balanceView = null;
                }
            });
            viewTabbedPane.openView(balanceView);
            viewTabbedPane.selectView(balanceView);
        } else {
            viewTabbedPane.selectView(balanceView);
	    }
	}
	
	private void handleViewOperationalResult() {
	    if (operationalResultView == null) {
	        operationalResultView = new OperationalResultView(Database.getInstance());
            operationalResultView.addViewListener(new ViewListener() {
                public void onViewClosed(View view) {
                    view.removeViewListener(this);
                    operationalResultView = null;
                }
            });
	        viewTabbedPane.openView(operationalResultView);
            viewTabbedPane.selectView(operationalResultView);
	    } else {
	        viewTabbedPane.selectView(operationalResultView);
	    }
	}

    private void handleEditParties() {
        if (partiesView == null) {
            partiesView = new PartiesView(Database.getInstance());
            partiesView.addViewListener(new ViewListener() {
                public void onViewClosed(View view) {
                    view.removeViewListener(this);
                    partiesView = null;
                }
            });
            viewTabbedPane.openView(partiesView);
            viewTabbedPane.selectView(partiesView);
        } else {
            viewTabbedPane.selectView(partiesView);
        }
    }

	private void handleViewAccountOverview()
	{
        AccountSelectionDialog accountSelectionDialog = new
        	AccountSelectionDialog(this, "mf.selectAccountForAccountOverview");
        accountSelectionDialog.showDialog();
        Account account = accountSelectionDialog.getAccount();
        if (account != null)
        {
            DateSelectionDialog dateSelectionDialog = 
                new DateSelectionDialog(this, "mf.selectDateForAccountOverview");
            dateSelectionDialog.showDialog();
            Date date = dateSelectionDialog.getDate();
            if (date != null)
            {
	            ViewAccountOverviewDialog dialog = new ViewAccountOverviewDialog(this, account, date);
	            dialog.showDialog();
	        }
	    }
	}
	
	private void handleViewPartyOverview()
	{
        PartySelectionDialog partySelectionDialog = new
    		PartySelectionDialog(this, "mf.selectPartyForPartyOverview",
    		        PartySelectionDialog.SELECTION_MODE);
	    partySelectionDialog.showDialog();
	    Party party = partySelectionDialog.getSelectedParty();
	    if (party != null)
	    {
	        DateSelectionDialog dateSelectionDialog = 
	            new DateSelectionDialog(this, "mf.selectDateForPartyOverview");
	        dateSelectionDialog.showDialog();
	        Date date = dateSelectionDialog.getDate();
	        if (date != null)
	        {
	            ViewPartyOverviewDialog dialog = new ViewPartyOverviewDialog(this, party, date);
	            dialog.showDialog();
	        }
	    }
	}

	private void handleViewPartiesOverview()
	{
        DateSelectionDialog dateSelectionDialog = 
            new DateSelectionDialog(this, "mf.selectDateForPartiesOverview");
        dateSelectionDialog.showDialog();
        Date date = dateSelectionDialog.getDate();
        if (date != null)
        {
            ViewPartiesOverviewDialog dialog = new ViewPartiesOverviewDialog(this, date);
            dialog.showDialog();
        }
	}
	
	private void handleAddJournal()
	{
	    Database db = Database.getInstance();
	    if (!db.hasAccounts())
	    {
	        MessageDialog.showMessage(this, "gen.warning", 
	                TextResource.getInstance().getString("mf.noAccountsPresent"));
	    }
	    else
	    {
		    EditJournalDialog dialog = new EditJournalDialog(this, "ajd.title", true);
		    dialog.showDialog();
		    Journal[] journals = dialog.getEditedJournals();
		    for (int i = 0; i < journals.length; i++) {
		        db.addJournal(journals[i]);
            }
	    }
	}
	
	private void handleEditJournals()
	{
	    Database db = Database.getInstance();
	    if (!db.hasAccounts())
	    {
	        MessageDialog.showMessage(this, "gen.warning", 
	                TextResource.getInstance().getString("mf.noAccountsPresent"));
	    }
	    else
	    {
		    EditJournalsDialog dialog = new EditJournalsDialog(this);
		    dialog.showDialog();
	    }
	}
	
	private void handleCleanUp() {
	    TextResource tr = TextResource.getInstance();
		MessageDialog dialog = new MessageDialog(this, "gen.titleWarning",
			tr.getString("mf.cleanUpWarning"),
			new String[] {"gen.yes", "gen.no"});
		if (dialog.getSelectedButton() != 0) {
		    return; // user does not want to continue
		}

	    DateSelectionDialog dateSelectionDialog = 
	        new DateSelectionDialog(this, "mf.selectDateForCleanUp");
	    dateSelectionDialog.showDialog();
	    Date date = dateSelectionDialog.getDate();
	    if (date != null) {
	        Database.getInstance().cleanUpJournalsBefore(date);
	    }
	}
	
	private void handleGenerateInvoices()
	{
	    InvoiceDialog dialog = new InvoiceDialog(this);
	    dialog.showDialog();
	}

	private void handleGenerateReport() {
	    ReportDialog dialog = new ReportDialog(this);
	    dialog.showDialog();
	}
	
	private void handlePrintAddressLabels() {
	    AddressLabelPrinter alp = new AddressLabelPrinter(Database.getInstance().getParties());
	    try {
            alp.printAddressLabels();
        } catch (PrinterException e) {
	        MessageDialog.showMessage(this, "gen.error", 
	                TextResource.getInstance().getString("mf.problemWhilePrinting"));
        }
	}
	
	private void handleAddInvoices() {
	    InvoiceGeneratorDialog dialog = new InvoiceGeneratorDialog(this);
	    dialog.showDialog();
	}
	
    /**
     * This method is called when the database has changed.
     * @param db the database that has changed
     */
    public void databaseChanged(Database db) 
    {
        setTitle(createTitle());
    }
}
