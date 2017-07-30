package nl.gogognome.gogoaccount.gui;

import nl.gogognome.gogoaccount.component.configuration.Account;
import nl.gogognome.gogoaccount.component.configuration.Bookkeeping;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentListener;
import nl.gogognome.gogoaccount.component.document.DocumentService;
import nl.gogognome.gogoaccount.gui.configuration.EmailConfigurationView;
import nl.gogognome.gogoaccount.gui.controllers.GenerateReportController;
import nl.gogognome.gogoaccount.gui.invoice.InvoiceGeneratorView;
import nl.gogognome.gogoaccount.gui.invoice.InvoicesView;
import nl.gogognome.gogoaccount.gui.views.*;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.dialogs.MessageDialog;
import nl.gogognome.lib.swing.views.*;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static nl.gogognome.gogoaccount.gui.ActionRunner.run;

public class MainFrame extends JFrame implements ActionListener, DocumentListener {

    private static final long serialVersionUID = 1L;

    private final Logger logger = LoggerFactory.getLogger(getClass());

    /** The current database of the application. */
    private Document document;

    /** The menu bar of the application. */
    private JMenuBar menuBar = new JMenuBar();

    /** The tabbed pane containing the views. */
    private ViewTabbedPane viewTabbedPane;

    private Map<Class<?>, View> openViews = new HashMap<>();

    private TextResource textResource = Factory.getInstance(TextResource.class);
    private WidgetFactory widgetFactory = Factory.getInstance(WidgetFactory.class);

    private final BookkeepingService bookkeepingService;
    private final DocumentService documentService;
    private final ConfigurationService configurationService;
    private final ViewFactory viewFactory;
    private final ControllerFactory controllerFactory;
    private final DocumentRegistry documentRegistry;
    private final ResourceLoader resourceLoader;
    private final MessageDialog messageDialog;
    private final HandleException handleException;

    public MainFrame(BookkeepingService bookkeepingService, DocumentService documentService, ConfigurationService configurationService,
                     ViewFactory viewFactory, ControllerFactory controllerFactory, DocumentRegistry documentRegistry,
                     ResourceLoader resourceLoader) {
        this.bookkeepingService = bookkeepingService;
        this.documentService = documentService;
        this.configurationService = configurationService;
        this.viewFactory = viewFactory;
        this.controllerFactory = controllerFactory;
        this.documentRegistry = documentRegistry;
        this.resourceLoader = resourceLoader;
        messageDialog = new MessageDialog(textResource, this);
        handleException = new HandleException(messageDialog);

        createMenuBar();
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        viewTabbedPane = new ViewTabbedPane(new JFrameViewOwner(this));
        getContentPane().add(viewTabbedPane);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { handleExit(); } }
        );

        setTitle(createTitle());

        setIcon();
        setMinimumSize(new Dimension(800, 600));
    }

    private void setIcon() {
        try {
            Resource imageResource = resourceLoader.getResource("icon-32x32.png");
            Image image = Toolkit.getDefaultToolkit().createImage(imageResource.getURL());
            setIconImage(image);
        } catch (IOException e) {
            logger.warn("Failed to load icon", e);
        }
    }

    /**
     * Creates the title to be shown in the title bar of the main frame.
     * @return the title
     */
    private String createTitle() {
        String result = textResource.getString("mf.title");
        String description = null;
        if (document != null) {
            try {
                Bookkeeping bookkeeping = configurationService.getBookkeeping(document);
                description = bookkeeping.getDescription();
                if (document.isReadonly()) {
                    description += " (" + textResource.getString("mf.readonly") + ")";
                }
            } catch (ServiceException e) {
                logger.warn("Failed to get bookkeeping: " + e.getMessage(), e);
            }
        }
        if (description != null) {
            result += " - " + description;
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
        JMenuItem miNewEdition = widgetFactory.createMenuItem("mi.newBookkeeping", e -> handleNewEdition());
        JMenuItem miOpenEdition = widgetFactory.createMenuItem("mi.openBookkeeping", e -> handleOpenBookkeeping());
        JMenuItem miConfigureBookkeeping = widgetFactory.createMenuItem("mi.configureBookkeeping", this);
        JMenuItem miConfigureEmail = widgetFactory.createMenuItem("mi.configureEmail", e -> onConfigureEmail());
        JMenuItem miCloseBookkeeping = widgetFactory.createMenuItem("mi.closeBookkeeping", e -> handleCloseBookkeeping());
        JMenuItem miImportBankStatement = widgetFactory.createMenuItem("mi.importBankStatement", this);
        JMenuItem miExit = widgetFactory.createMenuItem("mi.exit", this);

        // the edit menu
        JMenuItem miAddJournal = widgetFactory.createMenuItem("mi.addJournal", this);
        JMenuItem miEditJournals = widgetFactory.createMenuItem("mi.editJournals", this);
        JMenuItem miAddInvoices = widgetFactory.createMenuItem("mi.addInvoices", this);
        JMenuItem miEditParties = widgetFactory.createMenuItem("mi.editParties", this);

        // the view menu
        JMenuItem miBalanceAndOperationalResult = widgetFactory.createMenuItem("mi.viewBalanceAndOperationalResult", this);
        JMenuItem miAccountOverview = widgetFactory.createMenuItem("mi.viewAccountOverview", this);
        JMenuItem miInvoiceOverview = widgetFactory.createMenuItem("mi.viewInvoicesOverview", e -> handleInvoiceOverview());

        // the reporting menu
        JMenuItem miGenerateInvoices = widgetFactory.createMenuItem("mi.generateInvoices", e -> handleGenerateInvoices());
        JMenuItem miGenerateReport = widgetFactory.createMenuItem("mi.generateReport", e -> handleGenerateReport());

        // the help menu
        JMenuItem miAbout = widgetFactory.createMenuItem("mi.about", this);

        fileMenu.add(miNewEdition);
        fileMenu.add(miOpenEdition);
        fileMenu.add(miCloseBookkeeping);
        fileMenu.addSeparator();
        fileMenu.add(miConfigureBookkeeping);
        fileMenu.add(miConfigureEmail);
        fileMenu.add(miImportBankStatement);
        fileMenu.addSeparator();
        fileMenu.add(miExit);

        editMenu.add(miAddJournal);
        editMenu.add(miEditJournals);
        editMenu.add(miAddInvoices);
        editMenu.add(miEditParties);

        viewMenu.add(miBalanceAndOperationalResult);
        viewMenu.add(miAccountOverview);
        viewMenu.add(miInvoiceOverview);

        reportingMenu.add(miGenerateInvoices);
        reportingMenu.add(miGenerateReport);

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
        if ("mi.configureBookkeeping".equals(command)) { handleConfigureBookkeeping(); }
        if ("mi.importBankStatement".equals(command)) { handleImportBankStatement(); }
        if ("mi.exit".equals(command)) { handleExit(); }
        if ("mi.viewBalanceAndOperationalResult".equals(command)) { handleViewBalanceAndOperationalResult(); }
        if ("mi.viewAccountOverview".equals(command)) { handleViewAccountMutations(); }
        if ("mi.addJournal".equals(command)) { run(this, this::handleAddJournal); }
        if ("mi.editJournals".equals(command)) { run(this, this::handleEditJournals); }
        if ("mi.addInvoices".equals(command)) { run(this, this::handleAddInvoices); }
        if ("mi.editParties".equals(command)) { handleEditParties(); }
        if ("mi.about".equals(command)) { handleAbout(); }
    }

    public void handleNewEdition() {
        handleException.of("mf.failedToCreateNewBookkeeping", () -> {
            File file = askUserForFileOfNewBookkeeping();
            if (file == null) {
                return;
            }
            boolean existingBookkeeping = false;
            Document newDocument;
            if (file.exists()) {
                newDocument = doLoadFile(file);
                existingBookkeeping = true;
            } else {
                if (file.getName().endsWith(".h2.db")) {
                    file = new File(file.getParentFile(), file.getName().substring(0, file.getName().length() - 6));
                }
                newDocument = documentService.createNewDocument(file, textResource.getString("mf.newBookkeepingDescription"));
            }
            if (newDocument != null) {
                setDocument(newDocument);
                if (!existingBookkeeping) {
                    handleConfigureBookkeeping();
                }
            }
        });
    }

    private File askUserForFileOfNewBookkeeping() {
        String extension = ".h2.db";
        File directory = document != null ? new File(document.getFileName()).getParentFile() : null;
        JFileChooser fc = new JFileChooser(directory);
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String filename = f.getName().toLowerCase();
                return f.isDirectory() || filename.endsWith(extension);
            }

            @Override
            public String getDescription() {
                return textResource.getString("mf.fileSelection.description");
            }
        });

        int choice = fc.showDialog(this, textResource.getString("mf.titleNewBookkeeping"));
        if (choice != JFileChooser.APPROVE_OPTION) {
            return null;
        }
        File newFile = fc.getSelectedFile();
        if (newFile.getName().endsWith(extension + extension)) {
            newFile = new File(newFile.getParent(), newFile.getName().substring(0, newFile.getName().length() - extension.length()));
        }
        return newFile;
    }

    private void handleOpenBookkeeping()
    {
        JFileChooser fc = new JFileChooser();
        if (document != null && document.getFileName() != null) {
            fc.setCurrentDirectory(new File(document.getFileName()));
        }
        fc.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String filename = f.getName().toLowerCase();
                return f.isDirectory() || filename.endsWith(".xml") || filename.endsWith(".h2.db");
            }

            @Override
            public String getDescription() {
                return textResource.getString("mf.fileSelection.description");
            }
        });
        int choice = fc.showDialog(this, textResource.getString("mf.titleOpenBookkeeping"));
        if (choice == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            loadFile(file);
        }
        requestFocus();
    }

    private void handleConfigureBookkeeping() {
        handleException.of(() -> {
            if (document == null ) {
                messageDialog.showInfoMessage("mf.noBookkeepingPresent");
            } else {
                openView(ConfigureBookkeepingView.class);
            }
        });
    }

    private void onConfigureEmail() {
        handleException.of(() -> {
            if (document == null ) {
                messageDialog.showInfoMessage("mf.noBookkeepingPresent");
            } else {
                openViewInDialog(EmailConfigurationView.class);
            }
        });
    }

    private void handleCloseBookkeeping() {
        ensureAccountsPresent(() -> {
            CloseBookkeepingView cbv = new CloseBookkeepingView(document, configurationService);
            new ViewDialog(this, cbv).showDialog();
            Date date = cbv.getDate();
            Account accountToAddResultTo = cbv.getAccountToAddResultTo();
            String description = cbv.getDescription();
            if (date != null && accountToAddResultTo != null) {
                try {
                    File file = askUserForFileOfNewBookkeeping();
                    if (file == null) {
                        return;
                    }
                    Document newDocument = bookkeepingService.closeBookkeeping(document, file, description, date, accountToAddResultTo);
                    setDocument(newDocument);
                    handleViewBalanceAndOperationalResult();
                } catch (ServiceException e) {
                    messageDialog.showErrorMessage(e, "mf.closeBookkeepingException");
                }
            }
        });
    }

    private void handleImportBankStatement() {
        ensureAccountsPresent(() -> openView(ImportBankStatementView.class));
    }

    public void loadFile(File file) {
        try {
            Document newDocument = doLoadFile(file);
            newDocument.notifyChange();
            setDocument(newDocument);
            handleViewBalanceAndOperationalResult();
        } catch (ServiceException e) {
            messageDialog.showErrorMessage(e, "mf.errorOpeningFile");
        }
    }

    private Document doLoadFile(File file) throws ServiceException {
        String filename = file.getAbsolutePath();
        Document newDocument = documentService.openDocument(filename);

        Currency currency = configurationService.getBookkeeping(newDocument).getCurrency();
        Factory.bindSingleton(AmountFormat.class, new AmountFormat(Factory.getInstance(Locale.class), currency));
        return newDocument;
    }

    /**
     * Replaces the old database by the new database.
     * @param newDocument the new database
     */
    private void setDocument(Document newDocument) throws ServiceException {
        viewTabbedPane.closeAllViews();

        openViews.values().forEach(View::requestClose);

        if (document != null) {
            document.removeListener(this);
        }

        document = newDocument;
        if (document != null) {
            document.addListener(this);
            document.setLocale(Locale.getDefault());
        }

        documentRegistry.register(document);

        documentChanged(document);
    }

    private void handleExit() {
        if (document != null) {
            document.removeListener(this);
        }
        dispose();
    }

    private void handleViewBalanceAndOperationalResult() {
        ensureAccountsPresent(() -> openView(BalanceAndOperationResultView.class));
    }

    private void handleEditParties() {
        ensureAccountsPresent(() -> openView(PartiesView.class));
    }

    private void handleViewAccountMutations() {
        ensureAccountsPresent(() -> openView(AccountMutationsView.class));
    }

    private void handleInvoiceOverview() {
        ensureAccountsPresent(() -> openView(InvoicesView.class));
    }

    private void handleAddJournal() throws ServiceException {
        ensureAccountsPresent(() -> {
            viewFactory.createView(EditJournalView.class);
            EditJournalView view = (EditJournalView) viewFactory.createView(EditJournalView.class);
            view.setTitle("ajd.title");
            ViewDialog dialog = new ViewDialog(this, view);
            dialog.showDialog();
        });
    }

    private void handleEditJournals() throws ServiceException {
        ensureAccountsPresent(() -> openView(EditJournalsView.class));
    }

    private void handleGenerateInvoices() {
        ensureAccountsPresent(() -> openView(InvoiceToOdtView.class));
    }

    private void handleGenerateReport() {
        ensureAccountsPresent(() -> {
            GenerateReportController generateReportController = (GenerateReportController) controllerFactory.createController(GenerateReportController.class);
            generateReportController.setViewOwner(new JFrameViewOwner(this));
            generateReportController.execute();
        });
    }

    private void handleAddInvoices() throws ServiceException {
        ensureAccountsPresent(() -> openView(InvoiceGeneratorView.class));
    }

    private void handleAbout() {
        handleException.of(() -> new ViewDialog(this, new AboutView()).showDialog());
    }

    private void openView(Class<? extends View> viewClass) {
        handleException.of(() -> {
            View view = openViews.get(viewClass);
            if (view == null) {
                try {
                    view = viewFactory.createView(viewClass);
                } catch (Exception e) {
                    messageDialog.showErrorMessage(e, "mf.problemCreatingView");
                    return;
                }
                view.addViewListener(new ViewCloseListener());
                viewTabbedPane.openView(view);
                viewTabbedPane.selectView(view);

                openViews.put(viewClass, view);
            } else {
                viewTabbedPane.selectView(view);
            }
        });
    }

    private void openViewInDialog(Class<? extends View> viewClass) {
        handleException.of(() -> {
            try {
                View view = viewFactory.createView(viewClass);
                ViewDialog viewDialog = new ViewDialog(this, view);
                viewDialog.showDialog();
            } catch (Exception e) {
                messageDialog.showErrorMessage(e, "mf.problemCreatingView");
            }
        });
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

    private void ensureAccountsPresent(HandleException.RunnableWithException runnable) {
        handleException.of(() -> {
            if (document == null) {
                messageDialog.showInfoMessage("mf.noBookkeepingPresent");
            } else if (!configurationService.hasAccounts(document)) {
                messageDialog.showInfoMessage("mf.noAccountsPresent");
            } else {
                runnable.run();
            }
        });
    }

}
