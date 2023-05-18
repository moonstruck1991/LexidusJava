package org.example;

public class Main {
    public static void main(String[] args) {

        if(args.length == 0){
            new WebSocket("ETHUSDT",10);
        } else if (args.length==1) {
            String symbol = args[0]; //Ethusdt
            new WebSocket(symbol.toUpperCase(), 10);
        } else{
            String symbol = args[0]; //Ethusdt
            int param = Integer.parseInt(args[1]); //10
            new WebSocket(symbol.toUpperCase(),param);
        }
    }
}