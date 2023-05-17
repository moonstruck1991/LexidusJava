package org.example;

public class Main {
    public static void main(String[] args) {
        String symbol = args[0]; //Ethusdt
        int param = Integer.parseInt(args[1]); //10
        new WebSocket(symbol.toUpperCase(),param); }
}