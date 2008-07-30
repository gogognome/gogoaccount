/*
 * $Id: InvoiceToOdtView.java,v 1.1 2008-07-30 20:42:11 sanderk Exp $
 *
 * Copyright (C) 2006 Sander Kooijmans
 */
package cf.ui.views;

import cf.engine.Database;
import cf.engine.odt.InvoiceOdtFileGenerator;
import java.awt.Frame;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.Date;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import nl.gogognome.beans.DateSelectionBean;
import nl.gogognome.framework.View;
import nl.gogognome.framework.ViewDialog;
import nl.gogognome.framework.models.DateModel;
import nl.gogognome.swing.ButtonPanel;
import nl.gogognome.swing.MessageDialog;
import nl.gogognome.swing.SwingUtils;
import nl.gogognome.swing.WidgetFactory;
import nl.gogognome.text.TextResource;
import nl.gogognome.util.DateUtil;

/**
 * This view allows the user to generate invoices (an ODT file) for debtors.
 *
 * @author Sander Kooijmans
 */
public class InvoiceToOdtView extends View {

    /** The database used to determine the invoices. */
    private Database database;

    private JTextField tfTemplateFileName;

    private JTextField tfPdfFileName;

    private DateModel dateModel;

    private JTextField tfConcerning;

    private JTextField tfOurReference;

    private DateModel dueDateModel;

    private Frame parentFrame;

    /**
     * Constructor.
     * @param database the database used by this view
     */
    public InvoiceToOdtView(Database database) {
        super();
        this.database = database;
    }

    protected void handleOk() {
        Date date = dateModel.getDate();
        if (date == null) {
            MessageDialog.showMessage(parentFrame, "gen.error",
                    TextResource.getInstance().getString("gen.invalidDate"));
            return;
        }

        Date dueDate = dueDateModel.getDate();
        if (dueDate == null) {
            MessageDialog.showMessage(parentFrame, "gen.error",
                    TextResource.getInstance().getString("gen.invalidDate"));
            return;
        }

        // Let the user select the invoices that should be added to the PDF file.
        InvoiceEditAndSelectionView invoicesView = new InvoiceEditAndSelectionView(database, true, true);
        ViewDialog dialog = new ViewDialog(parentFrame, invoicesView);
        dialog.showDialog();
        if (invoicesView.getSelectedInvoices() != null) {
            InvoiceOdtFileGenerator converter = new InvoiceOdtFileGenerator();
            try {
                converter.generateInvoices(tfTemplateFileName.getText(),
                    tfPdfFileName.getText(), invoicesView.getSelectedInvoices(), date, tfConcerning.getText(),
                    tfOurReference.getText(), dueDate);
            } catch (IOException e) {
                MessageDialog.showMessage(getParentWindow(), "gen.error", e.getMessage());
                return;
            }
        }

        closeAction.actionPerformed(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTitle() {
        return TextResource.getInstance().getString("id.title");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInit() {
        WidgetFactory wf = WidgetFactory.getInstance();
        JPanel panel = new JPanel(new GridBagLayout());
        panel.add(wf.createLabel("id.templateFilename"),
                SwingUtils.createLabelGBConstraints(0, 0));

        tfTemplateFileName = wf.createTextField(30);
        panel.add(tfTemplateFileName,
                SwingUtils.createTextFieldGBConstraints(1, 0));

        JButton button = wf.createButton("gen.btSelectFile", new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser(tfTemplateFileName.getText());
                if (fileChooser.showOpenDialog(getParentWindow()) == JFileChooser.APPROVE_OPTION) {
                    tfTemplateFileName.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }

            }
        });
        panel.add(button,
                SwingUtils.createLabelGBConstraints(2, 0));

        panel.add(wf.createLabel("id.pdfFileName"),
                SwingUtils.createLabelGBConstraints(0, 1));
        tfPdfFileName = wf.createTextField(30);
        panel.add(tfPdfFileName,
                SwingUtils.createTextFieldGBConstraints(1, 1));

        button = wf.createButton("gen.btSelectFile", new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                JFileChooser fileChooser = new JFileChooser(tfPdfFileName.getText());
                if (fileChooser.showOpenDialog(getParentWindow()) == JFileChooser.APPROVE_OPTION) {
                    tfPdfFileName.setText(fileChooser.getSelectedFile().getAbsolutePath());
                }

            }
        });
        panel.add(button,
                SwingUtils.createLabelGBConstraints(2, 1));

        panel.add(wf.createLabel("id.date"),
                SwingUtils.createLabelGBConstraints(0, 2));
        dateModel = new DateModel();
        dateModel.setDate(new Date(), null);
        panel.add(new DateSelectionBean(dateModel),
                SwingUtils.createLabelGBConstraints(1, 2));

        panel.add(wf.createLabel("id.concerning"),
                SwingUtils.createLabelGBConstraints(0, 3));
        tfConcerning = wf.createTextField(30);
        panel.add(tfConcerning,
                SwingUtils.createLabelGBConstraints(1, 3));

        panel.add(wf.createLabel("id.ourReference"),
                SwingUtils.createLabelGBConstraints(0, 4));
        tfOurReference = wf.createTextField(30);
        panel.add(tfOurReference,
                SwingUtils.createLabelGBConstraints(1, 4));

        panel.add(wf.createLabel("id.dueDate"),
                SwingUtils.createLabelGBConstraints(0, 5));
        dueDateModel = new DateModel();
        dueDateModel.setDate(DateUtil.addDays(new Date(), 14), null);
        panel.add(new DateSelectionBean(dueDateModel),
                SwingUtils.createLabelGBConstraints(1, 5));

        // Create button panel
        ButtonPanel buttonPanel = new ButtonPanel(SwingConstants.CENTER);
        buttonPanel.add(WidgetFactory.getInstance().createButton("gen.ok", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                handleOk();
            }
        }));
        buttonPanel.add(WidgetFactory.getInstance().createButton("gen.cancel", closeAction));
        panel.add(buttonPanel, SwingUtils.createPanelGBConstraints(0, 6));

        add(panel);
    }
}
