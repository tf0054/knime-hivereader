package jp.co.recruit.hadoop.knime.reader;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabaseConnectionSettings;
import org.knime.core.node.port.database.DatabaseDriverLoader;
import org.knime.core.util.KnimeEncryption;

/**
 * Copied from
 * org.knime.base.node.io.database.DBDialogPane
 */

final class DBDialogPane extends JPanel {

    /**
	 * What is this?
	 */
	private static final long serialVersionUID = 1L;

	private static final NodeLogger LOGGER =
        NodeLogger.getLogger(DBDialogPane.class);

    private final JComboBox m_driver = new JComboBox();

    private final JComboBox m_db = new JComboBox();

    private final JTextField m_user = new JTextField("");

    private final JPasswordField m_pass = new JPasswordField();

    private boolean m_passwordChanged = false;

    private final JCheckBox m_credCheckBox = new JCheckBox();
    private final JComboBox m_credBox = new JComboBox();

    /** Default font used for all components within the database dialogs. */
    static final Font FONT = new Font("Monospaced", Font.PLAIN, 12);

    /**
     * Creates new dialog.
     */
    DBDialogPane() {
        super(new GridLayout(0, 1));
        m_driver.setEditable(false);
        m_driver.setFont(FONT);
        m_driver.setPreferredSize(new Dimension(400, 20));
        m_driver.setMaximumSize(new Dimension(400, 20));
        JPanel driverPanel = new JPanel(new BorderLayout());
        driverPanel.setBorder(BorderFactory
                .createTitledBorder(" Database driver "));
        driverPanel.add(m_driver, BorderLayout.CENTER);
        driverPanel.add(new JLabel(" (Additional Database Drivers can be loaded"
                + " in the KNIME preference page.) "), BorderLayout.SOUTH);
        super.add(driverPanel);
        JPanel dbPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        dbPanel.setBorder(BorderFactory.createTitledBorder(
                " Database URL "));
        m_db.setFont(FONT);
        m_db.setPreferredSize(new Dimension(400, 20));
        m_db.setMaximumSize(new Dimension(400, 20));
        m_db.setEditable(true);
        m_driver.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(final ItemEvent ie) {
                String url =
                    DatabaseDriverLoader.getURLForDriver((String) ie.getItem());
                m_db.setSelectedItem(url);
            }
        });
        dbPanel.add(m_db);
        super.add(dbPanel);

        JPanel credPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        credPanel.setBorder(BorderFactory.createTitledBorder(
            " Workflow Credentials "));
        credPanel.add(m_credCheckBox);
        m_credCheckBox.addItemListener(new ItemListener() {
           @Override
           public void itemStateChanged(final ItemEvent ie) {
                enableCredentials(m_credCheckBox.isSelected());
           }
        });
        m_credBox.setEditable(false);
        m_credBox.setFont(FONT);
        m_credBox.setPreferredSize(new Dimension(375, 20));
        credPanel.add(m_credBox);
        super.add(credPanel);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        userPanel.setBorder(BorderFactory.createTitledBorder(" User name "));
        m_user.setPreferredSize(new Dimension(400, 20));
        m_user.setFont(FONT);
        userPanel.add(m_user);
        super.add(userPanel);
        JPanel passPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        passPanel.setBorder(BorderFactory.createTitledBorder(" Password "));
        m_pass.setPreferredSize(new Dimension(400, 20));
        m_pass.setFont(FONT);
        m_pass.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void changedUpdate(final DocumentEvent e) {
                m_passwordChanged = true;
            }
            @Override
            public void insertUpdate(final DocumentEvent e) {
                m_passwordChanged = true;
            }
            @Override
            public void removeUpdate(final DocumentEvent e) {
                m_passwordChanged = true;
            }
        });
        m_pass.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(final FocusEvent fe) {
                if (!m_passwordChanged) {
                    m_pass.setText("");
                }
            }
        });
        passPanel.add(m_pass);
        super.add(passPanel);
    }

    private void enableCredentials(final boolean flag) {
        m_credBox.setEnabled(flag);
        m_pass.setEnabled(!flag);
        m_user.setEnabled(!flag);
    }

    /**
     * Load settings.
     * @param settings to load
     * @param specs input spec
     * @param creds credentials
     */
    protected void loadSettingsFrom(final NodeSettingsRO settings,
            final PortObjectSpec[] specs, final Collection<String> creds) {
        // update credentials
        m_credBox.removeAllItems();
        if (creds.isEmpty()) {
            m_credCheckBox.setEnabled(false);
            m_credBox.setEnabled(false);
        } else {
            m_credCheckBox.setEnabled(true);
            m_credBox.setEnabled(true);
            for (String c : creds) {
                m_credBox.addItem(c);
            }
        }
        // database driver and name
        m_driver.removeAllItems();
        // update list of registered driver
        updateDriver();
        String select = settings.getString("driver",
                m_driver.getSelectedItem().toString());
        m_driver.setSelectedItem(select);
        // update list of urls
        m_db.removeAllItems();
        for (String databaseURL
                : DatabaseConnectionSettings.DATABASE_URLS.getHistory()) {
            m_db.addItem(databaseURL);
        }
        String dbName = settings.getString("database", null);
        if (dbName == null) {
            m_db.setSelectedItem("jdbc:odbc:<database_name>");
        } else {
            m_db.setSelectedItem(dbName);
        }

        boolean useCredential = settings.containsKey("credential_name");
        enableCredentials(useCredential);
        if (useCredential) {
            String credName = settings.getString("credential_name", null);
            m_credBox.setSelectedItem(credName);
            m_credCheckBox.setSelected(true);
        } else {
            // user
            String user = settings.getString("user", null);
            m_user.setText(user == null ? "" : user);
            // password
            String password = settings.getString("password", null);
            m_pass.setText(password == null ? "" : password);
            m_passwordChanged = false;
            m_credCheckBox.setSelected(false);
        }
    }

    private void updateDriver() {
        m_driver.removeAllItems();
        Set<String> driverNames = new HashSet<String>(
                DatabaseDriverLoader.getLoadedDriver());
        for (String driverName
                : DatabaseConnectionSettings.DRIVER_ORDER.getHistory()) {
            if (driverNames.contains(driverName)) {
                m_driver.addItem(driverName);
                driverNames.remove(driverName);
            }
        }
        for (String driverName : driverNames) {
            m_driver.addItem(driverName);
        }
    }

    /**
     * Save settings.
     * @param settings to save into
     */
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        String driverName = m_driver.getSelectedItem().toString();
        settings.addString("driver", driverName);
        String url = m_db.getEditor().getItem().toString();
        settings.addString("database", url);
        boolean useCredential = m_credCheckBox.isSelected();
        if (useCredential) {
            settings.addString("credential_name",
                (String) m_credBox.getSelectedItem());
        } else {
            settings.addString("user", m_user.getText().trim());
            if (m_passwordChanged) {
                try {
                    settings.addString("password", KnimeEncryption.encrypt(
                            m_pass.getPassword()));
                } catch (Throwable t) {
                    LOGGER.error("Could not encrypt password, reason: "
                            + t.getMessage(), t);
                }
            } else {
                settings.addString("password",
                    new String(m_pass.getPassword()));
            }
        }
        // fix 2416: for backward compatible reason the loaded_driver still
        // needs to be added to the NodeSettings; dialog and model setting
        // must hold the same properties (see DatabaseConnectionSettings)
        final File driverFile =
            DatabaseDriverLoader.getDriverFileForDriverClass(driverName);
        settings.addString("loaded_driver",
                (driverFile == null ? null : driverFile.getAbsolutePath()));
    }
}

