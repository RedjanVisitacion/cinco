import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBConnection {

    public static final String DB_URL = "jdbc:mysql://localhost:3306/smartcampusborrowing?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true";
    public static final String DB_USER = "root";
    public static final String DB_PASS = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    public static void ensureSchema() throws SQLException {
        try (Connection conn = connect();
             Statement st = conn.createStatement()) {
            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS resources (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "type VARCHAR(20) NOT NULL, " +
                "name VARCHAR(100) NOT NULL, " +
                "UNIQUE KEY uniq_type_name (type, name)" +
                ")"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS borrowings (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "borrower_id VARCHAR(50) NOT NULL, " +
                "borrower_name VARCHAR(100) NOT NULL, " +
                "type VARCHAR(20) NOT NULL, " +
                "item VARCHAR(100) NOT NULL, " +
                "borrow_time DATETIME NOT NULL" +
                ")"
            );

            st.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "password VARCHAR(255) NOT NULL, " +
                "role VARCHAR(20) NOT NULL DEFAULT 'user'" +
                ")"
            );

            st.executeUpdate(
                "INSERT IGNORE INTO users(username, password, role) VALUES ('admin','admin123','admin')"
            );
        }
    }
}
