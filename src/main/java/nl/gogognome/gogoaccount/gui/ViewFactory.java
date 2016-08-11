package nl.gogognome.gogoaccount.gui;

import nl.gogognome.lib.swing.views.View;

public interface ViewFactory {

    View createView(Class<? extends View> viewClass);

}
