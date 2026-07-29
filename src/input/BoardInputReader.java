package input; // תיקייה חדשה לקלט

import model.Board;

import java.util.Scanner;
import java.util.ArrayList;
import java.util.List;

public class BoardInputReader {

    public static Board readBoard(Scanner scanner) {
        List<String> boardLines = new ArrayList<>();
        int expectedLength = -1;

      
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine().trim();
            if (line.isEmpty() || line.equals("Board:")) continue;
            if (line.equals("Commands:")) break;
            String[]tokens=line.split("\\s+");
            if (expectedLength == -1) {
                expectedLength = tokens.length;
            }else if(tokens.length!=expectedLength){
                System.out.println("ERROR ROW_WIDTH_MISMATCH");
                System.exit(0);
            }
            for (String token : tokens) {
                if (!isValidToken(token)) {
                    System.out.println("ERROR UNKNOWN_TOKEN");
                    System.exit(0);
                }
            }
            boardLines.add(line);
        }

        int numRows = boardLines.size();
        String[][] rawData = new String[numRows][];
        for (int i = 0; i < numRows; i++) {
            rawData[i] = boardLines.get(i).split("\\s+");
        }

       
        return new Board(rawData);
    }

    private static boolean isValidToken(String token) {
        if (token.equals(".")) return true; 
        if (token.length() != 2) return false;

        char color = token.charAt(0);
        char type = token.charAt(1);

        boolean validColor = (color == 'w' || color == 'b');
        boolean validType = "PRNBQK".indexOf(type) != -1;

        return validColor && validType;
    }
}