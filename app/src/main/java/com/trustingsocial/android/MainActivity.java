package com.trustingsocial.android;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.trustingsocial.tvsdk.TVCapturingCallBack;
import com.trustingsocial.tvsdk.TVCardInfoResult;
import com.trustingsocial.tvsdk.TVCompareFacesResult;
import com.trustingsocial.tvsdk.TVDetectionError;
import com.trustingsocial.tvsdk.TVDetectionResult;
import com.trustingsocial.tvsdk.TVIDConfiguration;
import com.trustingsocial.tvsdk.TVLivenessResult;
import com.trustingsocial.tvsdk.TVSDKCallback;
import com.trustingsocial.tvsdk.TVSDKConfiguration;
import com.trustingsocial.tvsdk.TVSDKUtil;
import com.trustingsocial.tvsdk.TVSanityResult;
import com.trustingsocial.tvsdk.TVSelfieConfiguration;
import com.trustingsocial.tvsdk.TVTransactionData;
import com.trustingsocial.tvsdk.internal.TVCardType;
import com.trustingsocial.tvsdk.internal.TrustVisionActivity;
import com.trustingsocial.tvsdk.internal.TrustVisionSDK;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private String idImageID1;
    private String idImageID2;
    private String customerSelfieImageID;
    private String agentSelfieImageID;
    private Map<String, String> ocrResult = new LinkedHashMap<>();

    private String transactionID;

    private ImageView idFrontImage;
    private ImageView idBackImage;
    private ImageView customerSelfieImage;
    private ImageView agentSelfieImage;
    private RecyclerView ocrRecyclerView;
    private IDListViewAdapter adapter;
    private Spinner spinneCardType;
    private TVCardType selectedCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TrustVisionSDK.init(this, new TrustVisionSDK.TVInitializeListener() {
            @Override
            public void onInitSuccess() {
                List<String> cardTypes = new ArrayList<>();
                for (TVCardType cardType: TrustVisionSDK.getCardTypes()) {
                    cardTypes.add(cardType.getCardName());
                }
                ArrayAdapter<String> adapter = new ArrayAdapter(MainActivity.this, R.layout.spinner_item, cardTypes);
                spinneCardType.setAdapter(adapter);
                startTransaction();
            }

            @Override
            public void onInitError(TVDetectionError error) {
                String message = error.getDetailErrorCode() + ":" + error.getErrorDescription();
                Log.e("", message);
                showToast(message);

                ///////////////////////////////////////
                // ERROR HANDLER
                int code = error.getErrorCode();
                String detailErrorCode = error.getDetailErrorCode();
                String description = error.getErrorDescription();
                if (code == TVDetectionError.DETECTION_ERROR_TIMEOUT) {
                    // Network timeout. Poor internet connection.
                } else if (code == TVDetectionError.DETECTION_ERROR_NETWORK) {
                    // Network error. No internet connection
                } else if (code == TVDetectionError.DETECTION_ERROR_AUTHENTICATION_MISSING) {
                    // Access key or secret is missing. Make sure the SDK is initialized
                } else if (code == TVDetectionError.DETECTION_ERROR_API_ERROR) {
                    if (detailErrorCode.equalsIgnoreCase("internal_server_error")) {
                        // our server has problem when processed the request.
                    }
                }
            }
        });

        idFrontImage = findViewById(R.id.iv_id_card_front);
        idBackImage = findViewById(R.id.iv_id_card_back);
        customerSelfieImage = findViewById(R.id.iv_customer_selfie);
        agentSelfieImage = findViewById(R.id.iv_agent_selfie);
        ocrRecyclerView = findViewById(R.id.ocr_result);
        ocrRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IDListViewAdapter(ocrResult);
        ocrRecyclerView.setAdapter(adapter);

        findViewById(R.id.button_frontid).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startIDCapturing(TVSDKConfiguration.TVCardSide.FRONT);
            }
        });

        findViewById(R.id.button_back_id).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startIDCapturing(TVSDKConfiguration.TVCardSide.BACK);
            }
        });


        findViewById(R.id.button_customer_selfie).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSelfieCapturing("customer");
            }
        });

        findViewById(R.id.button_agent_selfie).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSelfieCapturing("agent");
            }
        });

        findViewById(R.id.video_kyc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MainVideoKycActivity.class);
                startActivity(intent);
            }
        });

        findViewById(R.id.button_face_id_match).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (idImageID1 != null && customerSelfieImageID != null) {
                    faceCompare(idImageID1, customerSelfieImageID);
                } else {
                    showToast("Need to capture id and selfie first");
                }
            }
        });

        findViewById(R.id.button_agent_customer_match).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (agentSelfieImageID != null && customerSelfieImageID != null) {
                    faceCompare(agentSelfieImageID, customerSelfieImageID);
                } else {
                    showToast("Need to capture customer and agent selfie first");
                }
            }
        });
        spinneCardType = findViewById(R.id.spinner_card_type);

        spinneCardType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                selectedCard = TrustVisionSDK.getCardTypes().get(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            TVDetectionResult result = (TVDetectionResult) data.getSerializableExtra(TrustVisionSDK.TV_RESULT);
            Toast.makeText(this, "Face matching: " + result.getFaceCompareResult().getMatchResult().name(), Toast.LENGTH_LONG).show();
        }
    }

    private void startIDCapturing(final TVSDKConfiguration.TVCardSide cardSide) {
        TVIDConfiguration.Builder builder = new TVIDConfiguration.Builder()
                .setCardType(selectedCard)
                .setCardSide(cardSide)
                .setEnableTiltChecking(true);

        TrustVisionActivity.startIDCapturing(this, builder.build(), new TVCapturingCallBack() {
            @Override
            public void onError(TVDetectionError error) {
                String message = error.getDetailErrorCode() + ":" + error.getErrorDescription();
                Log.d("", message);
                showToast(message);

                ///////////////////////////////////////
                // ERROR HANDLER
                int code = error.getErrorCode();
                String detailErrorCode = error.getDetailErrorCode();
                String description = error.getErrorDescription();
                if (code == TVDetectionError.DETECTION_ERROR_TIMEOUT) {
                    // Network timeout. Poor internet connection.
                } else if (code == TVDetectionError.DETECTION_ERROR_NETWORK) {
                    // Network error. No internet connection
                } else if (code == TVDetectionError.DETECTION_ERROR_AUTHENTICATION_MISSING) {
                    // Access key or secret is missing. Make sure the SDK is initialized
                } else if (code == TVDetectionError.DETECTION_ERROR_PERMISSION_MISSING) {
                    // No camera permission.
                } else if (code == TVDetectionError.DETECTION_ERROR_CAMERA_ERROR) {
                    // The camera can't be opened.
                } else if (code == TVDetectionError.DETECTION_ERROR_API_ERROR) {
                    // Backend error
                    if (detailErrorCode.equalsIgnoreCase("image_has_no_faces")) {
                        // face not detected in selfie image
                    } else if (detailErrorCode.equalsIgnoreCase("image_has_multipe_faces")) {
                        // multiple faces are detected in selfie image
                    } else if (detailErrorCode.equalsIgnoreCase("bad_quality_card_image")) {
                        // ID card is not align well (tilted) or cropped
                    } else if (detailErrorCode.equalsIgnoreCase("incorrect_card_type")) {
                        // The selected card type and uploaded card type are different
                    } else if (detailErrorCode.equalsIgnoreCase("internal_server_error")) {
                        // our server has problem when processed the request.
                    }
                }
            }

            @Override
            public void onSuccess(TVDetectionResult result) {
                Toast.makeText(MainActivity.this, "onSuccess called", Toast.LENGTH_SHORT)
                        .show();
                TVCardInfoResult cardInfoResult = result.getCardInfoResult();

                if (cardSide == TVSDKConfiguration.TVCardSide.FRONT) {
                    idImageID1 = (result.getCroppedCardFrontImageId() != null) ? result.getCroppedCardFrontImageId() : result.getIdFrontImageId();
                    downloadImageFor(idFrontImage, idImageID1);
                } else {
                    idImageID2 = (result.getCroppedCardBackImageId() != null) ? result.getCroppedCardBackImageId() : result.getIdBackImageId();
                    downloadImageFor(idBackImage, idImageID2);
                }
                List<TVCardInfoResult.Info> ocrInfo = cardInfoResult.getInfos();
                if (ocrInfo != null) {
                    for (TVCardInfoResult.Info info : ocrInfo) {
                        ocrResult.put(info.getField(), info.getValue());
                        Log.d("", info.getField() + ":" + info.getValue());
                        refreshOcr();
                    }
                }
            }
        });
    }

    private void startSelfieCapturing(final String type) {
        TVSelfieConfiguration.Builder builder = new TVSelfieConfiguration.Builder()
                .setCameraOption(TVSDKConfiguration.TVCameraOption.BOTH)
                .setEnableSound(false)
                .setEnableSanityCheck(true)
                .setEnableVerticalChecking(true)
                .setLivenessMode(TVSDKConfiguration.TVLivenessMode.PASSIVE);

        TrustVisionActivity.startSelfieCapturing(this, builder.build(), new TVCapturingCallBack() {
            @Override
            public void onError(TVDetectionError error) {
                String message = error.getDetailErrorCode() + ":" + error.getErrorDescription();
                Log.d("", message);

                ///////////////////////////////////////
                // ERROR HANDLER
                int code = error.getErrorCode();
                String detailErrorCode = error.getDetailErrorCode();
                String description = error.getErrorDescription();
                if (code == TVDetectionError.DETECTION_ERROR_TIMEOUT) {
                    // Network timeout. Poor internet connection.
                } else if (code == TVDetectionError.DETECTION_ERROR_NETWORK) {
                    // Network error. No internet connection
                } else if (code == TVDetectionError.DETECTION_ERROR_AUTHENTICATION_MISSING) {
                    // Access key or secret is missing. Make sure the SDK is initialized
                } else if (code == TVDetectionError.DETECTION_ERROR_PERMISSION_MISSING) {
                    // No camera permission.
                } else if (code == TVDetectionError.DETECTION_ERROR_CAMERA_ERROR) {
                    // The camera can't be opened.
                } else if (code == TVDetectionError.DETECTION_ERROR_API_ERROR) {
                    // Backend error
                    if (detailErrorCode.equalsIgnoreCase("image_has_no_faces")) {
                        // face not detected in selfie image
                    } else if (detailErrorCode.equalsIgnoreCase("image_has_multipe_faces")) {
                        // multiple faces are detected in selfie image
                    } else if (detailErrorCode.equalsIgnoreCase("internal_server_error")) {
                        // our server has problem when processed the request.
                    }
                }
            }

            @Override
            public void onSuccess(TVDetectionResult result) {
                TVLivenessResult livenessResult = result.getLivenessResult();
                TVSanityResult selfieSanityResult = result.getSelfieSanityResult();

                if ("customer".equals(type)) {
                    customerSelfieImageID = result.getSelfieImageId();
                    showImage(customerSelfieImage, result.getSelfieImageUrl());
                } else {
                    agentSelfieImageID = result.getSelfieImageId();
                    showImage(agentSelfieImage, result.getSelfieImageUrl());
                }

                // verify liveness
                StringBuilder stringBuilder = new StringBuilder();
                if (livenessResult.isLive()) {
                    stringBuilder.append("Liveness verification: Passed");
                } else {
                    stringBuilder.append("Liveness verification: Failed");
                }
                Log.d("", "Liveness score: " + livenessResult.getScore());

                //verify sanity
                if (selfieSanityResult.isGood()) {
                    stringBuilder.append("\n");
                    stringBuilder.append("Selfie image quality: Good");
                } else {
                    stringBuilder.append("\n");
                    stringBuilder.append("Selfie image quality: " + selfieSanityResult.getError()); // ex: Not white background
                }
                Log.d("", "Sanity score: " + selfieSanityResult.getScore());
                showToast(stringBuilder.toString());
            }
        });
    }

    private void downloadImageFor(final ImageView imageView, String imageID) {
        TrustVisionSDK.downloadImage(imageID, new TVSDKCallback<Bitmap>() {
            @Override
            public void onSuccess(final Bitmap bitmap) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }

            @Override
            public void onError(TVDetectionError error) {
                String message = error.getDetailErrorCode() + ":" + error.getErrorDescription();
                Log.d("", message);
                showToast(message);
            }
        });
    }

    private void showImage(final ImageView imageView, String imageUrl) {
        final Bitmap bitmap = TVSDKUtil.loadImageFromStorage(imageUrl);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imageView.setImageBitmap(bitmap);
            }
        });
    }


    private void refreshOcr() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyDataSetChanged();
            }
        });
    }

    private void faceCompare(String imageId1, String imageId2) {

        TrustVisionSDK.faceMatching(imageId1, imageId2, new TVCapturingCallBack() {
            @Override
            public void onError(TVDetectionError error) {
                String message = error.getDetailErrorCode() + ":" + error.getErrorDescription();
                Log.d("", message);
                showToast(message);

                ///////////////////////////////////////
                // ERROR HANDLER
                int code = error.getErrorCode();
                String detailErrorCode = error.getDetailErrorCode();
                String description = error.getErrorDescription();
                if (code == TVDetectionError.DETECTION_ERROR_TIMEOUT) {
                    // Network timeout. Poor internet connection.
                } else if (code == TVDetectionError.DETECTION_ERROR_NETWORK) {
                    // Network error. No internet connection
                } else if (code == TVDetectionError.DETECTION_ERROR_AUTHENTICATION_MISSING) {
                    // Access key or secret is missing. Make sure the SDK is initialized
                } else if (code == TVDetectionError.DETECTION_ERROR_API_ERROR) {
                    // Backend error
                    if (detailErrorCode.equalsIgnoreCase("image_has_no_faces")) {
                        // face not detected in selfie image or id image
                    } else if (detailErrorCode.equalsIgnoreCase("image_has_multipe_faces")) {
                        // multiple faces are detected in selfie image or id image
                    } else if (detailErrorCode.equalsIgnoreCase("internal_server_error")) {
                        // our server has problem when processed the request.
                    }
                }
            }

            @Override
            public void onSuccess(TVDetectionResult result) {
                TVCompareFacesResult faceMatchingResult = result.getFaceCompareResult();

                // check response
                if (faceMatchingResult.getMatchResult() == TVCompareFacesResult.MatchResult.MATCHED) {
                    Log.d("", "Face compare: Matched");
                    showToast("Face compare: Matched");
                } else if (faceMatchingResult.getMatchResult() == TVCompareFacesResult.MatchResult.UNSURE) {
                    Log.d("", "Liveness verification: Unsure");
                    showToast("Face compare: Unsure");
                } else {
                    Log.d("", "Liveness verification: Not matched");
                    showToast("Face compare: Not matched");
                }
                Log.d("", "Liveness score: " + faceMatchingResult.getScore());
            }
        });
    }

    TVSDKCallback callback = new TVSDKCallback<TVTransactionData>() {

        @Override
        public void onSuccess(TVTransactionData transactionID) {
            MainActivity.this.transactionID = transactionID.getTransactionId();
            showToast("transactionId: " + MainActivity.this.transactionID);
        }

        @Override
        public void onError(TVDetectionError error) {
            String message = error.getDetailErrorCode() + ":" + error.getErrorDescription();
            Log.d("", message);
            showToast(message);

            ///////////////////////////////////////
            // ERROR HANDLER
            int code = error.getErrorCode();
            String detailErrorCode = error.getDetailErrorCode();
            String description = error.getErrorDescription();
            if (code == TVDetectionError.DETECTION_ERROR_TIMEOUT) {
                // Network timeout. Poor internet connection.
            } else if (code == TVDetectionError.DETECTION_ERROR_NETWORK) {
                // Network error. No internet connection
            } else if (code == TVDetectionError.DETECTION_ERROR_AUTHENTICATION_MISSING) {
                // Access key or secret is missing. Make sure the SDK is initialized
            } else if (code == TVDetectionError.DETECTION_ERROR_API_ERROR) {
                if (detailErrorCode.equalsIgnoreCase("internal_server_error")) {
                    // our server has problem when processed the request.
                }
            }
        }
    };

    private void startTransaction() {
        TrustVisionSDK.startTransaction(null, callback);
    }

    private void showToast(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });

    }
}
