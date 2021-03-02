package kursayin.team0.regexes;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class URLRegEx {
    public static final String regex = "(?<protocol>tcp)://(?<host>([a-zA-Z]+(.[a-zA-Z]+)+)|localhost|\\d{1,3}(.\\d{1,3}){3}):(?<port>\\d{1,5})";
    private static final Pattern pattern = Pattern.compile(regex);
    private Matcher matcher = null;

    public boolean isValidURL(String url) {
        if (url == null) {
            return false;
        }
        matcher = pattern.matcher(url);
        return matcher.find() && matcher.group().equals(url);
    }


    public String getHost() {
        if (matcher != null) {
            return matcher.group("host");
        }
        return null;
    }

    public int getPort() {
        if (matcher != null) {
            return Integer.parseInt(matcher.group("port"));
        }
        return -1;
    }
}
