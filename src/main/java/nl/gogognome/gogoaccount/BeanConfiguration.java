package nl.gogognome.gogoaccount;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.document.DocumentService;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.component.invoice.amountformula.AmountFormula;
import nl.gogognome.gogoaccount.component.invoice.amountformula.AmountFormulaParser;
import nl.gogognome.gogoaccount.component.ledger.LedgerService;
import nl.gogognome.gogoaccount.component.party.PartyService;
import nl.gogognome.gogoaccount.gui.DocumentRegistry;
import nl.gogognome.gogoaccount.gui.MainFrame;
import nl.gogognome.gogoaccount.gui.TextResourceRegistry;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.gui.invoice.InvoiceGeneratorView;
import nl.gogognome.gogoaccount.gui.invoice.InvoicesView;
import nl.gogognome.gogoaccount.gui.views.AccountMutationsView;
import nl.gogognome.gogoaccount.gui.views.ImportBankStatementView;
import nl.gogognome.gogoaccount.services.BookkeepingService;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;

import java.lang.reflect.Constructor;
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
                } catch (BeansException e) {
                }
                try {
                    Constructor<? extends View> c = viewClass.getConstructor(Document.class);
                    return c.newInstance(beanFactory.getBean("document"));
                } catch (Exception e) {
                    throw new RuntimeException("Could not create instance of view " + viewClass.getName(), e);
                }
            }
        };
    }

    @Bean
    public DocumentRegistry documentRegistry(ConfigurableListableBeanFactory beanFactory, ConfigurationService configurationService) {
        return new DocumentRegistry() {
            @Override
            public void register(Document document) {
                if (document != null) {
                    try {
                        Currency currency = configurationService.getBookkeeping(document).getCurrency();

                        beanFactory.registerSingleton("document", document);
                        beanFactory.registerSingleton("amountFormat", new AmountFormat(document.getLocale(), currency));
                    } catch (ServiceException e) {
                        throw new RuntimeException("Could not get currency", e);
                    }
                } else {
                    beanFactory.destroyBean("document");
                    beanFactory.destroyBean("amountFormat");
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
    public AccountMutationsView accountMutationsView(Document document, BookkeepingService bookkeepingService) {
        return new AccountMutationsView(document, bookkeepingService);
    }

    @Bean
    @Scope("prototype")
    public InvoicesView invoicesView(Document document, AmountFormat amountFormat, InvoiceService invoiceService) {
        return new InvoicesView(document, amountFormat, invoiceService);
    }

    @Bean
    @Scope("prototype")
    public ImportBankStatementView importBankStatementView(Document document, AmountFormat amountFormat) {
        return new ImportBankStatementView(document, amountFormat);
    }

    @Bean
    @Scope("prototype")
    public InvoiceGeneratorView invoiceGeneratorView(Document document, AmountFormulaParser amountFormulaParser, LedgerService ledgerService) {
        return new InvoiceGeneratorView(ledgerService, document, amountFormulaParser);
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
}
