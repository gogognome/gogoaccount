package nl.gogognome.gogoaccount;

import nl.gogognome.gogoaccount.component.document.Document;
import nl.gogognome.gogoaccount.gui.MainFrame;
import nl.gogognome.gogoaccount.gui.invoice.InvoicesView;
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
    public InvoicesView invoicesView(Document document) {
        return new InvoicesView(document);
    }
}
