package edu.umd.cs.findbugs.gui2;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.ProgressMonitor;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import edu.umd.cs.findbugs.AWTEventQueueExecutor;
import edu.umd.cs.findbugs.IGuiCallback;
import edu.umd.cs.findbugs.util.LaunchBrowser;

public abstract class AbstractSwingGuiCallback implements IGuiCallback {
    private AWTEventQueueExecutor bugUpdateExecutor = new AWTEventQueueExecutor();

    private final Component parent;

    public AbstractSwingGuiCallback(Component parent) {
        this.parent = parent;
    }

    public ExecutorService getBugUpdateExecutor() {
        return bugUpdateExecutor;
    }

    public void showMessageDialogAndWait(final String message) throws InterruptedException {
        if (SwingUtilities.isEventDispatchThread())
            JOptionPane.showMessageDialog(parent, message);
        else
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(parent, message);
                    }
                });
            } catch (InvocationTargetException e) {
                throw new IllegalStateException(e);
            }
    }

    public void showMessageDialog(final String message) {
        if (SwingUtilities.isEventDispatchThread())
            JOptionPane.showMessageDialog(parent, message);
        else
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    JOptionPane.showMessageDialog(parent, message);
                }
            });
    }

    public int showConfirmDialog(String message, String title, String ok, String cancel) {
        return JOptionPane.showOptionDialog(parent, message, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE,
                null, new Object[] { ok, cancel }, ok);
    }

    public InputStream getProgressMonitorInputStream(InputStream in, int length, String msg) {
        ProgressMonitorInputStream pmin = new ProgressMonitorInputStream(parent, msg, in);
        ProgressMonitor pm = pmin.getProgressMonitor();

        if (length > 0)
            pm.setMaximum(length);
        return pmin;
    }

    public void displayNonmodelMessage(String title, String message) {
        DisplayNonmodelMessage.displayNonmodelMessage(title, message, parent, true);
    }

    public String showQuestionDialog(String message, String title, String defaultValue) {
        return (String) JOptionPane.showInputDialog(parent, message, title, JOptionPane.QUESTION_MESSAGE, null, null,
                defaultValue);
    }

    public List<String> showForm(String message, String title, List<FormItem> items) {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.gridwidth = 2;
        gbc.gridy = 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        panel.add(new JLabel(message), gbc);
        gbc.gridwidth = 1;
        for (FormItem item : items) {
            gbc.gridy++;
            panel.add(new JLabel(item.getLabel()), gbc);
            String defaultValue = item.getDefaultValue();
            if (item.getPossibleValues() != null) {
                DefaultComboBoxModel model = new DefaultComboBoxModel();
                JComboBox box = new JComboBox(model);
                item.setField(box);
                for (String possibleValue : item.getPossibleValues()) {
                    model.addElement(possibleValue);
                }
                if (defaultValue == null)
                    model.setSelectedItem(model.getElementAt(0));
                else
                    model.setSelectedItem(defaultValue);
                panel.add(box, gbc);

            } else {
                JTextField field = (item.isPassword() ? new JPasswordField() : new JTextField());
                if (defaultValue != null) {
                    field.setText(defaultValue);
                }
                item.setField(field);
                panel.add(field, gbc);
            }
        }

        int result = JOptionPane.showConfirmDialog(parent, panel, title, JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION)
            return null;
        List<String> results = new ArrayList<String>();
        for (FormItem item : items) {
            JComponent field = item.getField();
            if (field instanceof JTextComponent) {
                JTextComponent textComponent = (JTextComponent) field;
                results.add(textComponent.getText());
            } else if (field instanceof JComboBox) {
                JComboBox box = (JComboBox) field;
                results.add((String) box.getSelectedItem());
            }
        }
        return results;
    }

    public boolean showDocument(URL u) {
        return LaunchBrowser.showDocument(u);
    }

    public boolean isHeadless() {
        return false;
    }

    public void invokeInGUIThread(Runnable r) {
        SwingUtilities.invokeLater(r);
    }
}