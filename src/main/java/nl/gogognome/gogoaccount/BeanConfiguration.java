package nl.gogognome.gogoaccount;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.component.invoice.InvoiceService;
import nl.gogognome.gogoaccount.gui.MainFrame;
import nl.gogognome.gogoaccount.gui.invoice.InvoiceOverviewTableModel;
import nl.gogognome.gogoaccount.gui.invoice.InvoicesView;
import nl.gogognome.lib.text.AmountFormat;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class BeanConfiguration {

    @Bean
    public MainFrame mainFrame(ConfigurableListableBeanFactory beanFactory, ResourceLoader resourceLoader) {
        return new MainFrame(beanFactory, resourceLoader);
    }

    @Bean
    public InvoicesView invoicesView(Document document, InvoiceOverviewTableModel invoiceOverviewTableModel, InvoiceService invoiceService) {
        return new InvoicesView(document, invoiceOverviewTableModel, invoiceService);
    }

    @Bean
    public InvoiceOverviewTableModel invoiceOverviewTableModel(AmountFormat amountFormat) {
        return new InvoiceOverviewTableModel(amountFormat);
    }

    @Bean
    public InvoiceService invoiceService() {
        return new InvoiceService();
    }
}
