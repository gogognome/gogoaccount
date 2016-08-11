package nl.gogognome.gogoaccount;

import nl.gogognome.gogoaccount.component.configuration.ConfigurationService;
import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.gui.DocumentRegistry;
import nl.gogognome.gogoaccount.gui.MainFrame;
import nl.gogognome.gogoaccount.gui.invoice.InvoiceOverviewTableModel;
import nl.gogognome.gogoaccount.gui.invoice.InvoicesView;
import nl.gogognome.gogoaccount.gui.ViewFactory;
import nl.gogognome.gogoaccount.services.ServiceException;
import nl.gogognome.lib.swing.views.View;
import nl.gogognome.lib.text.AmountFormat;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;

import java.lang.reflect.Constructor;
import java.util.Currency;

@Configuration
public class BeanConfiguration {

    @Bean
    public MainFrame mainFrame(ViewFactory viewFactory, DocumentRegistry documentRegistry, ResourceLoader resourceLoader) {
        return new MainFrame(viewFactory, documentRegistry, resourceLoader);
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

    // use this: http://docs.spring.io/spring/docs/current/spring-framework-reference/html/beans.html  see section Lookup method injection
    @Bean
    @Scope("prototype")
    public InvoicesView invoicesView(Document document, InvoiceOverviewTableModel invoiceOverviewTableModel, InvoiceService invoiceService) {
        return new InvoicesView(document, invoiceOverviewTableModel, invoiceService);
    }

    @Bean
    @Scope("prototype")
    public InvoiceOverviewTableModel invoiceOverviewTableModel(AmountFormat amountFormat) {
        return new InvoiceOverviewTableModel(amountFormat);
    }

    @Bean
    @Scope("prototype")
    public InvoiceService invoiceService() {
        return new InvoiceService();
    }

    @Bean
    @Scope("prototype")
    public ConfigurationService configurationService() {
        return new ConfigurationService();
    }
}
