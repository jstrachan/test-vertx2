package com.tikal.fuze.antscosmashing.antssmashingservice;

import com.tikal.fuze.antscosmashing.antssmashingservice.scoreservicepublish.GatewayPublisherVerticle;
import com.tikal.fuze.antscosmashing.antssmashingservice.scoreservicepublish.GatewayToKinesisPublisherVerticle;
import com.tikal.fuze.antscosmashing.antssmashingservice.scoreservicepublish.KinesisPublisherVerticle;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeEventType;
import io.vertx.ext.bridge.PermittedOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions;

import static com.tikal.fuze.antscosmashing.antssmashingservice.CrossOrigin.addCors;

public class SmashingWSServerVerticle extends AbstractVerticle {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SmashingWSServerVerticle.class);


  @Override
  public void start() {
      deployVerticle(HitTrialPublisherVerticle.class.getName());
      deployVerticle(GatewayPublisherVerticle.class.getName());
      deployVerticle(KinesisPublisherVerticle.class.getName());
      deployVerticle(GatewayToKinesisPublisherVerticle.class.getName());

      final Router router = Router.router(vertx);
      addCors(router);
      final BridgeOptions options = new BridgeOptions().setPingTimeout(config().getInteger("clientMinPingIntervalInSec")*1000)
              .addInboundPermitted(new PermittedOptions().setAddress("hit-trial-message"))
              .addOutboundPermitted(new PermittedOptions().setAddressRegex("smash-message.*"))
              .addOutboundPermitted(new PermittedOptions().setAddressRegex("self-smash-message.*"))
              .addOutboundPermitted(new PermittedOptions().setAddressRegex("playerScore-message.*"))
              .addOutboundPermitted(new PermittedOptions().setAddressRegex("teamScore-message.*"));

      router.route("/client.register/*").handler(SockJSHandler.create(vertx, new SockJSHandlerOptions().setHeartbeatInterval(config().getInteger("serverSendHeartbeatIntervalInSec") * 1000)).bridge(options, this::handleBridgeEvent));

      //This one is for testing purpose
      router.route("/hit-trial").handler(BodyHandler.create()).handler(rc -> {
          vertx.eventBus().publish("hit", rc.getBodyAsJson());
          rc.response().end();
      });
      //This one is for testing purpose
      router.post("/publish-hits/:method").handler(rc -> {
          vertx.eventBus().publish("post-publish-hits-method", rc.request().getParam("method"));
          rc.response().end();
      });
      //This one is for testing purpose
      router.get("/publish-hits/method").handler(rc -> {
          vertx.eventBus().send("get-publish-hits-method", null,ar->rc.response().end(ar.result().body().toString()));
      });

      router.route().handler(StaticHandler.create());
      vertx.createHttpServer().requestHandler(router::accept).listen(config().getInteger("http-port"));

      logger.info("Started the HTTP server for map-tiles om port {}...",config().getInteger("http-port"));

  }

    private void handleBridgeEvent(final BridgeEvent event) {
        String uri = event.socket().uri();
        int playerId = Integer.valueOf(extractPlayerId(uri.split("/")[3]));
        if (event.type() == BridgeEventType.SEND) {
            logger.debug("Got SEND message from payer {}:{}",playerId, event.getRawMessage().toString());
            String body = event.getRawMessage().getString("body");
            JsonObject hitTrial = new JsonObject(body);
            vertx.eventBus().publish(hitTrial.getString("type"),hitTrial.put("playerId",playerId));
        }

        if (event.type() == BridgeEventType.SOCKET_CREATED)
            logger.info("A socket was created by {}",playerId);
        else if (event.type() == BridgeEventType.SOCKET_CLOSED)
            vertx.eventBus().send("socket.closed",playerId);
        event.complete(true);
    }



    private String extractPlayerId(String sessionId1) {
        String sessionId = sessionId1;
        return sessionId.split("_")[1];
    }

    protected void deployVerticle(String className) {
        vertx.deployVerticle(className,new DeploymentOptions().setConfig(config()), res -> {
            if (res.succeeded()) {
                logger.info("Deployed {} verticle", className);
            } else {
                logger.error("Error deploying {} verticle:", className, res.cause());
            }
        });
    }

}
