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
package cf.ui;

import java.awt.FileDialog;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.Locale;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;

import nl.gogognome.cf.services.BookkeepingService;
import nl.gogognome.cf.services.CreationException;
import nl.gogognome.framework.View;
import nl.gogognome.framework.ViewDialog;
import nl.gogognome.framework.ViewListener;
import nl.gogognome.framework.ViewTabbedPane;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.swing.plaf.DefaultLookAndFeel;
import nl.gogognome.text.TextResource;
import cf.engine.Account;
import cf.engine.Database;
import cf.engine.DatabaseListener;
import cf.engine.Party;
import cf.engine.XMLFileReader;
import cf.engine.XMLFileWriter;
import cf.engine.XMLParseException;
import cf.print.AddressLabelPrinter;
import cf.ui.dialogs.AccountSelectionDialog;
import cf.ui.dialogs.DateSelectionDialog;
import cf.ui.dialogs.ReportDialog;
import cf.ui.dialogs.ViewAccountOverviewDialog;
import cf.ui.dialogs.ViewPartiesOverviewDialog;
import cf.ui.dialogs.ViewPartyOverviewDialog;
import cf.ui.views.AboutView;
import cf.ui.views.BalanceAndOperationResultView;
import cf.ui.views.BalanceView;
import cf.ui.views.CloseBookkeepingView;
import cf.ui.views.ConfigureBookkeepingView;
import cf.ui.views.EditJournalView;
import cf.ui.views.EditJournalsView;
import cf.ui.views.InvoiceGeneratorView;
import cf.ui.views.InvoiceToOdtView;
import cf.ui.views.OperationalResultView;
import cf.ui.views.PartiesView;

/**
 * This class implements the main frame of the application.
 *
 * @author Sander Kooijmans
 */
public class MainFrame extends JFrame implements ActionListener, DatabaseListener {
    private static final long serialVersionUID = 1L;

    /** The current database of the application. */
    private Database database = new Database();

    /** The menu bar of the application. */
	private JMenuBar menuBar = new JMenuBar();

	/** The tabbed pane containing the views. */
	private ViewTabbedPane viewTabbedPane;

   /** The view containing the balance and operational result. */
    private BalanceAndOperationResultView balanceAndOperationResultView;

	/** The balance view. */
	private BalanceView balanceView;

	/** The operational result view. */
	private OperationalResultView operationalResultView;

    /** The view for editing parties. */
    private PartiesView partiesView;

    /** The view to configure a bookkeeping. */
    private ConfigureBookkeepingView configureBookkeepingView;

    /** The view for invoice generation. */
    private InvoiceGeneratorView invoiceGeneratorView;

    /** The view to edit journals. */
    private EditJournalsView editJournalsView;

    /** The view to generate an ODT file for invoices. */
    private InvoiceToOdtView invoiceToOdtView;

	/** Creates the main frame. */
	public MainFrame() {
		super();
		database.addListener(this);
		createMenuBar();
		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		viewTabbedPane = new ViewTabbedPane(this);
		getContentPane().add(viewTabbedPane);

		addWindowListener(new WindowAdapter() {
			@Override
            public void windowClosing(WindowEvent e) { handleExit(); } }
		);

        setTitle(createTitle());

        // Set icon
        URL url = ClassLoader.getSystemResource("icon-32x32.png");
        Image image = Toolkit.getDefaultToolkit().createImage(url);
        setIconImage(image);
	}

	/**
	 * Creates the title to be shown in the title bar of the main frame.
	 * @return the title
	 */
	private String createTitle() {
	    String result = TextResource.getInstance().getString("mf.title");
	    String description = database.getDescription();
	    if (description != null)
	    {
	        result += " - " + description;
	        if (database.hasUnsavedChanges())
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
        JMenuItem miConfigureBookkeeping = wf.createMenuItem("mi.configureBookkeeping", this);
		JMenuItem miSaveEdition = wf.createMenuItem("mi.saveBookkeeping", this);
		JMenuItem miSaveEditionAs = wf.createMenuItem("mi.saveBookkeepingAs", this);
        JMenuItem miCloseBookkeeping = wf.createMenuItem("mi.closeBookkeeping", this);
		JMenuItem miExit = wf.createMenuItem("mi.exit", this);

		// the edit menu
		JMenuItem miAddJournal = wf.createMenuItem("mi.addJournal", this);
		JMenuItem miEditJournals = wf.createMenuItem("mi.editJournals", this);
		JMenuItem miAddInvoices = wf.createMenuItem("mi.addInvoices", this);
        JMenuItem miEditParties = wf.createMenuItem("mi.editParties", this);

		// the view menu
        JMenuItem miViewBalanceAndOpertaionalResult = wf.createMenuItem("mi.viewBalanceAndOperationalResult", this);
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
		fileMenu.add(miConfigureBookkeeping);
		fileMenu.add(miSaveEdition);
		fileMenu.add(miSaveEditionAs);
        fileMenu.add(miCloseBookkeeping);
		fileMenu.add(miExit);

		editMenu.add(miAddJournal);
		editMenu.add(miEditJournals);
		editMenu.add(miAddInvoices);
        editMenu.add(miEditParties);

        viewMenu.add(miViewBalanceAndOpertaionalResult);
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
		if ("mi.configureBookkeeping".equals(command)) { handleConfigureBookkeeping(); }
		if ("mi.saveBookkeeping".equals(command)) { handleSaveBookkeeping(); }
		if ("mi.saveBookkeepingAs".equals(command)) { handleSaveBookeepingAs(); }
        if ("mi.closeBookkeeping".equals(command)) { handleCloseBookkeeping(); }
		if ("mi.exit".equals(command)) { handleExit(); }
        if ("mi.viewBalanceAndOperationalResult".equals(command)) { handleViewBalanceAndOperationalResult(); }
		if ("mi.viewBalance".equals(command)) { handleViewBalance(); }
		if ("mi.viewOperationalResult".equals(command)) { handleViewOperationalResult(); }
		if ("mi.viewAccountOverview".equals(command)) { handleViewAccountOverview(); }
		if ("mi.viewPartyOverview".equals(command)) { handleViewPartyOverview(); }
		if ("mi.viewPartiesOverview".equals(command)) { handleViewPartiesOverview(); }
		if ("mi.addJournal".equals(command)) { handleAddJournal(); }
		if ("mi.editJournals".equals(command)) { handleEditJournals(); }
		if ("mi.addInvoices".equals(command)) { handleAddInvoices(); }
        if ("mi.editParties".equals(command)) { handleEditParties(); }
		if ("mi.generateInvoices".equals(command)) { handleGenerateInvoices(); }
		if ("mi.generateReport".equals(command)) { handleGenerateReport(); }
		if ("mi.printAddressLabels".equals(command)) { handlePrintAddressLabels(); }
		if ("mi.about".equals(command)) { handleAbout(); }
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
		if (database.hasUnsavedChanges())
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
*/
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

	/** Handles the configure bookkeeping event. */
	private void handleConfigureBookkeeping()
	{
	    if (configureBookkeepingView == null) {
	    	configureBookkeepingView = new ConfigureBookkeepingView(database);
	    	configureBookkeepingView.addViewListener(new ViewListener() {
                public void onViewClosed(View view) {
                    view.removeViewListener(this);
                    configureBookkeepingView = null;
                }
            });
            viewTabbedPane.openView(configureBookkeepingView);
            viewTabbedPane.selectView(configureBookkeepingView);
        } else {
            viewTabbedPane.selectView(configureBookkeepingView);
	    }
	}

	/** Handles the save bookkeeping event. */
	private void handleSaveBookkeeping()
	{
		String fileName = database.getFileName();
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
		fileDialog.setFile(database.getFileName());
		fileDialog.setVisible(true);
		String directory = fileDialog.getDirectory();
		String filename  = fileDialog.getFile();
		if (directory != null && filename != null)
		{
			saveBookkeeping(directory + filename);
		}
		requestFocus();
	}

	/** Handles closing the bookkeeping. */
	private void handleCloseBookkeeping() {
	    if (database.hasUnsavedChanges()) {
	        handleSaveBookkeeping();
	    }
        if (database.hasUnsavedChanges()) {
            // The user did not want to save the changes. Do not close the bookkeeping.
            return;
        }

        CloseBookkeepingView cbv = new CloseBookkeepingView(database);
        new ViewDialog(this, cbv).showDialog();
        Date date = cbv.getDate();
        Account accountToAddResultTo = cbv.getAccountToAddResultTo();
        String description = cbv.getDescription();
        if (date != null && accountToAddResultTo != null) {
            try {
                Database newDatabase = BookkeepingService.closeBookkeeping(database, description, date, accountToAddResultTo);
                setDatabase(newDatabase);
            } catch (CreationException e) {
                MessageDialog.showMessage(this, "gen.error", e.getMessage());
            }
        }
	}

	/**
	 *
	 * Loads a bookkeeping from an XML file.
	 * @param fileName the name of the file.
	 */
	private void loadFile(String fileName) {
        Database newDatabase = null;
		try {
			newDatabase = XMLFileReader.createDatabaseFromFile(fileName);
		} catch (XMLParseException e) {
			new MessageDialog(this, "mf.errorOpeningFile", e);
            return;
		} catch (IOException e) {
            new MessageDialog(this, "mf.errorOpeningFile", e);
            return;
        }
		newDatabase.databaseConsistentWithFile();
		setDatabase(newDatabase);
	}

	/**
	 * Replaces the old database by the new database.
	 * @param newDatabase the new database
	 */
	private void setDatabase(Database newDatabase) {
        database.removeListener(this);

        database = newDatabase;
        Database.setInstance(database);
        database.addListener(this);
        viewTabbedPane.closeAllViews();
        balanceView = null;
        operationalResultView = null;
        partiesView = null;
        invoiceGeneratorView = null;
        editJournalsView = null;

        databaseChanged(database);
        handleViewBalanceAndOperationalResult();
	}

	/**
	 * Saves the current bookkeeping to an XML file.
	 * @param fileName the name of the file.
	 */
	private void saveBookkeeping(String fileName) {
		try {
			XMLFileWriter.writeDatabaseToFile(database, fileName);
			database.setFileName(fileName);
			database.databaseConsistentWithFile();
		} catch (Exception e) {
			String message = e.getMessage();
			new MessageDialog(this, "gen.error",
				message != null ? message : e.toString());
		}
	}

	/** Handles the exit event. */
	private void handleExit() {
		if (mayCurrentDatabaseBeDestroyed()) {
			database.removeListener(this);
			dispose();
		}
	}

	private void handleViewBalanceAndOperationalResult() {
        if (balanceAndOperationResultView == null) {
            balanceAndOperationResultView = new BalanceAndOperationResultView(database);
            balanceAndOperationResultView.addViewListener(new ViewListener() {
                public void onViewClosed(View view) {
                    view.removeViewListener(this);
                    balanceAndOperationResultView = null;
                }
            });
            viewTabbedPane.openView(balanceAndOperationResultView);
            viewTabbedPane.selectView(balanceAndOperationResultView);
        } else {
            viewTabbedPane.selectView(balanceAndOperationResultView);
        }
    }

	private void handleViewBalance() {
	    if (balanceView == null) {
	        balanceView = new BalanceView(database);
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
	        operationalResultView = new OperationalResultView(database);
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
            partiesView = new PartiesView(database, false);
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
        AccountSelectionDialog accountSelectionDialog =
        	new AccountSelectionDialog(this, database, "mf.selectAccountForAccountOverview");
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
	            ViewAccountOverviewDialog dialog = new ViewAccountOverviewDialog(this, database, account, date);
	            dialog.showDialog();
	        }
	    }
	}

	private void handleViewPartyOverview() {
        PartiesView partiesView = new PartiesView(database, true);
        ViewDialog partyViewDialog = new ViewDialog(this, partiesView);
        partyViewDialog.showDialog();

	    Party[] parties = partiesView.getSelectedParties();
	    if (parties != null && parties.length == 1) {
	        DateSelectionDialog dateSelectionDialog =
	            new DateSelectionDialog(this, "mf.selectDateForPartyOverview");
	        dateSelectionDialog.showDialog();
	        Date date = dateSelectionDialog.getDate();
	        if (date != null) {
	            ViewPartyOverviewDialog dialog = new ViewPartyOverviewDialog(this, database, parties[0], date);
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

	private void handleAddJournal() {
	    if (!database.hasAccounts()) {
	        MessageDialog.showMessage(this, "gen.warning",
	                TextResource.getInstance().getString("mf.noAccountsPresent"));
	    } else {
            EditJournalView view = new EditJournalView(database, "ajd.title", null);
            ViewDialog dialog = new ViewDialog(this, view);
            dialog.showDialog();
	    }
	}

	private void handleEditJournals() {
	    if (!database.hasAccounts()) {
	        MessageDialog.showMessage(this, "gen.warning",
	                TextResource.getInstance().getString("mf.noAccountsPresent"));
	    } else {
            if (editJournalsView == null) {
                editJournalsView = new EditJournalsView(database);
                editJournalsView.addViewListener(new ViewListener() {
                    public void onViewClosed(View view) {
                        view.removeViewListener(this);
                        editJournalsView = null;
                    }
                });
                viewTabbedPane.openView(editJournalsView);
                viewTabbedPane.selectView(editJournalsView);
            } else {
                viewTabbedPane.selectView(editJournalsView);
            }
	    }
	}

	private void handleGenerateInvoices() {
//	    InvoiceToPdfDialog dialog = new InvoiceToPdfDialog(this, database);
//	    dialog.showDialog();
        if (invoiceToOdtView == null) {
            invoiceToOdtView = new InvoiceToOdtView(database);
            invoiceToOdtView.addViewListener(new ViewListener() {
                public void onViewClosed(View view) {
                    view.removeViewListener(this);
                    invoiceToOdtView = null;
                }
            });
            viewTabbedPane.openView(invoiceToOdtView);
            viewTabbedPane.selectView(invoiceToOdtView);
        } else {
            viewTabbedPane.selectView(invoiceToOdtView);
        }
	}

	private void handleGenerateReport() {
	    ReportDialog dialog = new ReportDialog(this, database);
	    dialog.showDialog();
	}

	private void handlePrintAddressLabels() {
	    AddressLabelPrinter alp = new AddressLabelPrinter(database.getParties());
	    try {
            alp.printAddressLabels();
        } catch (PrinterException e) {
	        MessageDialog.showMessage(this, "gen.error",
	                TextResource.getInstance().getString("mf.problemWhilePrinting"));
        }
	}

	private void handleAddInvoices() {
        if (invoiceGeneratorView == null) {
            invoiceGeneratorView = new InvoiceGeneratorView(database);
            invoiceGeneratorView.addViewListener(new ViewListener() {
                public void onViewClosed(View view) {
                    view.removeViewListener(this);
                    invoiceGeneratorView = null;
                }
            });
            viewTabbedPane.openView(invoiceGeneratorView);
            viewTabbedPane.selectView(invoiceGeneratorView);
        } else {
            viewTabbedPane.selectView(invoiceGeneratorView);
        }
	}

	/** Shows the about dialog. */
	private void handleAbout() {
	    AboutView aboutView = new AboutView();
	    new ViewDialog(this, aboutView).showDialog();
	}

    /**
     * This method is called when the database has changed.
     * @param database the database that has changed
     */
    public void databaseChanged(Database database) {
        setTitle(createTitle());
    }
}
