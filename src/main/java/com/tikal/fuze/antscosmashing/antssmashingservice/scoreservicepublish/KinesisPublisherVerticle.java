package com.tikal.fuze.antscosmashing.antssmashingservice.scoreservicepublish;

import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.kinesis.model.PutRecordRequest;
import com.amazonaws.services.kinesis.model.PutRecordResult;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

public class KinesisPublisherVerticle extends AbstractAwsPublisherVerticle{
    private static final Logger logger = LoggerFactory.getLogger(KinesisPublisherVerticle.class);

    private AmazonKinesis kinesisClient;
    private String region;
    private String kinesisStreamName;
    private String partitionKey;


    @Override
    public void start() {
        super.start();
        region = config().getJsonObject("scorePublisherDetails").getString("kinesisPublisher.region");
        kinesisStreamName=config().getJsonObject("scorePublisherDetails").getString("kinesisPublisher.kinesisStreamName");
        partitionKey=config().getJsonObject("scorePublisherDetails").getString("kinesisPublisher.partitionKey");
        createKinesiClient();
    }

    @Override
    protected boolean isActive() {
        return publishMethod.equals("kinesis");
    }

    @Override
    protected void postHitTrialForPlayer(JsonObject hitTrial) {
        if(!isActive()){
            logger.debug("publishMethod is {} which is different than 'kinesis'. We will not publish it from this verticle",publishMethod);
            return;
        }
        PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setStreamName(kinesisStreamName);
        putRecordRequest.setData(ByteBuffer.wrap(hitTrial.toString().getBytes()));
        putRecordRequest.setPartitionKey(hitTrial.getString(partitionKey));
        PutRecordResult putRecordResult = kinesisClient.putRecord(putRecordRequest);
        logger.debug("succfuly sent to Kinesis {}",hitTrial);
    }


    private void createKinesiClient(){
        AmazonKinesisClientBuilder clientBuilder = AmazonKinesisClientBuilder.standard();
        clientBuilder.setRegion(region);
//        clientBuilder.setCredentials(credentialsProvider);
//        clientBuilder.setClientConfiguration(config);
        kinesisClient = clientBuilder.build();
    }




}
