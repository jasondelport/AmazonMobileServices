package com.jasondelport.nobelprize.nobelprizelaureates;

import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBAttribute;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBHashKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBRangeKey;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBTable;

import java.util.List;

/**
 * Created by jasondelport on 10/05/16.
 */
@DynamoDBTable(tableName = Constants.TABLE_NAME)
public class NobelPrize {
    private int year;
    private String category;
    private List<Laureate> laureates;

    @DynamoDBHashKey(attributeName = "year")
    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }


    @DynamoDBRangeKey(attributeName = "category")
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }


    @DynamoDBAttribute(attributeName = "laureates")
    public List<Laureate> getLaureates() {
        return laureates;
    }

    public void setLaureates(List<Laureate> laureates) {
        this.laureates = laureates;
    }

}