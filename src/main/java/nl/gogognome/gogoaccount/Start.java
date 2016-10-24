package nl.gogognome.gogoaccount;

import nl.gogognome.dataaccess.transaction.CurrentTransaction;
import nl.gogognome.gogoaccount.component.document.DocumentAwareTransaction;
import nl.gogognome.gogoaccount.gui.MainFrame;
import nl.gogognome.gogoaccount.gui.TextResourceRegistry;
import nl.gogognome.lib.gui.beans.BeanFactory;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.plaf.DefaultLookAndFeel;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.CommandLinePropertySource;
import org.springframework.core.env.SimpleCommandLinePropertySource;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Currency;
import java.util.Locale;

/**
 * Starts gogo account with the graphical user interface.
 */
@SpringBootApplication
public class Start {

    private final static Logger logger = LoggerFactory.getLogger(Start.class);
    private String fileName;
    private final Object lock = new Object();

    /**
     * Starts the application.
     * @param args command line arguments; if one argument is passed, then
     *        it is used as file name of an edition that is loaded.
     *        Further, if the argument <tt>-lang=X</tt> is used, then
     *        the language is set to </tt>X</tt>. </tt>X</tt> should be a valid
     *        ISO 639 language code.
     */
    public static void main(String[] args) {
        Start start = new Start();
        start.startApplication(args);
    }

    private void startApplication(String[] args) {
        CurrentTransaction.transactionCreator = DocumentAwareTransaction::new;
        initFactory(Locale.getDefault());
        CommandLinePropertySource clps = new SimpleCommandLinePropertySource(args);
        parseArguments(clps);
        DefaultLookAndFeel.useDefaultLookAndFeel();
        logger.debug("Locale: " + Locale.getDefault());

        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(Start.class).headless(false).web(false).run(args);
        ctx.getEnvironment().getPropertySources().addFirst(clps);
        ctx.getBean(TextResourceRegistry.class).register(Factory.getInstance(TextResource.class));
        MainFrame mainFrame = initFrame(ctx);

        if (fileName != null) {
            File file = new File(fileName);
            if (!file.exists()) {
                file = new File(fileName + ".h2.db");
            }
            if (file.exists()) {
                mainFrame.loadFile(file);
            } else {
                MessageDialog.showErrorMessage(mainFrame, "mf.fileDoesNotExist", fileName);
            }
        } else {
            mainFrame.handleNewEdition();
        }

        waitForFrameToClose();
    }

    private void waitForFrameToClose() {
        try {
            synchronized (lock) {
                lock.wait();
            }
        } catch (InterruptedException e) {
            logger.debug("Wait was interrupted", e);
        }
    }

    public void initFactory(Locale locale) {
        Locale.setDefault(locale);
        TextResource tr = new TextResource(locale);
        tr.loadResourceBundle("stringresources");

        Factory.bindSingleton(TextResource.class, tr);
        Factory.bindSingleton(WidgetFactory.class, new WidgetFactory(tr));
        Factory.bindSingleton(BeanFactory.class, new BeanFactory(tr));
        Factory.bindSingleton(Locale.class, locale);
        Factory.bindSingleton(AmountFormat.class, new AmountFormat(locale, Currency.getInstance("EUR")));
    }

    private void parseArguments(CommandLinePropertySource commandLinePropertySource) {
        if (commandLinePropertySource.containsProperty("lang")) {
            Locale locale = new Locale(commandLinePropertySource.getProperty("lang"));
            initFactory(locale);
        }

        if (commandLinePropertySource.containsProperty("nonOptionArgs")) {
            fileName = commandLinePropertySource.getProperty("nonOptionArgs");
            int index = fileName.indexOf(',');
            if (index != -1) {
                fileName = fileName.substring(0, index);
            }
        }
    }

    private MainFrame initFrame(ConfigurableApplicationContext ctx) {
        MainFrame mainFrame = ctx.getBean(MainFrame.class);
        mainFrame.setVisible(true);
        SwingUtils.center(mainFrame);
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        mainFrame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                onFrameClosed();
            }
        });
        return mainFrame;
    }

    private void onFrameClosed() {
        synchronized (lock) {
            lock.notifyAll();
        }
    }

}
