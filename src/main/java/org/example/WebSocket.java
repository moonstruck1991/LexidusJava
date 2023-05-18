package org.example;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class WebSocket{

    // 2 maps to store the number of BIDS and ASKS
    NavigableMap<BigDecimal,BigDecimal> BIDS;
    NavigableMap<BigDecimal,BigDecimal> ASKS;

    private long lastUpdateId;

    // constructor
    public WebSocket(String symbol, int param) {
        initialize(symbol,param);
        startStreaming(symbol, param);
    }

    // initialize the orderbook
    private void initialize(String symbol, int param) {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        BinanceApiRestClient client = factory.newRestClient();
        OrderBook orderBook = client.getOrderBook(symbol.toUpperCase(), param);

        this.ASKS = new TreeMap<>();
        this.BIDS = new TreeMap<>(Comparator.reverseOrder());
        this.lastUpdateId = orderBook.getLastUpdateId();

        for (OrderBookEntry ask : orderBook.getAsks()) {
            ASKS.put(new BigDecimal(ask.getPrice()), new BigDecimal(ask.getQty()));
        }

        for (OrderBookEntry bid : orderBook.getBids()) {
            BIDS.put(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()));
        }
    }

    //open websocket client and get information of the bids and asks
    private void startStreaming(String symbol, int param) {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        BinanceApiWebSocketClient client = factory.newWebSocketClient();

        AtomicLong currTime = new AtomicLong(0);

        client.onDepthEvent(symbol.toLowerCase(), response -> {
            if ((response.getUpdateId() > lastUpdateId) && (System.currentTimeMillis()- currTime.get() >= 10000) ) {
                currTime.set(System.currentTimeMillis());
                lastUpdateId = response.getUpdateId();
                updateOrderBook(ASKS, response.getAsks());
                updateOrderBook(BIDS, response.getBids());
                printOrderBook(param);
            }
        });
    }

    // update the orderbook using deltas
    private void updateOrderBook(NavigableMap<BigDecimal, BigDecimal> lastOrderBookEntries, List<OrderBookEntry> orderBookDeltas) {
        for (OrderBookEntry orderBookDelta : orderBookDeltas) {
            BigDecimal price = new BigDecimal(orderBookDelta.getPrice());
            BigDecimal qty = new BigDecimal(orderBookDelta.getQty());
            if (qty.compareTo(BigDecimal.ZERO) == 0) {
                // qty=0 means remove this level
                lastOrderBookEntries.remove(price);
            } else {
                lastOrderBookEntries.put(price, qty);
            }
        }
    }



    // print the orderbook to PARAM levels
    private void printOrderBook(int param) {
        //System.out.println(depthCache);
        List<String> askPriceList = new ArrayList<>();
        List<String> askQtyList = new ArrayList<>();
        List<String> BidPriceList = new ArrayList<>();
        List<String> BidQtyList = new ArrayList<>();
        ASKS.entrySet().forEach(entry -> updateList(entry, askPriceList, askQtyList));
        BIDS.entrySet().forEach(entry -> updateList(entry, BidPriceList, BidQtyList));


        String bs = "BID_SIZE";
        String bp = "BID_PRICE";
        String as = "ASK_SIZE";
        String ap = "ASK_PRICE";
        System.out.printf("%-10s%10s %-10s%10s%n", bs,bp,ap,as);
        for(int i=0; i< param;i++){
            String nul = " ";
            if(i >= askPriceList.size() && i >= BidPriceList.size()){
                break;
            }
            else if(i >= askPriceList.size()){
                System.out.printf("%-10s%10s %-10s%10s%n", BidQtyList.get(i), BidPriceList.get(i), nul,nul);

            }
            else if(i >= BidPriceList.size()){
                System.out.printf("%-10s%10s %-10s%10s%n", nul, nul, askPriceList.get(i),askQtyList.get(i));

            }
            else{
                System.out.printf("%-10s%10s %-10s%10s%n", BidQtyList.get(i), BidPriceList.get(i), askPriceList.get(i),askQtyList.get(i));

            }

        }


    }
    // Use to put the values into a list
    private static void updateList(Map.Entry<BigDecimal, BigDecimal> depthCacheEntry, List<String> PriceList, List<String> QTYList) {
        PriceList.add(depthCacheEntry.getKey().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
        QTYList.add(depthCacheEntry.getValue().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
    }

    public static void main(String[] args) {

    }
}
