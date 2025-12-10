public class Main {
    public static void main(String[] args) {
        try {
            DBConnection.connect();
            System.out.println("Connected successfully!");
            DBConnection.ensureSchema();
            System.out.println("Database schema ensured.");
            // Launch the GUI so this configuration also opens the app
            smartborrow.main(args);
        } catch (Exception e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }
}
