package cli;

import model.Command;

public class CommandParser {

    public static Command parse(String input) {
        String[] tokens = input.trim().split("\\s+");
        if (tokens.length == 0) return null;

        String commandName = tokens[0];
        Command command = new Command(commandName);

        for (int i = 1; i < tokens.length; i++) {
            if (tokens[i].startsWith("-")) {
                String key = normalizeKey(tokens[i]);

                if (i + 1 < tokens.length && !tokens[i + 1].startsWith("-")) {
                    String value = tokens[i + 1];
                    command.addArg(key, value);
                    i++;
                } else {

                    command.addArg(key, "true");
                }
            }
        }

        return command;
    }

    private static String normalizeKey(String rawKey) {
        return switch (rawKey) {
            case "--min", "-m" -> "min";
            case "--max", "-M" -> "max";
            case "--letter", "-l" -> "letter";
            case "--output", "-o" -> "output";
            case "--job", "-j" -> "job";
            case "--save-jobs", "-s" -> "save-jobs";
            case "--load-jobs", "-L" -> "load-jobs";
            default -> rawKey.replaceFirst("^-+", "");
        };
    }
}
