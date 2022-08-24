package com.zobaer53.tomato_leaf_disease_detection_ml_androidapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.cardview.widget.CardView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import com.zobaer53.plantapp.R;
import com.zobaer53.plantapp.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

    public class MainActivity extends AppCompatActivity {


        private AppCompatButton select, predict,solution;
        private TextView textView;
        private ImageView imageView;
        private Bitmap img;
        int imageSize=224;

        @SuppressLint("ClickableViewAccessibility")
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            select = findViewById(R.id.select);
            predict = findViewById(R.id.predict);
            textView = findViewById(R.id.textId);
            imageView = findViewById(R.id.imageview);
            solution = findViewById(R.id.solution);

            imageView.setCropToPadding(true);
            select.setOnTouchListener(new View.OnTouchListener() {

                @SuppressLint("ClickableViewAccessibility")
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            v.getBackground().setColorFilter(Color.rgb(66, 133, 91), PorterDuff.Mode.SRC_ATOP);
                            v.invalidate();
                            textView.setText("");
                            solution.setVisibility(View.INVISIBLE);
                            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                            intent.setType("image/*");
                            startActivityForResult(intent, 10);
                            break;
                        }

                        case MotionEvent.ACTION_UP: {
                            v.getBackground().clearColorFilter();
                            v.invalidate();
                            break;
                        }
                    }
                    return false;
                }
            });


            predict.setOnTouchListener(new View.OnTouchListener() {
                @SuppressLint("ClickableViewAccessibility")
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            v.getBackground().setColorFilter(Color.rgb(66, 133, 91), PorterDuff.Mode.SRC_ATOP);
                            v.invalidate();
                            img = Bitmap.createScaledBitmap(img, 224, 224, true);

                            try {

                                Model model = Model.newInstance(getApplicationContext());

                                TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.FLOAT32);

                                TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
                                tensorImage.load(img);
                                ByteBuffer byteBuffer = tensorImage.getBuffer();

                                inputFeature0.loadBuffer(byteBuffer);

                                // Runs model inference and gets result.
                                Model.Outputs outputs = model.process(inputFeature0);
                                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();
                                float[] confidences=outputFeature0.getFloatArray();
                                int maxPos=0;
                                float maxConfidence=0;
                                for(int i=0;i<confidences.length;i++){
                                    if(confidences[i]>maxConfidence){
                                        maxConfidence=confidences[i];
                                        maxPos=i;
                                    }
                                    Log.i("Tag", String.valueOf(confidences[i]));
                                }
                                // Releases model resources if no longer used.
                                model.close();


                                 Log.i("Tag","max value= "+maxConfidence+"Max pos= "+maxPos);
                                switch(maxPos){
                                    case 0:
                                        textView.setText("Healthy leaf");
                                        break;
                                    case 1:
                                        textView.setText("Tomato___Bacterial_spot");
                                        solution.setVisibility(View.VISIBLE);
                                        break;

                                    case 2:
                                        textView.setText("Tomato___Early_blight");
                                        solution.setVisibility(View.VISIBLE);
                                        break;
                                    case 3:
                                        textView.setText("Tomato___Late_blight");
                                        solution.setVisibility(View.VISIBLE);
                                        break;
                                    case 4:
                                        textView.setText("Tomato___Leaf_Mold");
                                        solution.setVisibility(View.VISIBLE);
                                        break;
                                    case 5:
                                        textView.setText("Tomato___Septoria_leaf_spot");
                                        solution.setVisibility(View.VISIBLE);
                                        break;
                                    case 6:
                                        textView.setText("Tomato___Spider_mites Two-spotted_spider_mite");
                                        solution.setVisibility(View.VISIBLE);
                                        break;
                                    case 7:
                                        textView.setText("Tomato___Target_Spot");
                                        solution.setVisibility(View.VISIBLE);
                                        break;
                                    case 8:
                                        textView.setText("Tomato___Tomato_mosaic_virus");
                                        solution.setVisibility(View.VISIBLE);
                                        break;
                                    case 9:
                                        textView.setText("Tomato___Tomato_Yellow_Leaf_Curl_Virus");
                                        solution.setVisibility(View.VISIBLE);
                                        break;


                                }


                            } catch (IOException e) {
                                // TODO Handle the exception

                            }

                            predict.setVisibility(View.INVISIBLE);

                            break;
                        }
                        case MotionEvent.ACTION_UP: {
                            v.getBackground().clearColorFilter();
                            v.invalidate();
                            break;
                        }
                    }
                    return false;
                }
            });

            solution.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    startActivity(new Intent(MainActivity.this,WebView.class));
                }
            });
        }



        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            if(requestCode == 10)
            {
                imageView.setImageURI(data.getData());

                Uri uri = data.getData();
                try {
                    img = MediaStore.Images.Media.getBitmap(this.getContentResolver(), uri);
                    predict.setVisibility(View.VISIBLE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
