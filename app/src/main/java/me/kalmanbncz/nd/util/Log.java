package me.kalmanbncz.nd.util;

/**
 * Created by Kali on 2/23/2015.
 */
public class Log {
    public static void log(String message) {
        System.out.println("ND LOGGING:  " + message);
    }

    public static void log(int message) {
        System.out.println("ND LOGGING:  " + message);
    }

    public static void log(float message) {
        System.out.println("ND LOGGING:  " + message);
    }

    public static void log(double message) {
        System.out.println("ND LOGGING:  " + message);
    }

    public static void log(Object object) {
        System.out.println("ND LOGGING:  " + object);
    }
}
