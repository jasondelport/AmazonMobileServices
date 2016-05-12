package com.jasondelport.nobelprize.nobelprizelaureates;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageResult;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static MobileAnalyticsManager analytics;
    private TextView textView;
    private RecyclerView recyclerView;
    private List<NobelPrize> mNobelPrizes = new ArrayList<>();
    private RecyclerViewAdapter mAdapter;
    Subscriber<NobelPrize> subscriber = new Subscriber<NobelPrize>() {
        @Override
        public void onNext(NobelPrize prize) {
            Timber.d("RXJava OnNext");
            mAdapter.addNobelPrize(prize);
        }

        @Override
        public void onCompleted() {
            Timber.d("RXJava onCompleted");
        }

        @Override
        public void onError(Throwable e) {
            Timber.e("RXJava Error -> %s", e.getMessage());
        }
    };
    private Subscription subscription = Subscriptions.empty();
    private AmazonDynamoDBClient ddbClient = null;
    private AmazonSQS sqsClient = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CognitoCachingCredentialsProvider credentials = new CognitoCachingCredentialsProvider(
                this,
                Constants.IDENTITY_POOL_ID,
                Regions.US_EAST_1);

        ddbClient = new AmazonDynamoDBClient(credentials);
        ddbClient.setRegion(Region.getRegion(Regions.US_WEST_2));

        sqsClient = new AmazonSQSClient(credentials);
        sqsClient.setRegion(Region.getRegion(Regions.US_WEST_2));

        try {
            analytics = MobileAnalyticsManager.getOrCreateInstance(
                    this.getApplicationContext(),
                    Constants.APP_ID,
                    Constants.IDENTITY_POOL_ID);
        } catch (InitializationException ex) {
            Timber.e("Failed to initialize Amazon Mobile Analytics -> %s", ex.getMessage());
        }

        setContentView(R.layout.activity_main);

        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        mAdapter = new RecyclerViewAdapter(this, mNobelPrizes);
        recyclerView.setAdapter(mAdapter);

        textView = (TextView) findViewById(R.id.textView);

        Button button1 = (Button) findViewById(R.id.button1);
        if (button1 != null) {
            button1.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new SendMessageTask().execute();
                }
            });
        }

        Button button2 = (Button) findViewById(R.id.button2);
        if (button2 != null) {
            button2.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Observable<NobelPrize> observable = DataGrabber.getInstance(ddbClient).getObservableNobelPrizes();
                    subscription = observable.subscribeOn(Schedulers.newThread())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(subscriber);
                }
            });
        }

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        subscription.unsubscribe();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (analytics != null) {
            analytics.getSessionClient().resumeSession();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (analytics != null) {
            analytics.getSessionClient().pauseSession();
            analytics.getEventClient().submitEvents();
        }
    }

    private class SendMessageTask extends AsyncTask<Void, Void, SendMessageResult> {
        @Override
        protected SendMessageResult doInBackground(Void... params) {
            Timber.d("Running task");
            try {
                String message = "Test Message";
                return SendToQueue.getInstance(sqsClient).sendMessage(Constants.SQS_QUEUE, message);
            } catch (Exception e) {
                Timber.e("Error -> %s", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(SendMessageResult result) {
            if (result != null) {
                Timber.d("Message ID -> %s", result.getMessageId());
                textView.setText("id -> " + result.getMessageId());
            }

        }
    }

}
