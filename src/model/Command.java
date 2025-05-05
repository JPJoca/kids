package model;

import java.util.HashMap;
import java.util.Map;

public class Command {

    private final String name;
    private final Map<String,String> args;

  public Command(String name) {
      this.name = name.toUpperCase();
      this.args = new HashMap<>();
  }

    public void addArg(String key, String value) {
        args.put(key.toLowerCase(), value);
    }

    public String getName() {
        return name;
    }

    public String getArg(String key) {
        return args.get(key.toLowerCase());
    }

    public boolean hasArg(String key) {
        return args.containsKey(key.toLowerCase());
    }

    public Map<String, String> getAllArgs() {
        return args;
    }
}

