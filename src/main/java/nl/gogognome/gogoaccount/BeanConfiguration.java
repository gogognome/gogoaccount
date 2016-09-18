package nl.gogognome.gogoaccount;

import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionService;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentService;
import nl.gogognome.gogoaccount.component.importer.ImportBankStatementService;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.amountformula.AmountFormulaParser;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.settings.SettingsService;
import nl.gogognome.gogoaccount.gui.DocumentRegistry;
import nl.gogognome.gogoaccount.gui.MainFrame;
import nl.gogognome.gogoaccount.gui.TextResourceRegistry;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.controllers.DeleteJournalController;
import nl.gogognome.gogoaccount.gui.controllers.EditJournalController;
import nl.gogognome.gogoaccount.gui.invoice.EditInvoiceController;
import nl.gogognome.gogoaccount.gui.invoice.EditInvoiceView;
import nl.gogognome.gogoaccount.gui.invoice.InvoiceGeneratorView;
import nl.gogognome.gogoaccount.gui.invoice.InvoicesView;
import nl.gogognome.gogoaccount.gui.views.*;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;

import java.util.Currency;
import java.util.Locale;

@Configuration
public class BeanConfiguration {

    @Bean
    public MainFrame mainFrame(BookkeepingService bookkeepingService, DocumentService documentService, ConfigurationService configurationService,
                               ViewFactory viewFactory, DocumentRegistry documentRegistry, ResourceLoader resourceLoader) {
        return new MainFrame(bookkeepingService, documentService, configurationService, viewFactory, documentRegistry, resourceLoader);
    }

    @Bean
    public ViewFactory viewFactory(BeanFactory beanFactory) {
        return new ViewFactory() {
            @Override
            public View createView(Class<? extends View> viewClass) {
                try {
                    return beanFactory.getBean(viewClass);
                } catch (Exception e) {
                    throw new RuntimeException("Could not create instance of view " + viewClass.getName(), e);
                }
            }
        };
    }

    public static class DocumentWrapper {
        public Document document;
    }

    @Bean
    public DocumentWrapper documentWrapper() {
        return new DocumentWrapper();
    }

    public static class AmountFormatWrapper {
        public AmountFormat amountFormat;
    }

    @Bean
    public AmountFormatWrapper amountFormatWrapper() {
        return new AmountFormatWrapper();
    }

    @Bean
    public DocumentRegistry documentRegistry(ConfigurableListableBeanFactory beanFactory, ConfigurationService configurationService,
                                             DocumentWrapper documentWrapper, AmountFormatWrapper amountFormatWrapper) {
        return new DocumentRegistry() {
            @Override
            public void register(Document document) {
                if (document != null) {
                    try {
                        Currency currency = configurationService.getBookkeeping(document).getCurrency();

                        documentWrapper.document = document;
                        amountFormatWrapper.amountFormat = new AmountFormat(document.getLocale(), currency);
                    } catch (ServiceException e) {
                        throw new RuntimeException("Could not get currency", e);
                    }
                } else {
                    documentWrapper.document = null;
                    amountFormatWrapper.amountFormat = null;
                }
            }
        };
    }

    @Bean
    public TextResource textResource() {
        return new TextResource(Locale.US);
    }

    @Bean
    public AmountFormat amountFormat() {
        return new AmountFormat(Locale.US, Currency.getInstance("EUR"));
    }

    @Bean
    public TextResourceRegistry textResourceRegistry(TextResourceWrapper textResourceWrapper) {
        return new TextResourceRegistry() {
            @Override
            public void register(TextResource textResource) {
                textResourceWrapper.textResource = textResource;
            }
        };
    }

    @Bean
    public TextResourceWrapper textResourceWrapper() {
        return new TextResourceWrapper();
    }

    public static class TextResourceWrapper {
        public TextResource textResource;
    }

    @Bean
    @Scope("prototype")
    public AccountMutationsView accountMutationsView(DocumentWrapper documentWrapper, BookkeepingService bookkeepingService) {
        return new AccountMutationsView(documentWrapper.document, bookkeepingService);
    }

    @Bean
    @Scope("prototype")
    public BalanceAndOperationResultView balanceAndOperationResultView(DocumentWrapper documentWrapper, BookkeepingService bookkeepingService) {
        return new BalanceAndOperationResultView(documentWrapper.document, bookkeepingService);
    }

    @Bean
    @Scope("prototype")
    public ConfigureBookkeepingView configureBookkeepingView(DocumentWrapper documentWrapper,
                                                             AutomaticCollectionService automaticCollectionService,
                                                             ConfigurationService configurationService,
                                                             LedgerService ledgerService) {
        return new ConfigureBookkeepingView(documentWrapper.document, automaticCollectionService, configurationService, ledgerService);
    }

    @Bean
    @Scope("prototype")
    public EditInvoiceView editInvoiceView(DocumentWrapper documentWrapper, AmountFormatWrapper amountFormatWrapper, ConfigurationService configurationService,
                                           InvoiceService invoiceService, PartyService partyService, ViewFactory viewFactory) {
        return new EditInvoiceView(documentWrapper.document, amountFormatWrapper.amountFormat, configurationService, invoiceService, partyService, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public EditJournalView editJournalView(DocumentWrapper documentWrapper, ConfigurationService configurationService,
                                           InvoiceService invoiceService, LedgerService ledgerService, PartyService partyService,
                                           ViewFactory viewFactory) {
        return new EditJournalView(documentWrapper.document, configurationService, invoiceService, ledgerService, partyService, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public EditJournalEntryDetailView editJournalEntryDetailView(DocumentWrapper documentWrapper, ConfigurationService configurationService,
                                           InvoiceService invoiceService, PartyService partyService, ViewFactory viewFactory) {
        return new EditJournalEntryDetailView(documentWrapper.document, configurationService, invoiceService, partyService, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public EditJournalsView editJournalsView(DocumentWrapper documentWrapper, ConfigurationService configurationService,
                                           InvoiceService invoiceService, LedgerService ledgerService, PartyService partyService,
                                             ViewFactory viewFactory, DeleteJournalController deleteJournalController,  EditJournalController editJournalController) {
        return new EditJournalsView(documentWrapper.document, configurationService, invoiceService, ledgerService,
                partyService, viewFactory, deleteJournalController, editJournalController);
    }

    @Bean
    @Scope("prototype")
    public GenerateAutomaticCollectionFileView generateAutomaticCollectionFileView(DocumentWrapper documentWrapper,
                                                                                   AutomaticCollectionService automaticCollectionService,
                                                                                   ConfigurationService configurationService,
                                                                                   ViewFactory viewFactory) {
        return new GenerateAutomaticCollectionFileView(documentWrapper.document, automaticCollectionService, configurationService, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public InvoicesView invoicesView(DocumentWrapper documentWrapper, AmountFormat amountFormat, InvoiceService invoiceService,
                                     EditInvoiceController editInvoiceController) {
        return new InvoicesView(documentWrapper.document, amountFormat, invoiceService, editInvoiceController);
    }

    @Bean
    @Scope("prototype")
    public ImportBankStatementView importBankStatementView(DocumentWrapper documentWrapper, AmountFormat amountFormat,
                                                           ConfigurationService configurationService, LedgerService ledgerService,
                                                           ImportBankStatementService importBankStatementService,
                                                           InvoiceService invoiceService, PartyService partyService,
                                                           SettingsService settingsService, ViewFactory viewFactory,
                                                           DeleteJournalController deleteJournalController, EditJournalController editJournalController) {
        return new ImportBankStatementView(documentWrapper.document, amountFormat, configurationService,
                importBankStatementService, invoiceService, ledgerService, partyService, settingsService, viewFactory, deleteJournalController, editJournalController);
    }

    @Bean
    @Scope("prototype")
    public InvoiceEditAndSelectionView invoiceEditAndSelectionView(DocumentWrapper documentWrapper, AmountFormatWrapper amountFormatWrapper,
                                                                   InvoiceService invoiceService, PartyService partyService) {
        return new InvoiceEditAndSelectionView(documentWrapper.document, amountFormatWrapper.amountFormat, invoiceService, partyService);
    }

    @Bean
    @Scope("prototype")
    public InvoiceGeneratorView invoiceGeneratorView(DocumentWrapper documentWrapper, AmountFormulaParser amountFormulaParser,
                                                     LedgerService ledgerService, ViewFactory viewFactory) {
        return new InvoiceGeneratorView(ledgerService, documentWrapper.document, amountFormulaParser, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public InvoiceToOdtView invoiceToOdtView(DocumentWrapper documentWrapper, ViewFactory viewFactory) {
        return new InvoiceToOdtView(documentWrapper.document, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public PartiesView partiesView(DocumentWrapper documentWrapper, AutomaticCollectionService automaticCollectionService,
                                   PartyService partyService) {
        return new PartiesView(documentWrapper.document, automaticCollectionService, partyService);
    }

    @Bean
    @Scope("prototype")
    public DeleteJournalController deleteJournalController(DocumentWrapper documentWrapper, InvoiceService invoiceService,
                                                       LedgerService ledgerService) {
        return new DeleteJournalController(documentWrapper.document, invoiceService, ledgerService);
    }

    @Bean
    @Scope("prototype")
    public EditJournalController editJournalController(DocumentWrapper documentWrapper, InvoiceService invoiceService,
                                                       LedgerService ledgerService, ViewFactory viewFactory) {
        return new EditJournalController(documentWrapper.document, invoiceService, ledgerService, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public EditInvoiceController editInvoiceController(DocumentWrapper documentWrapper, InvoiceService invoiceService,
                                                       LedgerService ledgerService, ViewFactory viewFactory) {
        return new EditInvoiceController(documentWrapper.document, invoiceService, ledgerService, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public AutomaticCollectionService automaticCollectionService(ConfigurationService configurationService,
                                                                 LedgerService ledgerService, PartyService partyService) {
        return new AutomaticCollectionService(configurationService, ledgerService, partyService);
    }

    @Bean
    @Scope("prototype")
    public BookkeepingService bookkeepingService(LedgerService ledgerService, ConfigurationService configurationService,
                                                 InvoiceService invoiceService, PartyService partyService) {
        return new BookkeepingService(ledgerService, configurationService, invoiceService, partyService);
    }

    @Bean
    @Scope("prototype")
    public ConfigurationService configurationService(LedgerService ledgerService) {
        ConfigurationService configurationService = new ConfigurationService();
        configurationService.setLedgerService(ledgerService);
        return configurationService;
    }

    @Bean
    @Scope("prototype")
    public DocumentService documentService() {
        return new DocumentService();
    }

    @Bean
    @Scope("prototype")
    public ImportBankStatementService importBankStatementService() {
        return new ImportBankStatementService();
    }

    @Bean
    @Scope("prototype")
    public InvoiceService invoiceService(AmountFormat amountFormat, PartyService partyService) {
        return new InvoiceService(amountFormat, partyService);
    }

    @Bean
    @Scope("prototype")
    public LedgerService ledgerService(TextResourceWrapper textResourceWrapper, InvoiceService invoiceService, PartyService partyService) {
        return new LedgerService(textResourceWrapper.textResource, invoiceService, partyService);
    }

    @Bean
    @Scope("prototype")
    public PartyService partyService() {
        return new PartyService();
    }

    @Bean
    @Scope("prototype")
    public SettingsService settingsService() {
        return new SettingsService();
    }

    @Bean
    @Scope("prototype")
    public AmountFormulaParser amountFormulaParser(AmountFormatWrapper amountFormatWrapper) {
        return new AmountFormulaParser(amountFormatWrapper.amountFormat);
    }

}
