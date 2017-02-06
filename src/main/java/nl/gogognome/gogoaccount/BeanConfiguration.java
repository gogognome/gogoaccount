package nl.gogognome.gogoaccount;

import nl.gogognome.gogoaccount.component.automaticcollection.AutomaticCollectionService;
import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentService;
import nl.gogognome.gogoaccount.component.email.EmailService;
import nl.gogognome.gogoaccount.component.importer.ImportBankStatementService;
import nl.gogognome.gogoaccount.component.invoice.InvoicePreviewTemplate;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.amountformula.AmountFormulaParser;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.component.settings.SettingsService;
import nl.gogognome.gogoaccount.component.text.KeyValueReplacer;
import nl.gogognome.gogoaccount.gui.DocumentRegistry;
import nl.gogognome.gogoaccount.gui.MainFrame;
import nl.gogognome.gogoaccount.gui.TextResourceRegistry;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.configuration.EmailConfigurationView;
import nl.gogognome.gogoaccount.gui.controllers.DeleteJournalController;
import nl.gogognome.gogoaccount.gui.controllers.EditJournalController;
import nl.gogognome.gogoaccount.gui.controllers.GenerateReportController;
import nl.gogognome.gogoaccount.gui.invoice.*;
import nl.gogognome.gogoaccount.gui.views.*;
import nl.gogognome.gogoaccount.reportgenerators.InvoicesToModelConverter;
import nl.gogognome.gogoaccount.reportgenerators.ReportToModelConverter;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.gogoaccount.services.XMLFileReader;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import org.springframework.beans.factory.BeanFactory;
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
                               ViewFactory viewFactory, ControllerFactory controllerFactory, DocumentRegistry documentRegistry,
                               ResourceLoader resourceLoader, XMLFileReader xmlFileReader) {
        return new MainFrame(bookkeepingService, documentService, configurationService, viewFactory, controllerFactory, documentRegistry,
                resourceLoader, xmlFileReader);
    }

    @Bean
    public ViewFactory viewFactory(BeanFactory beanFactory) {
        return viewClass -> {
            try {
                String beanName = viewClass.getSimpleName();
                beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
                return beanFactory.getBean(beanName, viewClass);
            } catch (Exception e) {
                throw new RuntimeException("Could not create instance of view " + viewClass.getName(), e);
            }
        };
    }

    @Bean
    public ControllerFactory controllerFactory(BeanFactory beanFactory) {
        return controllerClass -> {
            try {
                String beanName = controllerClass.getSimpleName();
                beanName = Character.toLowerCase(beanName.charAt(0)) + beanName.substring(1);
                return beanFactory.getBean(beanName, controllerClass);
            } catch (Exception e) {
                throw new RuntimeException("Could not create instance of controller " + controllerClass.getName(), e);
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
    public DocumentRegistry documentRegistry(ConfigurationService configurationService,
                                             DocumentWrapper documentWrapper, AmountFormatWrapper amountFormatWrapper) {
        return document -> {
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
        return textResource -> textResourceWrapper.textResource = textResource;
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
    public AddJournalForTransactionView addJournalForTransactionView(DocumentWrapper documentWrapper,
             ConfigurationService configurationService, ImportBankStatementService importBankStatementService,
             InvoiceService invoiceService, LedgerService ledgerService,
             PartyService partyService, ViewFactory viewFactory) {
        return new AddJournalForTransactionView(documentWrapper.document, configurationService, importBankStatementService,
                invoiceService, ledgerService, partyService, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public AccountMutationsView accountMutationsView(DocumentWrapper documentWrapper, BookkeepingService bookkeepingService,
                                                     ConfigurationService configurationService, PartyService partyService) {
        return new AccountMutationsView(documentWrapper.document, bookkeepingService, configurationService, partyService);
    }

    @Bean
    @Scope("prototype")
    public BalanceAndOperationResultView balanceAndOperationResultView(DocumentWrapper documentWrapper, BookkeepingService bookkeepingService) {
        return new BalanceAndOperationResultView(documentWrapper.document, bookkeepingService);
    }

    @Bean
    @Scope("prototype")
    public CloseBookkeepingView closeBookkeepingView(DocumentWrapper documentWrapper, ConfigurationService configurationService) {
        return new CloseBookkeepingView(documentWrapper.document, configurationService);
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
    public EmailConfigurationView emailConfigurationView(DocumentWrapper documentWrapper, TextResourceWrapper textResourceWrapper,
                                                         EmailService emailService) {
        return new EmailConfigurationView(documentWrapper.document, textResourceWrapper.textResource, emailService);
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
    public EditPartyView editPartyView(DocumentWrapper documentWrapper, ConfigurationService configurationService,
                                           PartyService partyService) {
        return new EditPartyView(documentWrapper.document, configurationService, partyService);
    }

    @Bean
    @Scope("prototype")
    public EmailInvoicesView emailInvoicesView(DocumentWrapper documentWrapper, EmailService emailService,
                                               InvoiceService invoiceService, InvoicePreviewTemplate invoicePreviewTemplate,
                                               SettingsService settingsService) {
        return new EmailInvoicesView(documentWrapper.document, emailService, invoiceService, invoicePreviewTemplate, settingsService);
    }

    @Bean
    @Scope("prototype")
    public ExportPdfsInvoicesView exportPdfsInvoicesView(DocumentWrapper documentWrapper, InvoiceService invoiceService,
                                                         InvoicePreviewTemplate invoicePreviewTemplate,
                                                         SettingsService settingsService, PdfGenerator pdfGenerator) {
        return new ExportPdfsInvoicesView(documentWrapper.document, invoiceService, invoicePreviewTemplate, settingsService, pdfGenerator);
    }

    @Bean
    @Scope("prototype")
    public GenerateAutomaticCollectionFileView generateAutomaticCollectionFileView(DocumentWrapper documentWrapper,
                                                                                   AutomaticCollectionService automaticCollectionService,
                                                                                   ConfigurationService configurationService) {
        return new GenerateAutomaticCollectionFileView(documentWrapper.document, automaticCollectionService, configurationService);
    }

    @Bean
    @Scope("prototype")
    public GenerateReportView generateReportView(DocumentWrapper documentWrapper, ConfigurationService configurationService) {
        return new GenerateReportView(documentWrapper.document, configurationService);
    }

    @Bean
    @Scope("prototype")
    public InvoicesView invoicesView(DocumentWrapper documentWrapper, AmountFormat amountFormat, AutomaticCollectionService automaticCollectionService,
                                     ConfigurationService configurationService, InvoiceService invoiceService, PartyService partyService,
                                     EditInvoiceController editInvoiceController,
                                     ViewFactory viewFactory) {
        return new InvoicesView(documentWrapper.document, amountFormat, automaticCollectionService, configurationService, invoiceService, partyService, editInvoiceController, viewFactory);
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
                                                     ConfigurationService configurationService, LedgerService ledgerService, ViewFactory viewFactory) {
        return new InvoiceGeneratorView(documentWrapper.document, configurationService, ledgerService, amountFormulaParser, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public InvoiceToOdtView invoiceToOdtView(DocumentWrapper documentWrapper, ViewFactory viewFactory,
                                             InvoicesToModelConverter invoicesToModelConverter) {
        return new InvoiceToOdtView(documentWrapper.document, viewFactory, invoicesToModelConverter);
    }

    @Bean
    @Scope("prototype")
    public PartiesView partiesView(DocumentWrapper documentWrapper, AutomaticCollectionService automaticCollectionService,
                                   PartyService partyService, ViewFactory viewFactory) {
        return new PartiesView(documentWrapper.document, automaticCollectionService, partyService, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public PrintInvoicesView printInvoicesView(DocumentWrapper documentWrapper, InvoiceService invoiceService,
                                               InvoicePreviewTemplate invoicePreviewTemplate,
                                               SettingsService settingsService, PdfGenerator pdfGenerator) {
        return new PrintInvoicesView(documentWrapper.document, invoiceService, invoicePreviewTemplate, settingsService, pdfGenerator);
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
                                                       LedgerService ledgerService, ViewFactory viewFactory,
                                                       TextResourceWrapper textResourceWrappere) {
        return new EditJournalController(documentWrapper.document, invoiceService, ledgerService, viewFactory, textResourceWrappere.textResource);
    }

    @Bean
    @Scope("prototype")
    public EditInvoiceController editInvoiceController(DocumentWrapper documentWrapper, InvoiceService invoiceService,
                                                       LedgerService ledgerService, ViewFactory viewFactory,
                                                       TextResourceWrapper textResourceWrapper) {
        return new EditInvoiceController(documentWrapper.document, invoiceService, ledgerService, viewFactory, textResourceWrapper.textResource);
    }

    @Bean
    @Scope("prototype")
    public GenerateReportController generateReportController(DocumentWrapper documentWrapper, AmountFormatWrapper amountFormatWrapper,
                                                             TextResourceWrapper textResourceWrapper, BookkeepingService bookkeepingService,
                                                             ConfigurationService configurationService, InvoiceService invoiceService,
                                                             LedgerService ledgerService, PartyService partyService,
                                                             ReportToModelConverter reportToModelConverter, ViewFactory viewFactory) {
        return new GenerateReportController(documentWrapper.document, amountFormatWrapper.amountFormat, textResourceWrapper.textResource,
                bookkeepingService, configurationService, invoiceService, ledgerService, partyService, reportToModelConverter, viewFactory);
    }

    @Bean
    @Scope("prototype")
    public AutomaticCollectionService automaticCollectionService(AmountFormatWrapper amountFormatWrapper, ConfigurationService configurationService,
                                                                 LedgerService ledgerService, PartyService partyService) {
        return new AutomaticCollectionService(amountFormatWrapper.amountFormat, configurationService, ledgerService, partyService);
    }

    @Bean
    @Scope("prototype")
    public BookkeepingService bookkeepingService(AutomaticCollectionService automaticCollectionService, LedgerService ledgerService,
                                                 ConfigurationService configurationService,
                                                 DocumentService documentService, InvoiceService invoiceService, PartyService partyService) {
        return new BookkeepingService(automaticCollectionService, ledgerService, configurationService, documentService, invoiceService, partyService);
    }

    @Bean
    @Scope("prototype")
    public ConfigurationService configurationService() {
        return new ConfigurationService();
    }

    @Bean
    @Scope("prototype")
    public DocumentService documentService(ConfigurationService configurationService) {
        return new DocumentService(configurationService);
    }

    @Bean
    @Scope("prototype")
    public EmailService emailService(SettingsService settingsService, TextResourceWrapper textResourceWrapper) {
        return new EmailService(settingsService, textResourceWrapper.textResource);
    }

    @Bean
    @Scope("prototype")
    public ImportBankStatementService importBankStatementService(ConfigurationService configurationService) {
        return new ImportBankStatementService(configurationService);
    }

    @Bean
    @Scope("prototype")
    public InvoiceService invoiceService(AmountFormat amountFormat, PartyService partyService,
                                         TextResourceWrapper textResourceWrapper) {
        return new InvoiceService(amountFormat, partyService, textResourceWrapper.textResource);
    }

    @Bean
    @Scope("prototype")
    public LedgerService ledgerService(TextResourceWrapper textResourceWrapper, ConfigurationService configurationService,
                                       InvoiceService invoiceService, PartyService partyService) {
        return new LedgerService(textResourceWrapper.textResource, configurationService, invoiceService, partyService);
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

    @Bean
    @Scope("prototype")
    public InvoicesToModelConverter invoicesToModelConverter(AmountFormatWrapper amountFormatWrapper, ConfigurationService configurationService,
                                                             InvoiceService invoiceService, PartyService partyService,
                                                             TextResourceWrapper textResourceWrapper) {
        return new InvoicesToModelConverter(amountFormatWrapper.amountFormat, configurationService, invoiceService,
                partyService, textResourceWrapper.textResource);
    }

    @Bean
    @Scope("prototype")
    public InvoicePreviewTemplate invoicePreviewTemplate(AmountFormatWrapper amountFormatWrapper, KeyValueReplacer keyValueReplacer,
                                                         TextResourceWrapper textResourceWrapper) {
        return new InvoicePreviewTemplate(amountFormatWrapper.amountFormat, keyValueReplacer, textResourceWrapper.textResource);
    }

    @Bean
    @Scope("prototype")
    public ReportToModelConverter reportToModelConverter(DocumentWrapper documentWrapper, AmountFormatWrapper amountFormatWrapper, ConfigurationService configurationService,
                                                             PartyService partyService, TextResourceWrapper textResourceWrapper) {
        return new ReportToModelConverter(documentWrapper.document, amountFormatWrapper.amountFormat, textResourceWrapper.textResource,
                configurationService, partyService);
    }

    @Bean
    public KeyValueReplacer variableReplacer() {
        return new KeyValueReplacer();
    }

    @Bean
    @Scope("prototype")
    public XMLFileReader xmlFileReader(ConfigurationService configurationService, DocumentService documentService,
                                       ImportBankStatementService importBankStatementService, InvoiceService invoiceService,
                                       LedgerService ledgerService, PartyService partyService) {
        return new XMLFileReader(configurationService, documentService, importBankStatementService, invoiceService,
                ledgerService, partyService);
    }

    @Bean
    public PdfGenerator pdfGenerator() {
        return new PdfGenerator();
    }
}
