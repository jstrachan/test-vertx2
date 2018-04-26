package com.tikal.fuze.antscosmashing.antssmashingservice;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HitTrialPublisherVerticle  extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(HitTrialPublisherVerticle.class);

    private WebClient adminServiceWebClient;
    private Integer adminServicePort;
    private String adminServiceHost;


    @Override
    public void start() {
        adminServicePort = config().getJsonObject("adminService").getInteger("port");
        adminServiceHost = config().getJsonObject("adminService").getString("host");
        adminServiceWebClient = WebClient.create(vertx);

        vertx.eventBus().consumer("hit", event -> handleHit((JsonObject) event.body(),false));
        vertx.eventBus().consumer("selfHit", event -> handleHit((JsonObject) event.body(),true));
        vertx.eventBus().consumer("miss", event -> handleMiss((JsonObject) event.body()));

        vertx.eventBus().consumer("game.start",event -> gameStarted());
        vertx.eventBus().consumer("game.stop",event -> gameStopped());
        adminServiceWebClient = WebClient.create(vertx);
    }

    private void handleMiss(JsonObject hitTrial) {
        logger.debug("missed event:{}", hitTrial);
    }

    private void handleHit(JsonObject hitTrial, boolean isSelf) {
        String antId = hitTrial.getString("antId");
        Integer playerId = hitTrial.getInteger("playerId");
        String message;
        if(isSelf)
            message = "self-smash-message";
        else
            message = "smash-message";
        logger.debug("{} event:{}",message, hitTrial);
        vertx.eventBus().publish(message, new JsonObject().put("playerId", playerId).put("antId", antId));
    }


    private void gameStopped() {
        adminServiceWebClient.put(adminServicePort,adminServiceHost,"/games/stop")
                .sendJsonObject(
                        null,
                        ar -> {
                            if (ar.succeeded() && ar.result().statusCode()==200){
                                logger.info("Game finished we notifid admin about it");
                            } else {
                                if (ar.failed())
                                    logger.error("Failed to send gameStop message to admin service - Something went wrong error is : " ,ar.cause());
                                else
                                    logger.error("WFailed to send gameStop message to admin service - Something went wrong wrong error is : {}" , ar.result().bodyAsString());
                            }
                        });
    }



    private void gameStarted() {
        logger.info("Game Started. Will clear all data...");
    }
}