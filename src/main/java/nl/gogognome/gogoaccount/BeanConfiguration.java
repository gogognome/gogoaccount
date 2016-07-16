package nl.gogognome.gogoaccount;

import nl.gogognome.gogoaccount.gui.MainFrame;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

@Configuration
public class BeanConfiguration {

    @Bean
    public MainFrame mainFrame(BeanFactory beanFactory, ResourceLoader resourceLoader) {
        return new MainFrame(beanFactory, resourceLoader);
    }
}
