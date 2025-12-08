import APIManagement.APIManager;
import DataBaseManagement.DataBaseManager;
import QRManager.QRcodeGenerator;
import utilities.InputManager;
import utilities.WelcomeUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());
    private static final int QR_CODE_WIDTH = 350;
    private static final int QR_CODE_HEIGHT = 350;
    private static String response="";

    public static void main(String[] args) {
        InputManager inputManager = null;
        APIManager apiManager = null;

        try {
            // 1. Initialize InputManager
            inputManager = new InputManager();

            // 2. Welcome user
            try {
                String name = inputManager.getStringInput("Please enter your name: ");
                WelcomeUtility.welcomeUser(name);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during welcome process", e);
                System.out.println("Welcome to QR Code Generator!");
            }

            // 3. Take input from user
            String url = null;
            String mobileNumber = null;

            try {
                url = inputManager.getStringInput("Please enter the URL to encode in QR Code: ");
                mobileNumber = inputManager.getStringInput("Please enter the recipient mobile number: ");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error reading user input", e);
                System.err.println("Failed to read input. Using default values.");
                url = "https://devfolios.online";
                mobileNumber = "0000000000";
            }

            // 4. Generate QR Code
            try {
                QRcodeGenerator qRcodeGenerator = new QRcodeGenerator();
                boolean isGenerated = qRcodeGenerator.generateQRcode(url, QR_CODE_WIDTH, QR_CODE_HEIGHT, "web.png");

                if (!isGenerated) {
                    LOGGER.log(Level.SEVERE, "Failed to generate QR Code");
                    System.err.println("Failed to generate QR Code.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Unexpected error during QR code generation", e);
                System.err.println("Error generating QR Code: " + e.getMessage());
            }

            // 6. Call API using APIManager
            try {
                apiManager = new APIManager();

                String jsonBody = """
                        {
                          "contents": [
                            {
                              "parts": [
                                {
                                  "text": "summarize in 3 lines the given url """ + url + """ 
                                "
                                }
                              ]
                            }
                          ]
                        }
                        """;

                System.out.println(jsonBody);
                // Prepare headers
                Map<String, String> headers = new HashMap<>();
                headers.put("x-goog-api-key", "AIzaSyC-hKwhMWRNWCI8tp0vA7WqhU4ql-dhBLo");
                headers.put("Content-Type", "application/json");

                // Execute POST request using APIManager
                response = apiManager.executePOST(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent",
                        jsonBody,
                        headers
                );

                if (response != null) {
                    System.out.println("API Response:");
                    System.out.println(response);
                    LOGGER.log(Level.INFO, "API call completed successfully");
                } else {
                    LOGGER.log(Level.WARNING, "API request failed or returned null");
                    System.err.println("API request failed.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during API call", e);
                System.err.println("API call error: " + e.getMessage());
            }




            // 5. Store data in database
            try {

                DataBaseManager dataBaseManager = new DataBaseManager();
                String query ="INSERT INTO qrcode_generator (link, recipient_mobile_number, generated_qr_code, short_description) VALUES(' "+url+"','"+mobileNumber+"','QRCOdeDmo','"+response+"')";

                boolean queryResult = dataBaseManager.triggerQuery(query);

                if (!queryResult) {
                    LOGGER.log(Level.WARNING, "Failed to store data in database");
                    System.err.println("Failed to store data in database.");
                } else {
                    LOGGER.log(Level.INFO, "Data stored successfully in database");
                    System.out.println("Data stored successfully in database.");
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error interacting with database", e);
                System.err.println("Database error: " + e.getMessage());
            }



        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Unexpected error in main application", e);
            System.err.println("Application error: " + e.getMessage());
        } finally {
            // Clean up resources
            if (apiManager != null) {
                try {
                    apiManager.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error closing APIManager", e);
                }
            }

            if (inputManager != null) {
                try {
                    inputManager.close();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Error closing InputManager", e);
                }
            }

            LOGGER.log(Level.INFO, "Application shutdown complete");
            System.out.println("\nApplication completed.");
        }
    }

    /**
     * Sanitize SQL string to prevent SQL injection
     * @param input The input string
     * @return Sanitized string
     */
    private static String sanitizeSqlString(String input) {
        if (input == null) {
            return "";
        }
        // Basic sanitization - replace single quotes
        return input.replace("'", "''");
    }
}


