package com.tikal.fuze.antscosmashing.antssmashingservice;


import okhttp3.*;
import okio.ByteString;

public class WebSocketClientForSmashingServiceWS extends WebSocketListener {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WebSocketClientForSmashingServiceWS.class);


    int playerId = 10;
    String hitTrial = "{ \\\\\\\"type\\\\\\\":\\\\\\\"hit\\\\\\\" , \\\\\\\"antId\\\\\\\":\\\\\\\"333333\\\\\\\"}";

//
    String server = "localhost";
//        String server = "52.41.200.225";


    private void run() {
        OkHttpClient client = new OkHttpClient.Builder().build();

        Request request = new Request.Builder()
                .url("http://"+server+":5080/client.register/123/abc_"+playerId+"/websocket")
                .build();
        client.newWebSocket(request, this);
        // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
        client.dispatcher().executorService().shutdown();
    }

    @Override public void onOpen(WebSocket webSocket, Response response) {
        new Thread(()->ping(webSocket)).start();
        webSocket.send("[\"{\\\"type\\\":\\\"register\\\",\\\"address\\\":\\\"smash-message\\\",\\\"headers\\\":{}}\"]");
        webSocket.send("[\"{\\\"type\\\":\\\"register\\\",\\\"address\\\":\\\"self-smash-message\\\",\\\"headers\\\":{}}\"]");
        webSocket.send("[\"{\\\"type\\\":\\\"register\\\",\\\"address\\\":\\\"playerScore-message\\\",\\\"headers\\\":{}}\"]");
        webSocket.send("[\"{\\\"type\\\":\\\"register\\\",\\\"address\\\":\\\"teamScore-message\\\",\\\"headers\\\":{}}\"]");

        String text = "[\"{\\\"type\\\":\\\"send\\\",\\\"address\\\":\\\"hit-trial-message\\\",\\\"body\\\":\\\"" + hitTrial + "\\\",\\\"headers\\\":{}}\"]";
        System.out.println(text);
        webSocket.send(text);
    }

    private void ping(WebSocket webSocket) {
        while (true){
            webSocket.send("[\"{\\\"type\\\":\\\"ping\\\"}\"]");
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override public void onMessage(WebSocket webSocket, String text) {
        if(!text.startsWith("h"))
            System.out.println("MESSAGE: " + text);

    }

    @Override public void onMessage(WebSocket webSocket, ByteString bytes) {
        System.out.println("MESSAGE: " + bytes.hex());
    }

    @Override public void onClosing(WebSocket webSocket, int code, String reason) {
        webSocket.close(1000, null);
        System.out.println("CLOSE: " + code + " " + reason);
    }

    @Override public void onFailure(WebSocket webSocket, Throwable t, Response response) {
        t.printStackTrace();
    }

    public static void main(String... args) {
        new WebSocketClientForSmashingServiceWS().run();
    }

}

