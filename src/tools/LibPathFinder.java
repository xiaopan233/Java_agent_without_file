package tools;

import java.util.Properties;

public class LibPathFinder {
    public static String getLibPath() throws Exception{
        Properties properties = System.getProperties();
        String libPath = properties.getProperty("sun.boot.library.path");
        if (libPath == null){
            throw new Exception("[-] LibPath Error");
        }
        System.out.println("[+] Find lib path: " + libPath);
        return properties.getProperty("sun.boot.library.path");
    }
}
