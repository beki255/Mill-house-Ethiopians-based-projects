package fxml;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CheckEncoding {
    public static void main(String[] args) throws Exception {
        // Point this to your actual file path
        String filePath = "src/main/resources/i18n/messages_am.properties";
        
        System.out.println("Reading with UTF-8:");
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains("system_title")) {
                    System.out.println("Line: " + line);
                    String value = line.split("=")[1];
                    System.out.println("Value: " + value);
                    System.out.print("Chars: ");
                    for (char c : value.toCharArray()) {
                        System.out.printf("%c(U+%04X) ", c, (int)c);
                    }
                    System.out.println();
                }
            }
        }
    }
}