package com.jasondelport.nobelprize.nobelprizelaureates;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.InitializationException;
import com.amazonaws.mobileconnectors.amazonmobileanalytics.MobileAnalyticsManager;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUser;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserAttributes;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserCodeDeliveryDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserPool;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.CognitoUserSession;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.AuthenticationDetails;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.continuations.MultiFactorAuthenticationContinuation;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.AuthenticationHandler;
import com.amazonaws.mobileconnectors.cognitoidentityprovider.handlers.SignUpHandler;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.SendMessageResult;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    private static MobileAnalyticsManager analytics;
    private TextView textView;
    AuthenticationHandler authHandler = new AuthenticationHandler() {

        @Override
        public void onSuccess(CognitoUserSession userSession) {
            Timber.d("Successful user authentication -> %s", userSession.toString());
            textView.setText("success -> " + userSession.toString());
        }

        @Override
        public void getAuthenticationDetails(AuthenticationContinuation authenticationContinuation, String UserId) {
            Timber.d("user details -> %s", authenticationContinuation.getParameters());
            textView.setText("details -> " + authenticationContinuation.getParameters());
        }

        @Override
        public void getMFACode(MultiFactorAuthenticationContinuation continuation) {

        }

        @Override
        public void onFailure(Exception exception) {
            Timber.e(exception, "Error -> %s", exception.getMessage());
            textView.setText(exception.getMessage());
        }
    };
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
    private AmazonS3 s3 = null;
    private CognitoUserPool userPool = null;
    private TransferUtility transferUtility = null;
    private String userId = null;
    SignUpHandler signUpHandler = new SignUpHandler() {
        @Override
        public void onSuccess(CognitoUser user, boolean signUpConfirmationState, CognitoUserCodeDeliveryDetails cognitoUserCodeDeliveryDetails) {
            userId = user.getUserId();

            Timber.d("Successful user registration -> %s", user.getUserId());
            textView.setText("success -> " + user.getUserId());
        }

        @Override
        public void onFailure(Exception exception) {
            Timber.e(exception, "Error -> %s", exception.getMessage());
            textView.setText(exception.getMessage());
        }
    };
    private Button button3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CognitoCachingCredentialsProvider credentials = new CognitoCachingCredentialsProvider(
                this,
                Constants.IDENTITY_POOL_ID,
                Regions.US_EAST_1);

        try {
            userPool = new CognitoUserPool(this, Constants.USER_POOL_ID,
                    Constants.USER_POOL_CLIENT_ID, Constants.USER_POOL_SECRET, new ClientConfiguration());
        } catch (Exception e) {
            Timber.e("Error -> %s", e.getMessage());
        }

        ddbClient = new AmazonDynamoDBClient(credentials);
        ddbClient.setRegion(Region.getRegion(Regions.US_WEST_2));

        sqsClient = new AmazonSQSClient(credentials);
        sqsClient.setRegion(Region.getRegion(Regions.US_WEST_2));

        s3 = new AmazonS3Client(credentials);
        s3.setRegion(Region.getRegion(Regions.US_WEST_2));

        transferUtility = new TransferUtility(s3, this);

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


        button3 = (Button) findViewById(R.id.button3);
        if (button3 != null) {
            button3.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectPhotoFromGallery();
                }
            });
        }


        Button button4 = (Button) findViewById(R.id.button4);
        if (button4 != null) {
            button4.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    CognitoUserAttributes atts = new CognitoUserAttributes();
                    atts.addAttribute("email", "jason.delport@gmail.com");
                    userPool.signUpInBackground("testuser", "Hello@1000", atts, null, signUpHandler);
                }
            });
        }

        Button button5 = (Button) findViewById(R.id.button5);
        if (button5 != null) {
            button5.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        AuthenticationDetails authDetails = new AuthenticationDetails("testuser", "Hello@1000", null);
                        CognitoUser user = userPool.getUser(userId);
                        user.authenticateUserInBackground(authDetails, authHandler);
                    } catch (Exception e) {
                        Timber.e(e,"Error -> %s", e.getMessage());
                    }
                }
            });
        }

        RxPermissions.getInstance(this)
                .request(Manifest.permission.READ_EXTERNAL_STORAGE)
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean granted) {

                    }
                });
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

    private void selectPhotoFromGallery() {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");

        Intent chooser = Intent.createChooser(intent, "Select Image");
        startActivityForResult(chooser, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == RESULT_OK && data != null && data.getData() != null) {

            textView.setText("about to start uploading image");
            button3.setEnabled(false);
            runTask(data.getData());

        }
    }

    private void runTask(Uri uri) {
        Timber.d("uri path -> %s", uri.getPath());
        File file = FileUtils.getFile(MainActivity.this, uri);
        new UploadImageTask().execute(file);
    }

    private class UploadImageTask extends AsyncTask<File, Void, String> {
        @Override
        protected String doInBackground(File... params) {
            Timber.d("Uploading image");
            Timber.d("Image name -> %s", params[0].getName());
            try {

                TransferObserver transferObserver = transferUtility.upload(
                        Constants.BUCKET_NAME,
                        params[0].getName(),
                        params[0]
                );
                transferObserver.setTransferListener(new TransferListener() {

                    @Override
                    public void onStateChanged(int id, TransferState state) {
                        Timber.d("state change -> %s", state.name());
                        if (state == TransferState.COMPLETED) {
                            textView.setText("image upload completed");
                            button3.setEnabled(true);
                        }
                    }

                    @Override
                    public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                        try {
                            int percentage = (int) (bytesCurrent / bytesTotal * 100);
                            Timber.d("percent -> %d", percentage);
                            textView.setText("percent complete -> " + percentage);
                        } catch (Exception e) {
                            Timber.e("error -> %s", e.getMessage());
                        }
                    }

                    @Override
                    public void onError(int id, Exception ex) {
                        Timber.e("Error -> %s", ex.getMessage());
                    }

                });

                return "";
            } catch (Exception e) {
                Timber.e("Error -> %s", e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Timber.d("Result -> %s", result);
            }

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
