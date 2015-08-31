package nl.gogognome.gogoaccount.gui;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.components.document.Document;
import nl.gogognome.gogoaccount.components.document.DocumentListener;
import nl.gogognome.gogoaccount.gui.controllers.GenerateReportController;
import nl.gogognome.gogoaccount.gui.views.*;
import nl.gogognome.gogoaccount.services.*;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.swing.views.ViewDialog;
import nl.gogognome.lib.swing.views.ViewListener;
import nl.gogognome.lib.swing.views.ViewTabbedPane;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PrinterException;
import java.io.File;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static nl.gogognome.gogoaccount.gui.ActionRunner.run;

/**
 * This class implements the main frame of the application.
 *
 * @author Sander Kooijmans
 */
public class MainFrame extends JFrame implements ActionListener, DocumentListener {

    private static final long serialVersionUID = 1L;

    /** The current database of the application. */
    private Document document;

    /** The menu bar of the application. */
	private JMenuBar menuBar = new JMenuBar();

	/** The tabbed pane containing the views. */
	private ViewTabbedPane viewTabbedPane;

	private Map<Class<?>, View> openViews = new HashMap<>();

	private TextResource textResource = Factory.getInstance(TextResource.class);
	private WidgetFactory widgetFactory = Factory.getInstance(WidgetFactory.class);

    private BookkeepingService bookkeepingService = new BookkeepingService();
	private ConfigurationService configurationService = new ConfigurationService();

	public MainFrame() {
		super();
		try {
			document = new Document();
		} catch (SQLException e) {
			throw new RuntimeException("Could not create inital database: " + e.getMessage(), e);
		}
		document.addListener(this);
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

        setMinimumSize(new Dimension(800, 600));
	}

	/**
	 * Creates the title to be shown in the title bar of the main frame.
	 * @return the title
	 */
	private String createTitle() {
	    String result = textResource.getString("mf.title");
	    String description = document.getDescription();
	    if (description != null)
	    {
	        result += " - " + description;
	        if (document.hasUnsavedChanges())
	        {
	            result += "*";
	        }
	    }
	    return result;
	}

	/** Creates the menu bar. */
	private void createMenuBar() {
		// the menus
		JMenu fileMenu = widgetFactory.createMenu("mi.file");
		JMenu editMenu = widgetFactory.createMenu("mi.edit");
		JMenu viewMenu = widgetFactory.createMenu("mi.view");
		JMenu reportingMenu = widgetFactory.createMenu("mi.reporting");
		JMenu helpMenu = widgetFactory.createMenu("mi.help");

		// the file menu
		JMenuItem miNewEdition = widgetFactory.createMenuItem("mi.newBookkeeping", this);
		JMenuItem miOpenEdition = widgetFactory.createMenuItem("mi.openBookkeeping", this);
        JMenuItem miConfigureBookkeeping = widgetFactory.createMenuItem("mi.configureBookkeeping", this);
		JMenuItem miSaveEdition = widgetFactory.createMenuItem("mi.saveBookkeeping", this);
		JMenuItem miSaveEditionAs = widgetFactory.createMenuItem("mi.saveBookkeepingAs", this);
        JMenuItem miCloseBookkeeping = widgetFactory.createMenuItem("mi.closeBookkeeping", this);
        JMenuItem miImportBankStatement = widgetFactory.createMenuItem("mi.importBankStatement", this);
		JMenuItem miExit = widgetFactory.createMenuItem("mi.exit", this);

		// the edit menu
		JMenuItem miAddJournal = widgetFactory.createMenuItem("mi.addJournal", this);
		JMenuItem miEditJournals = widgetFactory.createMenuItem("mi.editJournals", this);
		JMenuItem miAddInvoices = widgetFactory.createMenuItem("mi.addInvoices", this);
        JMenuItem miEditParties = widgetFactory.createMenuItem("mi.editParties", this);

		// the view menu
        JMenuItem miViewBalanceAndOpertaionalResult = widgetFactory.createMenuItem("mi.viewBalanceAndOperationalResult", this);
		JMenuItem miViewAccountOverview = widgetFactory.createMenuItem("mi.viewAccountOverview", this);
		JMenuItem miViewPartyOverview = widgetFactory.createMenuItem("mi.viewInvoicesOverview", this);

		// the reporting menu
		JMenuItem miGenerateInvoices = widgetFactory.createMenuItem("mi.generateInvoices", this);
		JMenuItem miGenerateReport = widgetFactory.createMenuItem("mi.generateReport", this);
//		JMenuItem miPrintAddressLabels = wf.createMenuItem("mi.printAddressLabels", this);

		// the help menu
		JMenuItem miAbout = widgetFactory.createMenuItem("mi.about", this);

		fileMenu.add(miNewEdition);
		fileMenu.add(miOpenEdition);
		fileMenu.add(miSaveEdition);
		fileMenu.add(miSaveEditionAs);
        fileMenu.add(miCloseBookkeeping);
        fileMenu.addSeparator();
		fileMenu.add(miConfigureBookkeeping);
        fileMenu.add(miImportBankStatement);
        fileMenu.addSeparator();
		fileMenu.add(miExit);

		editMenu.add(miAddJournal);
		editMenu.add(miEditJournals);
		editMenu.add(miAddInvoices);
        editMenu.add(miEditParties);

        viewMenu.add(miViewBalanceAndOpertaionalResult);
		viewMenu.add(miViewAccountOverview);
		viewMenu.add(miViewPartyOverview);

		reportingMenu.add(miGenerateInvoices);
		reportingMenu.add(miGenerateReport);
		// TODO: Improve printing of address labels before enabling this menu item
//		reportingMenu.add(miPrintAddressLabels);

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
	@Override
	public void actionPerformed(ActionEvent e)
	{
		String command = e.getActionCommand();
		if ("mi.newBookkeeping".equals(command)) { run(this, () -> handleNewEdition()); }
		if ("mi.openBookkeeping".equals(command)) { handleOpenBookkeeping(); }
		if ("mi.configureBookkeeping".equals(command)) { handleConfigureBookkeeping(); }
		if ("mi.saveBookkeeping".equals(command)) { handleSaveBookkeeping(); }
		if ("mi.saveBookkeepingAs".equals(command)) { handleSaveBookeepingAs(); }
        if ("mi.closeBookkeeping".equals(command)) { handleCloseBookkeeping(); }
        if ("mi.importBankStatement".equals(command)) { handleImportBankStatement(); }
		if ("mi.exit".equals(command)) { handleExit(); }
        if ("mi.viewBalanceAndOperationalResult".equals(command)) { handleViewBalanceAndOperationalResult(); }
		if ("mi.viewAccountOverview".equals(command)) { handleViewAccountMutations(); }
		if ("mi.viewInvoicesOverview".equals(command)) { handleViewPartyOverview(); }
		if ("mi.addJournal".equals(command)) { run(this, () -> handleAddJournal()); }
		if ("mi.editJournals".equals(command)) { run(this, () -> handleEditJournals()); }
		if ("mi.addInvoices".equals(command)) { run(this, () -> handleAddInvoices()); }
        if ("mi.editParties".equals(command)) { handleEditParties(); }
		if ("mi.generateInvoices".equals(command)) { run(this, () -> handleGenerateInvoices()); }
		if ("mi.generateReport".equals(command)) { run(this, () -> handleGenerateReport()); }
		if ("mi.printAddressLabels".equals(command)) { handlePrintAddressLabels(); }
		if ("mi.about".equals(command)) { handleAbout(); }
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
		if (document.hasUnsavedChanges())
		{
			int choice = MessageDialog.showYesNoCancelQuestion(this, "gen.titleWarning",
				"mf.saveChangesBeforeExit");
			switch (choice)	{
				case MessageDialog.YES_OPTION:
					handleSaveBookkeeping();
					result = true;
					break;
				case MessageDialog.NO_OPTION: // continue without saving
					result = true;
					break;
				case MessageDialog.CLOSED_OPTION:
				case MessageDialog.CANCEL_OPTION:
					result = false;
					break;
				default:
					throw new IllegalStateException("Unknown button pressed. Index: " + choice);
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

	private void handleNewEdition() throws ServiceException {
		if (mayCurrentDatabaseBeDestroyed()) {
            try {
                setDocument(new Document());
            } catch (SQLException e) {
                throw new ServiceException(e);
            }
            document.setDescription(textResource.getString("mf.newBookkeepingDescription"));
			document.databaseConsistentWithFile();
			handleConfigureBookkeeping();
		}
	}

	private void handleOpenBookkeeping()
	{
		if (mayCurrentDatabaseBeDestroyed())
		{
			JFileChooser fc = new JFileChooser();
			if (document.getFileName() != null) {
				fc.setCurrentDirectory(new File(document.getFileName()));
			}
			fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
			int choice = fc.showDialog(this, textResource.getString("mf.titleOpenBookkeeping"));
			if (choice == JFileChooser.APPROVE_OPTION) {
				File file = fc.getSelectedFile();
				loadFile(file.getAbsolutePath());
			}
			requestFocus();
		}
	}

	/** Handles the configure bookkeeping event. */
	private void handleConfigureBookkeeping() {
		openView(ConfigureBookkeepingView.class);
	}

	/** Handles the save bookkeeping event. */
	private void handleSaveBookkeeping()
	{
		String fileName = document.getFileName();
		if (fileName != null)
		{
		    saveBookkeeping(fileName);
		}
		else
		{
			handleSaveBookeepingAs();
		}
	}

	private void handleSaveBookeepingAs() {
		JFileChooser fc = new JFileChooser();
		if (document.getFileName() != null) {
			fc.setCurrentDirectory(new File(document.getFileName()));
		}
		fc.setFileFilter(new FileNameExtensionFilter("XML file", "xml"));
		int choice = fc.showDialog(this, textResource.getString("mf.titleSaveAs"));
		if (choice == JFileChooser.APPROVE_OPTION) {
			saveBookkeeping(fc.getSelectedFile().getAbsolutePath());
		}
		requestFocus();
	}

	/** Handles closing the bookkeeping. */
	private void handleCloseBookkeeping() {
	    if (document.hasUnsavedChanges()) {
	        handleSaveBookkeeping();
	    }
        if (document.hasUnsavedChanges()) {
            // The user did not want to save the changes. Do not close the bookkeeping.
            return;
        }

        CloseBookkeepingView cbv = new CloseBookkeepingView(document);
        new ViewDialog(this, cbv).showDialog();
        Date date = cbv.getDate();
        Account accountToAddResultTo = cbv.getAccountToAddResultTo();
        String description = cbv.getDescription();
        if (date != null && accountToAddResultTo != null) {
            try {
                Document newDocument = bookkeepingService.closeBookkeeping(document, description, date, accountToAddResultTo);
                setDocument(newDocument);
            } catch (ServiceException e) {
                MessageDialog.showErrorMessage(this, e, "mf.closeBookkeepingException");
            }
        }
	}

	private void handleImportBankStatement() {
		openView(ImportBankStatementView.class);
	}

	/**
	 * Loads a bookkeeping from an XML file.
	 * @param fileName the name of the file.
	 */
	public void loadFile(String fileName) {
        Document newDocument;
		try {
			newDocument = new XMLFileReader(new File(fileName)).createDatabaseFromFile();
		} catch (ServiceException e) {
			MessageDialog.showErrorMessage(this, e, "mf.errorOpeningFile");
            return;
        }
		newDocument.databaseConsistentWithFile();
		setDocument(newDocument);
	}

	/**
	 * Replaces the old database by the new database.
	 * @param newDocument the new database
	 */
	private void setDocument(Document newDocument) {
        document.removeListener(this);

        document = newDocument;
        document.addListener(this);
        viewTabbedPane.closeAllViews();

        for (View view : openViews.values()) {
        	view.requestClose();
        }

        documentChanged(document);
        handleViewBalanceAndOperationalResult();
	}

	/**
	 * Saves the current bookkeeping to an XML file.
	 * @param fileName the name of the file.
	 */
	private void saveBookkeeping(String fileName) {
		try {
			new XMLFileWriter(document, new File(fileName)).writeDatabaseToFile();
			document.setFileName(fileName);
			document.databaseConsistentWithFile();
		} catch (Exception e) {
			MessageDialog.showErrorMessage(this, e, "mf.saveException");
		}
	}

	/** Handles the exit event. */
	private void handleExit() {
		if (mayCurrentDatabaseBeDestroyed()) {
			document.removeListener(this);
			dispose();
		}
	}

	private void handleViewBalanceAndOperationalResult() {
		openView(BalanceAndOperationResultView.class);
    }

    private void handleEditParties() {
    	openView(PartiesView.class);
    }

	private void handleViewAccountMutations() {
		openView(AccountMutationsView.class);
	}

	private void handleViewPartyOverview() {
		openView(InvoicesPerPartyView.class);
	}

	private void handleAddJournal() throws ServiceException {
	    if (!configurationService.hasAccounts(document)) {
	        MessageDialog.showInfoMessage(this, "mf.noAccountsPresent");
	    } else {
            EditJournalView view = new EditJournalView(document, "ajd.title", null);
            ViewDialog dialog = new ViewDialog(this, view);
            dialog.showDialog();
	    }
	}

	private void handleEditJournals() throws ServiceException {
	    if (!configurationService.hasAccounts(document)) {
	        MessageDialog.showInfoMessage(this, "mf.noAccountsPresent");
	    } else {
	    	openView(EditJournalsView.class);
	    }
	}

	private void handleGenerateInvoices() throws ServiceException {
	    if (!configurationService.hasAccounts(document)) {
	        MessageDialog.showInfoMessage(this, "mf.noAccountsPresent");
	    } else {
	    	openView(InvoiceToOdtView.class);
	    }
	}

	private void handleGenerateReport() throws ServiceException {
	    if (!configurationService.hasAccounts(document)) {
	        MessageDialog.showInfoMessage(this, "mf.noAccountsPresent");
	    } else {
		    GenerateReportController controller = new GenerateReportController(document, this);
		    controller.execute();
	    }
	}

	private void handlePrintAddressLabels() {
	    AddressLabelPrinter alp = new AddressLabelPrinter(document.getParties());
	    try {
            alp.printAddressLabels();
        } catch (PrinterException e) {
	        MessageDialog.showMessage(this, "gen.error", "mf.problemWhilePrinting");
        }
	}

	private void handleAddInvoices() throws ServiceException {
        if (!configurationService.hasAccounts(document)) {
            MessageDialog.showInfoMessage(this, "mf.noAccountsPresent");
        } else {
            openView(InvoiceGeneratorView.class);
        }
	}

	/** Shows the about dialog. */
	private void handleAbout() {
	    AboutView aboutView = new AboutView();
	    new ViewDialog(this, aboutView).showDialog();
	}

	private void openView(Class<? extends View> viewClass) {
		View view = openViews.get(viewClass);
	    if (view == null) {
	    	try {
				view = createView(viewClass);
			} catch (Exception e) {
				MessageDialog.showErrorMessage(this, e, "mf.problemCreatingView");
				return;
			}
	    	view.addViewListener(new ViewCloseListener());
            viewTabbedPane.openView(view);
            viewTabbedPane.selectView(view);

            openViews.put(viewClass, view);
        } else {
            viewTabbedPane.selectView(view);
	    }
	}

    private View createView(Class<? extends View> viewClass) throws Exception {
    	Constructor<? extends View> c = viewClass.getConstructor(Document.class);
    	return c.newInstance(document);
	}

	/**
     * This method is called when the database has changed.
     * @param document the database that has changed
     */
    @Override
	public void documentChanged(Document document) {
        setTitle(createTitle());
    }

    private class ViewCloseListener implements ViewListener {
        @Override
		public void onViewClosed(View view) {
            view.removeViewListener(this);
            openViews.remove(view.getClass());
        }
    }

}
