package nl.gogognome.gogoaccount.gui;

import nl.gogognome.lib.gui.beans.BeanFactory;
import nl.gogognome.lib.swing.MessageDialog;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.plaf.DefaultLookAndFeel;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

import javax.swing.*;
import java.io.File;
import java.util.Currency;
import java.util.Locale;

/**
 * Starts gogo account with the graphical user interface.
 */
public class Start {

    private String fileName;

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
        initFactory(Locale.getDefault());
        parseArguments(args);
        DefaultLookAndFeel.useDefaultLookAndFeel();
        MainFrame mainFrame = initFrame();

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
    }

    public void initFactory(Locale locale) {
        Locale.setDefault(locale);
        TextResource tr = new TextResource(locale);
        tr.loadResourceBundle("stringresources");

        Factory.bindSingleton(TextResource.class, tr);
        Factory.bindSingleton(WidgetFactory.class, new WidgetFactory(tr));
        Factory.bindSingleton(BeanFactory.class, new BeanFactory());
        Factory.bindSingleton(Locale.class, locale);
        Factory.bindSingleton(AmountFormat.class, new AmountFormat(locale, Currency.getInstance("EUR")));
    }

    /**
     * Parses arguments: language must be set before creating main frame
     * @param args command line arguments
     */
    private void parseArguments(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("-lang=")) {
                Locale locale = new Locale(arg.substring(6));
                initFactory(locale);
            } else {
                fileName = arg;
            }
        }
    }

    private MainFrame initFrame() {
        MainFrame mainFrame = new MainFrame();
        mainFrame.setVisible(true);
        SwingUtils.center(mainFrame);
        mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        return mainFrame;
    }

}
