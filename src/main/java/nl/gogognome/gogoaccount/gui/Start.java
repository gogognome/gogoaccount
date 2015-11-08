package nl.gogognome.gogoaccount.gui;

import java.util.Locale;

import javax.swing.JFrame;

import nl.gogognome.lib.gui.beans.BeanFactory;
import nl.gogognome.lib.swing.SwingUtils;
import nl.gogognome.lib.swing.WidgetFactory;
import nl.gogognome.lib.swing.plaf.DefaultLookAndFeel;
import nl.gogognome.lib.text.AmountFormat;
import nl.gogognome.lib.text.TextResource;
import nl.gogognome.lib.util.Factory;

/**
 * Starts gogo account with the graphical user interface.
 */
public class Start {

	private String fileName;
	private MainFrame mainFrame;

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
		initFrame();

		if (fileName != null) {
		    mainFrame.loadFile(fileName);
		}
	}

	public void initFactory(Locale locale) {
		TextResource tr = new TextResource(locale);
		tr.loadResourceBundle("stringresources");

		Factory.bindSingleton(TextResource.class, tr);
		Factory.bindSingleton(WidgetFactory.class, new WidgetFactory(tr));
		Factory.bindSingleton(AmountFormat.class, new AmountFormat(tr.getLocale()));
		Factory.bindSingleton(BeanFactory.class, new BeanFactory());
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

	private void initFrame() {
		mainFrame = new MainFrame();
        mainFrame.setVisible(true);
        SwingUtils.center(mainFrame);
		mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
	}

}
