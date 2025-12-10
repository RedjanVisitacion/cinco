import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class smartborrow {

    // =========================q
    // Resource & Borrower Manager
    // =========================
    static class ResourceManager {
        List<String> gadgets = new ArrayList<>();
        List<String> tools = new ArrayList<>();
        List<String> rooms = new ArrayList<>();

        public ResourceManager() {
            loadResourcesFromDB();
        }

        public void loadResourcesFromDB() {
            gadgets.clear();
            tools.clear();
            rooms.clear();

            try (Connection conn = DBConnection.connect();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT type, name FROM resources")) {

                while (rs.next()) {
                    String type = rs.getString("type");
                    String name = rs.getString("name");
                    switch (type) {
                        case "Gadget" -> gadgets.add(name);
                        case "Tool" -> tools.add(name);
                        case "Room" -> rooms.add(name);
                    }
                }

            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void removeResource(String type, String name) {
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM resources WHERE type=? AND name=?")) {
                ps.setString(1, type);
                ps.setString(2, name);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        public void addResource(String type, String name) {
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement("INSERT INTO resources(type,name) VALUES(?,?)")) {
                ps.setString(1, type);
                ps.setString(2, name);
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static class BorrowerManager {
        Map<String, List<String>> borrowedGadgets = new HashMap<>();
        Map<String, List<String>> borrowedTools = new HashMap<>();
        Map<String, List<String>> borrowedRooms = new HashMap<>();
        Map<String, String> borrowerNames = new HashMap<>();

        public void addBorrowing(String borrowerId, String borrowerName, String type, String item) {
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement(
                         "INSERT INTO borrowings(borrower_id, borrower_name, type, item, borrow_time) VALUES(?,?,?,?,?)")) {
                ps.setString(1, borrowerId);
                ps.setString(2, borrowerName);
                ps.setString(3, type);
                ps.setString(4, item);
                ps.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                ps.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    static ResourceManager resources = new ResourceManager();
    static BorrowerManager borrowers = new BorrowerManager();

    // =========================
    // MAIN
    // =========================
    public static void main(String[] args) {
        try {
            DBConnection.ensureSchema();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(LoginFrame::new);
    }

    // =========================
    // LOGIN FRAME
    // =========================
    static class LoginFrame extends JFrame implements ActionListener {
        private final JTextField usernameField;
        private final JPasswordField passwordField;

        public LoginFrame() {
            setTitle("Smart Campus Borrowing System - Login");
            setSize(350, 200);
            setLayout(new GridBagLayout());
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);

            gbc.gridx = 0; gbc.gridy = 0; gbc.anchor = GridBagConstraints.EAST;
            add(new JLabel("Username:"), gbc);
            gbc.gridx = 1; gbc.gridy = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
            usernameField = new JTextField(15);
            add(usernameField, gbc);

            gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
            add(new JLabel("Password:"), gbc);
            gbc.gridx = 1; gbc.gridy = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
            passwordField = new JPasswordField(15);
            add(passwordField, gbc);

            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.CENTER;
            JButton loginBtn = new JButton("Login");
            loginBtn.addActionListener(this);
            add(loginBtn, gbc);

            setVisible(true);
        }

        public void actionPerformed(ActionEvent e) {
            String user = usernameField.getText().trim();
            String pass = new String(passwordField.getPassword()).trim();
            if (authenticate(user, pass)) {
                JOptionPane.showMessageDialog(this, "Login successful!");
                dispose();
                new MainDashboard();
            } else {
                JOptionPane.showMessageDialog(this, "Invalid username or password!");
            }
        }

        private boolean authenticate(String username, String password) {
            if (username.isEmpty() || password.isEmpty()) return false;
            try (Connection conn = DBConnection.connect();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT id FROM users WHERE username=? AND password=?")) {
                ps.setString(1, username);
                ps.setString(2, password);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Login error: " + ex.getMessage());
                return false;
            }
        }
    }

    // =========================
    // MAIN DASHBOARD
    // =========================
    static class MainDashboard extends JFrame {
        private final JTextArea displayArea;

        public MainDashboard() {
            setTitle("Smart Campus Borrowing System ");
            setSize(900, 600);
            setLayout(new BorderLayout());
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setLocationRelativeTo(null);

            // Menu
            JMenuBar menuBar = new JMenuBar();
            JMenu fileMenu = new JMenu("File");
            JMenuItem exitItem = new JMenuItem("Exit");
            exitItem.addActionListener(e -> System.exit(0));
            fileMenu.add(exitItem);
            menuBar.add(fileMenu);
            setJMenuBar(menuBar);

            displayArea = new JTextArea(20, 60);
            displayArea.setEditable(false);
            displayArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            add(new JScrollPane(displayArea), BorderLayout.CENTER);

            JPanel controlPanel = new JPanel(new GridLayout(1, 3, 10, 10));
            controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            controlPanel.add(createResourcePanel());
            controlPanel.add(createBorrowPanel());
            controlPanel.add(createViewPanel());
            add(controlPanel, BorderLayout.SOUTH);

            refreshDisplay();
            setVisible(true);
        }

        private JPanel createResourcePanel() {
            JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
            panel.setBorder(BorderFactory.createTitledBorder("Resource Management"));

            JButton addBtn = new JButton("Add Resource");
            JButton removeBtn = new JButton("Remove Resource");
            JButton viewAllBtn = new JButton("View All Resources");
            JButton clearBtn = new JButton("Clear All");

            addBtn.addActionListener(e -> addResource());
            removeBtn.addActionListener(e -> removeResource());
            viewAllBtn.addActionListener(e -> refreshDisplay());
            clearBtn.addActionListener(e -> clearAllResources());

            panel.add(addBtn);
            panel.add(removeBtn);
            panel.add(viewAllBtn);
            panel.add(clearBtn);

            return panel;
        }

        private JPanel createBorrowPanel() {
            JPanel panel = new JPanel(new GridLayout(4, 1, 5, 5));
            panel.setBorder(BorderFactory.createTitledBorder("Borrowing Operations"));

            JButton borrowBtn = new JButton("Borrow Resource");
            JButton returnBtn = new JButton("Return Resource");
            JButton viewBorrowersBtn = new JButton("View Borrowers");
            JButton historyBtn = new JButton("View History");

            borrowBtn.addActionListener(e -> borrowResource());
            returnBtn.addActionListener(e -> returnResource());
            viewBorrowersBtn.addActionListener(e -> viewBorrowers());
            historyBtn.addActionListener(e -> viewHistory());

            panel.add(borrowBtn);
            panel.add(returnBtn);
            panel.add(viewBorrowersBtn);
            panel.add(historyBtn);

            return panel;
        }

        private JPanel createViewPanel() {
            JPanel panel = new JPanel(new GridLayout(3, 1, 5, 5));
            panel.setBorder(BorderFactory.createTitledBorder("Quick Views"));

            JButton gadgetsBtn = new JButton("View Gadgets");
            JButton toolsBtn = new JButton("View Tools");
            JButton roomsBtn = new JButton("View Rooms");

            gadgetsBtn.addActionListener(e -> viewByType("Gadgets"));
            toolsBtn.addActionListener(e -> viewByType("Tools"));
            roomsBtn.addActionListener(e -> viewByType("Rooms"));

            panel.add(gadgetsBtn);
            panel.add(toolsBtn);
            panel.add(roomsBtn);

            return panel;
        }

        private void refreshDisplay() {
            resources.loadResourcesFromDB(); // refresh resources from DB
            StringBuilder sb = new StringBuilder();
            sb.append("=".repeat(70)).append("\n");
            sb.append("        SMART CAMPUS BORROWING SYSTEM - DASHBOARD\n");
            sb.append("=".repeat(70)).append("\n\n");

            sb.append("AVAILABLE RESOURCES:\n");
            sb.append("-".repeat(40)).append("\n");
            sb.append("Gadgets: ").append(resources.gadgets).append("\n");
            sb.append("Tools:   ").append(resources.tools).append("\n");
            sb.append("Rooms:   ").append(resources.rooms).append("\n\n");

            displayArea.setText(sb.toString());
        }

        // =========================
        // ADD / REMOVE / CLEAR
        // =========================
        private void addResource() {
            JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Gadget", "Tool", "Room"});
            JTextField nameField = new JTextField(20);

            panel.add(new JLabel("Type:")); panel.add(typeCombo);
            panel.add(new JLabel("Name:")); panel.add(nameField);

            int result = JOptionPane.showConfirmDialog(this, panel, "Add Resource", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String type = (String) typeCombo.getSelectedItem();
                String name = nameField.getText().trim();
                if (!name.isEmpty()) {
                    resources.addResource(type, name);
                    JOptionPane.showMessageDialog(this, "Resource added: " + type + " - " + name);
                    refreshDisplay();
                }
            }
        }

        private void removeResource() {
            JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Gadget", "Tool", "Room"});
            JTextField nameField = new JTextField(20);

            panel.add(new JLabel("Type:")); panel.add(typeCombo);
            panel.add(new JLabel("Name:")); panel.add(nameField);

            int result = JOptionPane.showConfirmDialog(this, panel, "Remove Resource", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result == JOptionPane.OK_OPTION) {
                String type = (String) typeCombo.getSelectedItem();
                String name = nameField.getText().trim();
                resources.removeResource(type, name);
                JOptionPane.showMessageDialog(this, "Resource removed: " + type + " - " + name);
                refreshDisplay();
            }
        }

        private void clearAllResources() {
            int confirm = JOptionPane.showConfirmDialog(this, "Clear ALL resources? This cannot be undone.", "Confirm", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                for (String g : new ArrayList<>(resources.gadgets)) resources.removeResource("Gadget", g);
                for (String t : new ArrayList<>(resources.tools)) resources.removeResource("Tool", t);
                for (String r : new ArrayList<>(resources.rooms)) resources.removeResource("Room", r);
                JOptionPane.showMessageDialog(this, "All resources cleared!");
                refreshDisplay();
            }
        }

        // =========================
        // BORROW RESOURCE
        // =========================
        private void borrowResource() {
            JPanel panel = new JPanel(new GridLayout(5, 2, 5, 5));

            JTextField borrowerIdField = new JTextField(10);
            JTextField borrowerNameField = new JTextField(20);
            JComboBox<String> typeCombo = new JComboBox<>(new String[]{"Gadget", "Tool", "Room"});
            JComboBox<String> itemCombo = new JComboBox<>();

            panel.add(new JLabel("Borrower ID:")); panel.add(borrowerIdField);
            panel.add(new JLabel("Borrower Name:")); panel.add(borrowerNameField);
            panel.add(new JLabel("Resource Type:")); panel.add(typeCombo);
            panel.add(new JLabel("Resource Name:")); panel.add(itemCombo);

            // Dynamic update of items
            typeCombo.addActionListener(e -> {
                itemCombo.removeAllItems();
                String selectedType = (String) typeCombo.getSelectedItem();
                List<String> items = switch (selectedType) {
                    case "Gadget" -> resources.gadgets;
                    case "Tool" -> resources.tools;
                    case "Room" -> resources.rooms;
                    default -> new ArrayList<>();
                };
                for (String item : items) itemCombo.addItem(item);
            });
            typeCombo.setSelectedIndex(0); // trigger initial load

            int result = JOptionPane.showConfirmDialog(this, panel, "Borrow Resource", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result == JOptionPane.OK_OPTION) {
                String borrowerId = borrowerIdField.getText().trim();
                String borrowerName = borrowerNameField.getText().trim();
                String type = (String) typeCombo.getSelectedItem();
                String itemName = (String) itemCombo.getSelectedItem();

                if (borrowerId.isEmpty() || borrowerName.isEmpty() || itemName == null) {
                    JOptionPane.showMessageDialog(this, "Please fill all fields!");
                    return;
                }

                // Remove from resources
                switch (type) {
                    case "Gadget" -> resources.gadgets.remove(itemName);
                    case "Tool" -> resources.tools.remove(itemName);
                    case "Room" -> resources.rooms.remove(itemName);
                }
                resources.removeResource(type, itemName);

                // Add to borrowers
                borrowers.borrowerNames.put(borrowerId, borrowerName);
                borrowers.addBorrowing(borrowerId, borrowerName, type, itemName);

                JOptionPane.showMessageDialog(this, "Borrowed: " + type + " - " + itemName);
                refreshDisplay();
            }
        }

        // =========================
        // RETURN RESOURCE
        // =========================
        private void returnResource() {
            JOptionPane.showMessageDialog(this, "Returning is still manual in DB for now.");
        }

        // =========================
        // VIEW FUNCTIONS
        // =========================
        private void viewBorrowers() {
            StringBuilder sb = new StringBuilder();
            sb.append("BORROWERS:\n").append("-".repeat(40)).append("\n");
            for (String id : borrowers.borrowerNames.keySet()) {
                sb.append(id).append(" - ").append(borrowers.borrowerNames.get(id)).append("\n");
            }
            displayArea.setText(sb.toString());
        }

        private void viewHistory() {
            StringBuilder sb = new StringBuilder();
            sb.append("SYSTEM INFO:\n").append("-".repeat(40)).append("\n");
            displayArea.setText(sb.toString());
        }

        private void viewByType(String type) {
            StringBuilder sb = new StringBuilder();
            sb.append(type).append(":\n").append("-".repeat(30)).append("\n");
            List<String> items = switch (type) {
                case "Gadgets" -> resources.gadgets;
                case "Tools" -> resources.tools;
                case "Rooms" -> resources.rooms;
                default -> new ArrayList<>();
            };
            for (String item : items) sb.append("• ").append(item).append("\n");
            displayArea.setText(sb.toString());
        }
    }
}
