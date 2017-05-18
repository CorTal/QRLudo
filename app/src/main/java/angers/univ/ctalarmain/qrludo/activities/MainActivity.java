package angers.univ.ctalarmain.qrludo.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import angers.univ.ctalarmain.qrludo.R;
import angers.univ.ctalarmain.qrludo.utils.BasicImageDownloader;
import angers.univ.ctalarmain.qrludo.utils.OnSwipeTouchListener;

import static java.lang.String.valueOf;

public class MainActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "MyPrefsFile";

    public static final Locale LOCALE_DEFAULT = Locale.FRANCE;
    public static final float SPEEDSPEECH_DEFAULT = (float)1.2;
    private final int RES_PLACEHOLDER = R.drawable.placeholder_grey;
    private final int RES_ERROR = R.drawable.error_orange;
    static final int OPTION_REQUEST = 80;  // The request code
    private static final int CAMERA_REQUEST = 10;
    private static final int INTERNET_REQUEST = 20;
    private final int START_STATE = 30;
    private final int DETECTING_STATE = 40;
    private final int NO_QUESTION_PRINTED_STATE = 50;
    private final int QUESTION_PRINTED_STATE = 60;
    private final int REPONSE_PRINTED_STATE = 70;
    private final int MY_DATA_CHECK_CODE = 0;
    private int cameraState;
    private int questionState;
    private float speechSpeed;
    private Locale ttslanguage;
    private LinearLayout mainLayout;
    private RelativeLayout imageLayout;
    private LinearLayout contentLayout;
    private TextView text_space;
    private ImageView image_space;
    private SurfaceView cameraView;
    private BarcodeDetector detector;
    private CameraSource cameraSource;
    private TextToSpeech ttobj;
    private boolean marshmallow;
    private boolean camera;
    private boolean internet;
    private Detector.Processor<Barcode> detector_processor;
    private boolean lollipop;
    ToneGenerator toneGen;
    private Toolbar toolbar;

    private String question;
    private String reponse;

    private File questionFile;

    //private String image_url;

    //private boolean image;
    private boolean musique;


    private MediaPlayer mpintro;

    private boolean mpPaused;
    private int mpPosition;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeAttributes();
        /**/
        if(savedInstanceState != null) {
            if (savedInstanceState.containsKey("QUESTION"))
                question = savedInstanceState.getString("QUESTION");
            if (savedInstanceState.containsKey("REPONSE"))
                reponse = savedInstanceState.getString("REPONSE");
            musique = savedInstanceState.getBoolean("MUSIQUE");
            cameraState = savedInstanceState.getInt("STATE");
            switch (cameraState) {
                case DETECTING_STATE: {
                    startDetection();
                    break;
                }
            }
        }

        initializeListeners();

        if(marshmallow)
            checkPermissions();


    }

    private void SetUpTTS() {
        /*Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);*/
        ttobj = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                ttobj.setLanguage(ttslanguage);
                ttobj.setSpeechRate(speechSpeed);
                toSpeech("Application lancée, appuyez longuement pour lancer la détection.", TextToSpeech.QUEUE_FLUSH);
            }
        });


        ttobj.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (utteranceId.equals("synthetizeKey")) {
                    Log.d("UTTERANCE", "Start");
                }
            }

            @Override
            public void onDone(String utteranceId) {
                if (utteranceId.equals("synthetizeKey")) {
                    Log.d("UTTERANCE", "Done");
                    printQuestion();
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.e("UTTERANCE", "Error while trying to synthesize sample text");
            }
        });
    }


    private void initializeAttributes() {


        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        speechSpeed = settings.getFloat("speedSpeech", SPEEDSPEECH_DEFAULT);

        ttslanguage = new Locale(settings.getString("language", LOCALE_DEFAULT.getLanguage()));

        SetUpTTS();

        questionFile = new File(getCacheDir(),"question.wav");

        try {
            boolean ar = questionFile.createNewFile();
            Log.d("CREATION",valueOf(ar));
        } catch (IOException e) {
            e.printStackTrace();
        }

        mpPaused = false;

        mpPosition = 0;

        //Main Layout
        mainLayout = (LinearLayout) findViewById(R.id.main_layout);

        //Image layout
        imageLayout = (RelativeLayout) findViewById(R.id.image_layout);

        //Content Layout
        contentLayout = (LinearLayout) findViewById(R.id.content_layout);

        text_space = (TextView) findViewById(R.id.text_space);

        image_space = (ImageView) findViewById(R.id.image);

        cameraView = (SurfaceView) findViewById(R.id.camera_view);

        cameraView.setVisibility(View.INVISIBLE);

        toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        AudioManager am = (AudioManager)getSystemService(getBaseContext().AUDIO_SERVICE);

        am.setMode(AudioManager.MODE_NORMAL);

        cameraState = START_STATE;

        questionState = NO_QUESTION_PRINTED_STATE;



        marshmallow = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M;

        lollipop = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ;

        internet = false;

        camera = false;

        setUpDetector();
    }

    private void setUpDetector() {
        detector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.DATA_MATRIX | Barcode.QR_CODE)
                .build();

        if (!detector.isOperational()) {
            Log.e("DETECTOR","Could not set up detector.");
            return;
        }

        cameraSource = new CameraSource
                .Builder(this, detector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true)
                .build();


        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {

                    cameraSource.start(cameraView.getHolder());
                    cameraState = DETECTING_STATE;
                    toSpeech("Détection en cours.",TextToSpeech.QUEUE_FLUSH);
                    Log.d("CAMERA_START", "Camera started");
                } catch(SecurityException se)
                {
                    Log.e("CAMERA SECURITY", se.getMessage());
                }
                catch (Exception e)
                {
                    Log.e("CAMERA SOURCE", e.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                try {

                    if(cameraState == DETECTING_STATE) {
                        toSpeech("Détection interrompue.",TextToSpeech.QUEUE_ADD);
                        cameraState = START_STATE;
                    }
                    cameraSource.stop();
                } catch (Exception e)
                {
                    Log.e("CAMERA SOURCE", e.getMessage());
                }
            }
        });



        detector_processor = new Detector.Processor<Barcode>() {
            String lastBarcode;
            int lastBarcodesSize;
            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                    Log.d("BARCODE", valueOf(barcodes.size()));
                    if(barcodes.size() == 1)
                    {
                        if(!barcodes.valueAt(0).rawValue.equals(lastBarcode))
                        {
                            resetQuestion();
                            toneGen.startTone(ToneGenerator.TONE_CDMA_PIP,150);
                            lastBarcode = barcodes.valueAt(0).rawValue;
                            parseJSON(lastBarcode);
                        }
                    }else
                    {
                        if(lastBarcodesSize != barcodes.size()) {
                            Log.d("BARCODE", "Là");
                            for (int i = 0; i < barcodes.size(); i++) {
                                toneGen.startTone(ToneGenerator.TONE_CDMA_PIP, 150);
                                synchronized (this)
                                {
                                    try {
                                        wait(750);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }

                            }
                            for (int i = 0; i < barcodes.size(); i++) {
                                parseJSON(barcodes.valueAt(i).rawValue);
                                while(ttobj.isSpeaking());

                            }
                            lastBarcodesSize = barcodes.size();
                        }
                    }
                    //state = CODE_DETECTED_STATE;
                    //stopDetection();
                   /* for(int i = 0 ; i < barcodes.size(); i++)
                    {
                        toneGen.startTone(ToneGenerator.TONE_CDMA_PIP,150);
                       // parseJSON(barcodes.valueAt(i).rawValue);

                    }*/
                   /* Log.d("COUCOU","--------------------STOP--------------");
                    boolean image = false, musique = false;
                    for (int i = 0; i < barcodes.size(); i++) {
                        JSONObject object;
                        try {
                            object = new JSONObject(barcodes.valueAt(i).rawValue);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                       /switch (barcode.valueFormat) {
                            case Barcode.URL:
                                Log.d("COUCOU","--------------------URL--------------");
                                if (internet) {
                                    URLConnection connection = null;
                                    boolean image = false;
                                    try {
                                        connection = new URL(barcode.rawValue).openConnection();
                                        String contentType = connection.getHeaderField("Content-Type");
                                        image = contentType.startsWith("image/");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    if(image)
                                    {
                                        Intent intent = new Intent(getBaseContext(), ImageDialog.class);
                                        intent.putExtra("EXTRA_HTML", barcode.rawValue);
                                        startActivity(intent);
                                    }
                                } else {
                                    printToast("Can't open link, internet use not permitted.");
                                }
                                break;
                            case Barcode.TEXT:
                                Log.d("COUCOU","--------------------TEXT--------------");
                                toSpeech(barcodes.valueAt(i).rawValue);
                                break;
                        }


                    }*/


                }
            }
        };


        detector.setProcessor(detector_processor);
    }

    private void resetQuestion() {
        question = "";
        reponse = "";

    }

    private void initializeListeners() {
    /*    mainLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @OverrideQuestion
            public boolean onLongClick(View v) {
                Log.d("LISTENER","COUCOU C'EST LE LONG CLICK LISTENER");
                switch(state)
                {
                    case START_STATE:
                    {
                        startDetection();
                        return true;
                    }
                    case DETECTING_STATE:
                    {
                        stopDetection();
                        return true;
                    }
                    case CODE_DETECTED_STATE:
                    {
                        printQuestion();
                        return true;
                    }
                    case QUESTION_PRINTED_STATE:
                        toSpeech(question,TextToSpeech.QUEUE_ADD);
                        return true;
                }
                return false;
            }
        });*/
        mainLayout.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeTop() {
                if(questionState == QUESTION_PRINTED_STATE)
                {
                    mpintro.seekTo(0);
                    if(ttobj.isSpeaking()) ttobj.stop();
                    mpintro.start();

                }
            }
            public void onSwipeRight() {
                if(questionState == REPONSE_PRINTED_STATE && question != "") {
                    printQuestion();
                }
            }
            public void onSwipeLeft() {
                if(questionState == QUESTION_PRINTED_STATE && reponse != "") {
                    printReponse();
                }
            }
            public void onSwipeBottom() {
                if(ttobj.isSpeaking())
                {
                    ttobj.stop();
                }
                if(mpintro.isPlaying()){
                    mpintro.pause();
                    mpintro.seekTo(0);
                }
            }

            public void onLongClick(){
                if(cameraState == START_STATE)
                {
                    startDetection();
                }else if( cameraState == DETECTING_STATE){
                    stopDetection();
                }
            }

            @Override
            public void onSingleTap() {

                switch (questionState)
                {
                    case QUESTION_PRINTED_STATE:
                        if(mpintro.isPlaying())
                        {
                            mpintro.pause();
                            mpPosition = mpintro.getCurrentPosition();
                            mpPaused = true;
                        }else if(mpPaused == true){
                            mpintro.seekTo(mpPosition);
                            if(ttobj.isSpeaking()) ttobj.stop();
                            mpintro.start();
                            mpPaused = false;
                        }
                }
            }
        });

    }

    private void checkPermissions() {
        if (marshmallow) {
            Log.d("PERMISSION_CHECK","---------Marshallow----------");

            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION_CHECK","---------CheckPermission----------");
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    Log.d("PERMISSION_CHECK","---------Explanation----------");
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {
                    Log.d("PERMISSION_CHECK","---------NoExplanation----------");
                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_REQUEST);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }else{
                camera = true;
            }
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED)
            {
                Log.d("PERMISSION_CHECK","---------CheckPermission----------");
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.INTERNET)) {
                    Log.d("PERMISSION_CHECK","---------Explanation----------");
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {
                    Log.d("PERMISSION_CHECK","---------NoExplanation----------");
                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.INTERNET},
                            INTERNET_REQUEST);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }else
                internet = true;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            saveState();
            Intent pickOptionIntent = new Intent(getBaseContext(), OptionActivity.class);
            startActivityForResult(pickOptionIntent, OPTION_REQUEST);
            /*Intent intent = new Intent(getBaseContext(), OptionActivity.class);
            startActivity(intent);*/
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == OPTION_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

                speechSpeed = settings.getFloat("speechSpeed", SPEEDSPEECH_DEFAULT);

                ttslanguage = new Locale(settings.getString("language",LOCALE_DEFAULT.getLanguage()));
                int i = ttobj.setSpeechRate(speechSpeed);
                Log.d("SETSR",valueOf(i));
                if(ttobj.isLanguageAvailable(ttslanguage) == TextToSpeech.LANG_AVAILABLE){
                    ttobj.setLanguage(ttslanguage);
                }

                Log.d("language",ttslanguage.getDisplayCountry());
                Log.d("speedSpeech",valueOf(speechSpeed));
                // Do something with the contact here (bigger example below)
            }
        }
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance

            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }
    }

    private void saveState() {

    }



    private void Wait(int i) {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                try {
                    wait(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void printQuestion() {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                text_space.setText(question);
                /*if(image)
                {
                    Bitmap bmp = BitmapFactory.decodeFile(image_url);
                    image_space.setImageBitmap(bmp);
                    image_space.setVisibility(View.VISIBLE);
                }else
                {
                    image_space.setVisibility(View.INVISIBLE);
                }*/
                text_space.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.VISIBLE);
                if(questionFile.exists()) {
                    mpintro = MediaPlayer.create(activity, Uri.parse(Uri.encode(questionFile.getAbsolutePath())));
                    if (mpintro != null) {
                        mpintro.setLooping(false);
                        if (ttobj.isSpeaking()) ttobj.stop();
                        mpintro.start();
                    } else {
                        toSpeech("Un problème numéro 1 est survenu dans la lecture du code.", TextToSpeech.QUEUE_FLUSH);
                    }
                }else
                {
                    toSpeech("Un problème numéro 2 est survenu dans la lecture du code.", TextToSpeech.QUEUE_ADD);
                }
                questionState = QUESTION_PRINTED_STATE;
            }
        });

    }

    private void printReponse() {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                text_space.setText(reponse);
                //image_space.setVisibility(View.INVISIBLE);
                text_space.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.VISIBLE);
                toSpeech(reponse,TextToSpeech.QUEUE_FLUSH);
                questionState = REPONSE_PRINTED_STATE;
            }
        });

    }


    private void stopDetection() {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                cameraView.setVisibility(View.INVISIBLE);
            }
        });
    }

    private void startDetection() {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                cameraView.setVisibility(View.VISIBLE);
            }
        });
    }

    private void parseJSON(String rawValue) {
        JSONObject object;
        try {
            object = new JSONObject(rawValue);
            question = object.getString("question");
            reponse = object.getString("reponse");
            //image = object.getBoolean("picture");
            musique = object.getBoolean("music");

            /*if(image){
                JSONObject image_json = object.getJSONObject("Picture");
                String image_name = image_json.getString("name");
                Uri uri = Uri.parse(image_json.getString("url"));
                String type = uri.getScheme();
                image_url = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "QRCodeForGames" + File.separator +  image_name + ".jpeg";
                if(!(new File(image_url).exists())) {
                    if (type.equals("http") || type.equals("https")) {
                        if (internet) {
                            String url = uri.toString();
                            URLConnection connection = null;
                            boolean html_image = false;
                            try {
                                connection = new URL(url).openConnection();
                                String contentType = connection.getHeaderField("Content-Type");
                                html_image = contentType.startsWith("image/");
                                Log.d("CONTENT", contentType);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (html_image) {
                                if(write) {
                                    downloadPicture(image_name, url);
                                }else{
                                    toSpeech("L'écriture sur le stockage externe n'a pas été accordée.",TextToSpeech.QUEUE_ADD);
                                }
                            }else{
                                toSpeech("L'URL internet n'est pas une image.",TextToSpeech.QUEUE_ADD);
                            }
                        } else {
                            toSpeech("Erreur : Une ressource internet est demandée, mais non permis.", TextToSpeech.QUEUE_ADD);
                        }

                    } else if(type.equals("file") ){
                        File f = new File(uri.getSchemeSpecificPart());
                        if(f.exists()){
                            image_url = uri.getSchemeSpecificPart();
                        }else
                        {
                            toSpeech("L'url indiquée pour le fichier est erronnée.",TextToSpeech.QUEUE_ADD);
                        }
                    }
                }else{
                    printQuestion();
                }

            }*/
            if(musique)
            {
                toSpeech("Il y a une musique.",TextToSpeech.QUEUE_ADD);
            }

        } catch (JSONException e) {
            question = rawValue;

            //image = false;
            musique = false;
            toFile(question);
        }



    }

    private void downloadPicture(String image_name, String url) {
        final String i_name = image_name;
        final BasicImageDownloader downloader = new BasicImageDownloader(new BasicImageDownloader.OnImageLoaderListener() {
            @Override
            public void onError(BasicImageDownloader.ImageError error) {
                Toast.makeText(MainActivity.this, "Error code " + error.getErrorCode() + ": " +
                        error.getMessage(), Toast.LENGTH_LONG).show();
                error.printStackTrace();
                image_space.setImageResource(RES_ERROR);
            }

            @Override
            public void onProgressChange(int percent) {

            }

            @Override
            public void onComplete(Bitmap result) {
                        /* save the image - I'm gonna use JPEG */
                final Bitmap.CompressFormat mFormat = Bitmap.CompressFormat.JPEG;
                        /* don't forget to include the extension into the file name */
                final File myImageFile = new File(getBaseContext().getFilesDir().getAbsolutePath() +
                        File.separator + "images" + File.separator +  i_name + "." + mFormat.name().toLowerCase());
                Log.d("FILE", myImageFile.getAbsolutePath());
                BasicImageDownloader.writeToDisk(myImageFile, result, new BasicImageDownloader.OnBitmapSaveListener() {
                    @Override
                    public void onBitmapSaved() {
                        Toast.makeText(MainActivity.this, "Image saved as: " + myImageFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        printQuestion();
                    }

                    @Override
                    public void onBitmapSaveError(BasicImageDownloader.ImageError error) {
                        Toast.makeText(MainActivity.this, "Error code " + error.getErrorCode() + ": " +
                                error.getMessage(), Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }


                }, mFormat, false);

            }
        });
        downloader.download(url, true);

    }

    private void printPicture(String image_name, String url, boolean b) {
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d("PERMISSION_RESULT","---------START----------");
        switch (requestCode) {
            case CAMERA_REQUEST: {
                Log.d("PERMISSION_RESULT","---------1----------");

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSION_RESULT","---------GRANTED----------");
                    camera = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    Log.d("PERMISSION_RESULT","---------DENIED----------");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to use the camera", Toast.LENGTH_SHORT).show();
                }
            }
            case INTERNET_REQUEST: {
                Log.d("PERMISSION_RESULT", "---------INTERNET_REQUEST----------");

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSION_RESULT", "---------GRANTED----------");
                    internet = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    Log.d("PERMISSION_RESULT", "---------DENIED----------");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to use internet", Toast.LENGTH_SHORT).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void printToast(final String str) {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, str, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void toSpeech20(String str, int queue)
    {
        ttobj.speak(str, queue, null);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void toSpeech21(String str, int queue)
    {
        String utteranceId = this.hashCode() + "";
        ttobj.speak(str, queue, null, utteranceId);
    }

    private void toSpeech(String str, int queue) {
        if (lollipop) {
            toSpeech21(str, queue);
        } else {
            toSpeech20(str,queue);
        }
    }

    private void toFile(String str){
        if(lollipop)
        {
            toFile21(str);
        }else{
            toFile20(str);
        }
    }

    @SuppressWarnings("deprecation")
    private void toFile20(String str) {
        HashMap<String, String> utteranceMap = new HashMap<String,String>();
        utteranceMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "synthetizeKey");
        ttobj.synthesizeToFile(str,utteranceMap,questionFile.getAbsolutePath());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void toFile21(String str) {
        Bundle utteranceBundle = new Bundle();
        utteranceBundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "synthetizeKey");
        ttobj.synthesizeToFile(str,utteranceBundle,questionFile,"synthetizeKey");
    }

}


