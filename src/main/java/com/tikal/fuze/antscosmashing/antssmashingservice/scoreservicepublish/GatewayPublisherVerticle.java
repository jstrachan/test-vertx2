package com.tikal.fuze.antscosmashing.antssmashingservice.scoreservicepublish;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class GatewayPublisherVerticle extends AbstractAwsPublisherVerticle {
    private static final Logger logger = LoggerFactory.getLogger(GatewayPublisherVerticle.class);

    private String host;
    private String uri;

    @Override
    public void start() {
        super.start();
        host = config().getJsonObject("scorePublisherDetails").getString("gatewayPublisher.host");
        uri= config().getJsonObject("scorePublisherDetails").getString("gatewayPublisher.uri");
    }

    @Override
    protected boolean isActive() {
        return publishMethod.equals("gatewayToLambda");
    }

    @Override
    protected void postHitTrialForPlayer(JsonObject hitTrial) {
        if(!isActive()){
            logger.debug("publishMethod is {} which is different than 'gatewayToLambda'. We will not publish it from this verticle",publishMethod);
            return;
        }

        getClient().post(443,host,uri)
                .ssl(true)
                .sendJsonObject( hitTrial, ar -> {
                    if (ar.succeeded() && ar.result().statusCode()==200){
                        logger.debug("succfuly sent {}. result is {}",hitTrial,ar.result().bodyAsString());
                    } else {
                        if (ar.failed())
                            logger.error("Will NOT save this Hit-Trial {} to score service - Something went wrong error is : " ,hitTrial ,ar.cause());
                        else
                            logger.error("Will NOT save this Hit-Trial {} to score service - Something went wrong wrong error is : {}" ,hitTrial, ar.result().bodyAsString());
                    }
                });
    }

}
