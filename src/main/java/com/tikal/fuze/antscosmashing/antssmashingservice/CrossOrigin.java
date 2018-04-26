package com.tikal.fuze.antscosmashing.antssmashingservice;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CorsHandler;

public class CrossOrigin {
    public static void addCors(Router router){
        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET).allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.OPTIONS).allowedHeader("X-PINGARUNER").allowedHeader("Content-Type"));

        router.get("/access-control-with-get").handler(ctx -> {

            ctx.response().setChunked(true);

            final MultiMap headers = ctx.request().headers();
            for (final String key : headers.names()) {
                ctx.response().write(key);
                ctx.response().write(headers.get(key));
                ctx.response().write("\n");
            }

            ctx.response().end();
        });

        router.post("/access-control-with-post-preflight").handler(ctx -> {
            ctx.response().setChunked(true);

            final MultiMap headers = ctx.request().headers();
            for (final String key : headers.names()) {
                ctx.response().write(key);
                ctx.response().write(headers.get(key));
                ctx.response().write("\n");
            }

            ctx.response().end();
        });
    }
}
