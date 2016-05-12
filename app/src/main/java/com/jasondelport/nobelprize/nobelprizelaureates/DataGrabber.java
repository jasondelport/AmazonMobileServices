package com.jasondelport.nobelprize.nobelprizelaureates;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBScanExpression;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.PaginatedScanList;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Observable;
import rx.functions.Func0;
import timber.log.Timber;

/**
 * Created by jasondelport on 10/05/16.
 */
public class DataGrabber {

    private static DataGrabber instance = null;
    private static AmazonDynamoDBClient ddbClient;

    protected DataGrabber(AmazonDynamoDBClient ddbClient) {
        this.ddbClient = ddbClient;
    }

    public static DataGrabber getInstance(AmazonDynamoDBClient ddbClient) {
        if (instance == null) {
            instance = new DataGrabber(ddbClient);
        }
        return instance;
    }

    public Observable<NobelPrize> getObservableNobelPrizes() {
        return Observable.defer(new Func0<Observable<NobelPrize>>() {
            @Override
            public Observable<NobelPrize> call() {
                try {
                    return Observable.from(getNobelPrizes());
                } catch (Exception e) {
                    Timber.e("Error -> %s", e.getMessage());
                    return Observable.error(e);
                }
            }
        });
    }


    public List<NobelPrize> getNobelPrizes() {

        HashMap<String, String> nameMap = new HashMap<>();
        nameMap.put("#yr", "year");

        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":year", new AttributeValue().withN("1995"));

        DynamoDBMapper mapper = new DynamoDBMapper(ddbClient);

        DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
                .withFilterExpression("#yr = :year")
                .withExpressionAttributeValues(eav).withExpressionAttributeNames(nameMap);

        //eav.put(":category", new AttributeValue().withS("physics"));
        //DynamoDBScanExpression scanExpression = new DynamoDBScanExpression(); // all
        //DynamoDBScanExpression scanExpression = new DynamoDBScanExpression()
        //        .withFilterExpression("#yr = :year and category = :category")
        //        .withExpressionAttributeValues(eav).withExpressionAttributeNames(nameMap);

        try {
            PaginatedScanList<NobelPrize> result = mapper.scan(
                    NobelPrize.class, scanExpression);

            ArrayList<NobelPrize> resultList = new ArrayList<>();
            for (NobelPrize np : result) {
                resultList.add(np);

            }
            return resultList;

        } catch (AmazonServiceException e) {
            Timber.e("Error -> %s", e.getMessage());
        }
        return null;
    }

}
