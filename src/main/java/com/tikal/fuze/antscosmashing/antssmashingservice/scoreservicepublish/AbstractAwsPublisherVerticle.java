package com.tikal.fuze.antscosmashing.antssmashingservice.scoreservicepublish;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractAwsPublisherVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(AbstractAwsPublisherVerticle.class);

    private WebClient client;

    private Integer adminServicePort;
    private String adminServiceHost;

    // playerId -> ref
    private Map<Integer,JsonObject> playersDetails = new HashMap();


    protected WebClient getClient() {
        return client;
    }

    protected String publishMethod;

    private DateTimeFormatter dateFormatter  = DateTimeFormatter.BASIC_ISO_DATE;
    private DateTimeFormatter timeFormatter  = DateTimeFormatter.ofPattern("HHmmss");


    @Override
    public void start() {
        publishMethod = config().getString("publishMethod");

        adminServicePort = config().getJsonObject("adminService").getInteger("port");
        adminServiceHost = config().getJsonObject("adminService").getString("host");


        vertx.eventBus().<String>consumer("post-publish-hits-method",e->publishMethod=e.body());
        vertx.eventBus().<String>consumer("get-publish-hits-method",e->e.reply(publishMethod));

        vertx.eventBus().consumer("hit",event -> postHitTrial((JsonObject) event.body()));
        vertx.eventBus().consumer("selfHit",event -> postHitTrial((JsonObject) event.body()));
        vertx.eventBus().consumer("miss",event -> postHitTrial((JsonObject) event.body()));

        vertx.eventBus().consumer("game.start",event -> gameStarted());
        vertx.eventBus().consumer("game.stop",event -> gameStopped());

        client = WebClient.create(vertx);
    }

    private void gameStopped() {
        if(!isActive())
            return;
        logger.debug("Game stopped");
        playersDetails.clear();
    }

    private void gameStarted() {
        if(!isActive())
            return;
        logger.debug("Game started");
        playersDetails.clear();
    }


    private void postHitTrial(JsonObject hitTrial) {
        if(!isActive())
            return;
        Integer playerId = hitTrial.getInteger("playerId");
        JsonObject playerRef = playersDetails.get(playerId);
        if(playerRef!=null)
            postHitTrialForPlayer(hitTrial,playerRef);
        else {
            client.get(adminServicePort, adminServiceHost, "/players/references/" + playerId).send(ar -> {
                if (ar.succeeded() && ar.result().statusCode() == 200) {
                    if (ar.result().body() == null)
                        logger.error("Failed to get references for playerId {}", playerId);
                    else {
                        JsonObject ref = ar.result().bodyAsJsonObject();
                        logger.debug("Got reply from admin REST API for playerId {} the following ref {}", playerId, ref);
                        playersDetails.put(playerId, ref);
                        postHitTrialForPlayer(hitTrial, ref);
                    }
                } else {
                    if (ar.failed())
                        logger.error("Failed to get references from admin for this player (check the playerId) in this message). Will NOT save this Hit-Trial {} to score service - Something went wrong error is : " ,hitTrial ,ar.cause());
                    else
                        logger.error("Failed to get references from admin for this player (check the playerId) in this message). Will NOT save this Hit-Trial {} to score service - Something went wrong wrong error is : {}" ,hitTrial, ar.result().bodyAsString());
                }
            });
        }
    }

    private void postHitTrialForPlayer(JsonObject hitTrial, JsonObject ref) {
        LocalDateTime now = LocalDateTime.now();
        int date = Integer.valueOf(dateFormatter.format(now));
        int time = Integer.valueOf(timeFormatter.format(now));
        ref.put("date",date);
        ref.put("time",time);
        hitTrial.mergeIn(ref);
        postHitTrialForPlayer(hitTrial);
    }

    protected abstract boolean isActive() ;
    protected abstract void postHitTrialForPlayer(JsonObject hitTrial) ;






}
