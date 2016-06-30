package com.daose.secretcalculator;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class VoiceActivity extends AppCompatActivity implements KnurldListener {

    private AudioRecord recorder;
    private Button recordButton;
    private ProgressDialog progressDialog;
    private ProgressDialog preDialog;
    private TextView txtProgress;
    private String[] vocab = {"Diamond", "Circle", "Oval"};
    private Handler handler;
    private int count = 0;
    private boolean isRecording = false;
    private boolean isNewUser = false;
    private Thread recordingThread;
    private int bufferSize;
    private JSONObject wordIntervals;
    private String storageName;

    public static final int RECORDER_BPP = 16;
    public static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    public static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    public static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";

    public static final int RECORDER_SAMPLERATE = 44100;
    public static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    public static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private static final String LOG_TAG = "VoiceActivity";
    private Knurld knurldSdk;
    private StorageReference wavRef;
    private int numOfWords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_voice);
        isNewUser = getIntent().getBooleanExtra("isNewUser", true);
        if(isNewUser){
            numOfWords = 9;
        } else {
            numOfWords = 3;
        }
        setupView();
    }

    private void setupView() {
        txtProgress = (TextView) findViewById(R.id.txtProgress);
        handler = new Handler();
        progressDialog = new ProgressDialog(this);
        //TODO: change it up
        progressDialog.setTitle("Analyzing Voice");
        progressDialog.setMessage("Please wait, this will take awhile");
        progressDialog.setCancelable(false);

        preDialog = new ProgressDialog(this);
        preDialog.setTitle("Loading...");
        preDialog.setCancelable(false);

        if(!isNewUser) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    preDialog.show();
                    Knurld sdk = new Knurld(VoiceActivity.this);
                    sdk.setKnurldListener(VoiceActivity.this);
                    sdk.getVocab();
                }
            });
        }
        recordButton = (Button) findViewById(R.id.recordButton);
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startRecording();
            }
        });
    }

    @Override
    public void vocabReceived(String[] vocab){
        preDialog.dismiss();
        this.vocab = vocab;
    }

    private void startRecording() {
        isRecording = true;
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE, AudioFormat.CHANNEL_IN_MONO, RECORDER_AUDIO_ENCODING);
        recorder = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE,
                RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING,
                bufferSize);
        if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
            Log.d(LOG_TAG, "startRecording");
            recorder.startRecording();
        }
        showProgress();

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void writeAudioDataToFile() {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        int read = 0;

        if (null != os) {
            while (isRecording) {
                read = recorder.read(data, 0, bufferSize);
                if (read != AudioRecord.ERROR_INVALID_OPERATION) {
                    try {
                        os.write(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String getTempFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDER_TEMP_FILE);

        if (tempFile.exists()) {
            tempFile.delete();
        }

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void showProgress() {
        recordButton.setVisibility(View.INVISIBLE);
        txtProgress.setVisibility(View.VISIBLE);

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                //TODO: variable vocab length
                while (count < numOfWords) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            txtProgress.setText(vocab[count % vocab.length]);
                        }
                    });
                    try {
                        Thread.sleep(1800);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    count++;
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                stopRecording();
            }
        }).start();
    }

    private void stopRecording() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtProgress.setVisibility(View.INVISIBLE);
                progressDialog.show();
            }
        });

        if (null != recorder) {
            isRecording = false;
            if (recorder.getState() == AudioRecord.STATE_INITIALIZED) {
                Log.d(LOG_TAG, "stopRecording");
                recorder.stop();
            }
            recorder.release();
            recorder = null;
        }

        copyWaveFile(getTempFilename(), getFilename());
        deleteTempFile();
        uploadFile();
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    private String getFilename() {
        String filepath = Environment.getExternalStorageDirectory().getPath();
        Log.d(LOG_TAG, "filePath: " + filepath);
        File file = new File(filepath, AUDIO_RECORDER_FOLDER);

        if (!file.exists()) {
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/knurld" + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private void copyWaveFile(String inFilename, String outFilename) {
        Log.d(LOG_TAG, "copyWavefile");
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels / 8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen, longSampleRate, channels, byteRate);

            while (in.read(data) != -1) {
                out.write(data);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(LOG_TAG, "finished copying file");
    }

    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen, long totalDataLen, long longSampleRate, int channels, long byteRate) throws IOException {

        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = RECORDER_BPP; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }

    private void uploadFile(){
        storageName = String.valueOf(UUID.randomUUID().getMostSignificantBits());
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://secret-calculator.appspot.com");
        wavRef = storageRef.child(storageName);

        Uri file = Uri.fromFile(new File(getFilename()));
        UploadTask uploadTask = wavRef.putFile(file);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(LOG_TAG, "upload failed", e.getCause());
                e.printStackTrace();
            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Uri downloadUrl = taskSnapshot.getDownloadUrl();
                Log.d(LOG_TAG, "downloadUrl: " + downloadUrl.toString());
                knurldSdk = new Knurld(getApplicationContext(), getWordIntervals());
                knurldSdk.setKnurldListener(VoiceActivity.this);
                if(isNewUser) {
                    knurldSdk.enrollUser(downloadUrl.toString());
                } else {
                    knurldSdk.verifyUser(downloadUrl.toString());
                }
            }
        });
    }

    @Override
    public void KnurldSuccess(){
        progressDialog.dismiss();
        wavRef.delete();

        if(isNewUser) {
            SharedPreferences pref = getSharedPreferences("first_install", 0);
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("hasInstalled", true);
            editor.apply();
        }

        Intent intent = new Intent(this, TextActivity.class);
        startActivity(intent);
        Log.d(LOG_TAG, "KNURLD SUCCESS!");
    }

    @Override
    public void KnurldFailure(){
        progressDialog.dismiss();
        wavRef.delete();
        Log.d(LOG_TAG, "KNURLD FAILURE");
        Intent intent = new Intent(this, CalculatorActivity.class);
        startActivity(intent);
    }

    private JSONObject getWordIntervals() {
        int multiplier = 1;
        if(isNewUser) multiplier = 3;
        List<WordInterval> wordList = WordDetection.detectWordsAutoSensitivity(getFilename(), vocab.length * multiplier);
        if (wordList.size() == 0) {
            Log.e(LOG_TAG, "wordList size is 0");
            Intent intent = new Intent(this, CalculatorActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
            return null;
        }
        JSONObject wordIntervals = new JSONObject();
        JSONArray intervals = new JSONArray();
        int count = 0;
        try {
            for (WordInterval words : wordList) {
                JSONObject interval = new JSONObject();
                if (words.getStopTime() - words.getStartTime() < 600) {
                    interval.accumulate("stop", words.getStartTime() + 601);
                } else {
                    interval.accumulate("stop", words.getStopTime());
                }
                interval.accumulate("start", words.getStartTime());
                interval.accumulate("phrase", vocab[count % vocab.length]);
                intervals.put(interval);
                count++;
            }
            wordIntervals.accumulate("intervals", intervals);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return wordIntervals;
    }
}
