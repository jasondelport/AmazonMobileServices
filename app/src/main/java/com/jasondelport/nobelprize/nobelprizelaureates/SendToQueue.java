package com.jasondelport.nobelprize.nobelprizelaureates;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.SendMessageRequest;
import com.amazonaws.services.sqs.model.SendMessageResult;

import java.util.List;

import timber.log.Timber;

/**
 * Created by jasondelport on 11/05/16.
 */
public class SendToQueue {
    private static SendToQueue instance = null;
    private static AmazonSQS sqsClient;

    protected SendToQueue(AmazonSQS sqsClient) {
        this.sqsClient = sqsClient;
    }

    public static SendToQueue getInstance(AmazonSQS sqsClient) {
        if (instance == null) {
            instance = new SendToQueue(sqsClient);
        }
        return instance;
    }

    public List<String> listQueues() {
        try {
            Timber.d("Listing all queues");
            List<String> queues = sqsClient.listQueues().getQueueUrls();
            for (String queueUrl : queues) {
                Timber.d("QueueUrl: " + queueUrl);
            }
            return queues;
        } catch (Exception e) {
            Timber.e("Error -> %s", e.getMessage());
            return null;
        }
    }

    public SendMessageResult sendMessage(String queueUrl, String message) {
        try {
            Timber.d("Sending message");
            SendMessageResult result = sqsClient.sendMessage(new SendMessageRequest(queueUrl, message));
            return result;
        } catch (Exception e) {
            Timber.e("Error -> %s", e.getMessage());
            return null;
        }
    }
}
