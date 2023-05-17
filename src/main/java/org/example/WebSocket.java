package org.example;

import com.binance.api.client.BinanceApiClientFactory;
import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.BinanceApiWebSocketClient;
import com.binance.api.client.domain.market.OrderBook;
import com.binance.api.client.domain.market.OrderBookEntry;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class WebSocket{

    private static final String BIDS = "BIDS";
    private static final String ASKS = "ASKS";

    private long lastUpdateId;

    private Map<String, NavigableMap<BigDecimal, BigDecimal>> depthCache;

    public WebSocket(String symbol, int param) {
        initializeDepthCache(symbol);
        startDepthEventStreaming(symbol, param);
    }

    /**
     * Initializes the depth cache by using the REST API.
     */
    private void initializeDepthCache(String symbol) {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        BinanceApiRestClient client = factory.newRestClient();
        OrderBook orderBook = client.getOrderBook(symbol.toUpperCase(), 10);

        this.depthCache = new HashMap<>();
        this.lastUpdateId = orderBook.getLastUpdateId();

        NavigableMap<BigDecimal, BigDecimal> asks = new TreeMap<>(Comparator.reverseOrder());
        for (OrderBookEntry ask : orderBook.getAsks()) {
            asks.put(new BigDecimal(ask.getPrice()), new BigDecimal(ask.getQty()));
        }
        depthCache.put(ASKS, asks);

        NavigableMap<BigDecimal, BigDecimal> bids = new TreeMap<>(Comparator.reverseOrder());
        for (OrderBookEntry bid : orderBook.getBids()) {
            bids.put(new BigDecimal(bid.getPrice()), new BigDecimal(bid.getQty()));
        }
        depthCache.put(BIDS, bids);
    }

    /**
     * Begins streaming of depth events.
     */
    private void startDepthEventStreaming(String symbol, int param) {
        BinanceApiClientFactory factory = BinanceApiClientFactory.newInstance();
        BinanceApiWebSocketClient client = factory.newWebSocketClient();

        AtomicLong currTime = new AtomicLong(System.currentTimeMillis());
        AtomicInteger first = new AtomicInteger(1);

        client.onDepthEvent(symbol.toLowerCase(), response -> {
            if ((response.getUpdateId() > lastUpdateId) && (System.currentTimeMillis()- currTime.get() >= 10000 || first.get() == 1) ) {
                currTime.set(System.currentTimeMillis());
                first.set(0);
                //System.out.println(response);
                lastUpdateId = response.getUpdateId();
                updateOrderBook(getAsks(), response.getAsks());
                updateOrderBook(getBids(), response.getBids());
                printDepthCache(param);
            }
        });
    }

    /**
     * Updates an order book (bids or asks) with a delta received from the server.
     * <p>
     * Whenever the qty specified is ZERO, it means the price should was removed from the order book.
     */
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

    public NavigableMap<BigDecimal, BigDecimal> getAsks() {
        return depthCache.get(ASKS);
    }

    public NavigableMap<BigDecimal, BigDecimal> getBids() {
        return depthCache.get(BIDS);
    }

    /**
     * @return the best ask in the order book
     */
    private Map.Entry<BigDecimal, BigDecimal> getBestAsk() {
        return getAsks().lastEntry();
    }

    /**
     * @return the best bid in the order book
     */
    private Map.Entry<BigDecimal, BigDecimal> getBestBid() {
        return getBids().firstEntry();
    }

    /**
     * @return a depth cache, containing two keys (ASKs and BIDs), and for each, an ordered list of book entries.
     */
    public Map<String, NavigableMap<BigDecimal, BigDecimal>> getDepthCache() {
        return depthCache;
    }

    /**
     * Prints the cached order book / depth of a symbol as well as the best ask and bid price in the book.
     */
    private void printDepthCache(int param) {
        //System.out.println(depthCache);
        List<String> askPriceList = new ArrayList<>();
        List<String> askQtyList = new ArrayList<>();
        List<String> BidPriceList = new ArrayList<>();
        List<String> BidQtyList = new ArrayList<>();
        getAsks().entrySet().forEach(entry -> toDepthCacheEntryString(entry, askPriceList, askQtyList));
        getBids().entrySet().forEach(entry -> toDepthCacheEntryString(entry, BidPriceList, BidQtyList));


        System.out.printf("BID_SIZE   BID_PRICE ASK_PRICE   ASK_SIZE%n");
        for(int i=0; i< param;i++){
            String nul = new String(" ");
            if(i >= askPriceList.size() && i >= BidPriceList.size()){
                break;
            }
            else if(i >= askPriceList.size()){
                System.out.printf("%-10s%10s %-10s%10s%n", BidQtyList.get(i), BidPriceList.get(i), nul,nul);

            }
            else if(i >= BidPriceList.size()){
                System.out.printf("%-10s%10s %-10s%10s%n", nul, nul, askPriceList.get(askPriceList.size()-i-1),askQtyList.get(askQtyList.size()-i-1));

            }
            else{
                System.out.printf("%-10s%10s %-10s%10s%n", BidQtyList.get(i), BidPriceList.get(i), askPriceList.get(askPriceList.size()-i-1),askQtyList.get(askQtyList.size()-i-1));

            }

        }


    }

    /**
     * Pretty prints an order book entry in the format "price / quantity".
     */
    private static void toDepthCacheEntryString(Map.Entry<BigDecimal, BigDecimal> depthCacheEntry, List<String> PriceList, List<String> QTYList) {

        PriceList.add(depthCacheEntry.getKey().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());
        QTYList.add(depthCacheEntry.getValue().setScale(2, BigDecimal.ROUND_HALF_UP).toPlainString());


    }

    public static void main(String[] args) {
        ;
    }
}
