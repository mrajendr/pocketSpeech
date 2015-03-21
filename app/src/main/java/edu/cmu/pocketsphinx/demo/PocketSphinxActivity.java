/* ====================================================================
 * Copyright (c) 2014 Alpha Cephei Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY ALPHA CEPHEI INC. ``AS IS'' AND
 * ANY EXPRESSED OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY
 * NOR ITS EMPLOYEES BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 */

package edu.cmu.pocketsphinx.demo;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;

public class PocketSphinxActivity extends Activity implements
        RecognitionListener {

    private static final String TAG = "MyActivity";

    private static final String KWS_SEARCH = "wakeup";
    private static final String KWS_STOP = "stop";
    private static final String FORECAST_SEARCH = "forecast";
    private static final String STOP_SEARCH = "stop";
    private static final String DIGITS_SEARCH = "presentation";
    private static final String MENU_SEARCH = "menu";
    private static final String KEYPHRASE = "start";

    private SpeechRecognizer recognizer;
    private HashMap<String, Integer> captions;
    private HashMap<String, Integer> wordCount = new HashMap<String, Integer>();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Prepare the data for UI
        captions = new HashMap<String, Integer>();
        captions.put(KWS_SEARCH, R.string.kws_caption);
        captions.put(KWS_STOP, R.string.kws_stop_caption);
        captions.put(MENU_SEARCH, R.string.menu_caption);
        captions.put(DIGITS_SEARCH, R.string.digits_caption);
        captions.put(FORECAST_SEARCH, R.string.forecast_caption);
        setContentView(R.layout.main);
        ((TextView) findViewById(R.id.caption_text))
                .setText("Preparing the recognizer");

        // Recognizer initialization is a time-consuming and it involves IO,
        // so we execute it in async task

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(PocketSphinxActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    ((TextView) findViewById(R.id.caption_text))
                            .setText("Failed to init recognizer " + result);
                } else {
                    switchSearch(KWS_SEARCH);
                }
            }
        }.execute();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        String text = hypothesis.getHypstr();

        Log.d(TAG, "onPartialResult");
        if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
//        else if (text.equals(DIGITS_SEARCH))
//            switchSearch(DIGITS_SEARCH);
        else if (text.equals(STOP_SEARCH))
            makeText(getApplicationContext(), "STOPPPP Pls", Toast.LENGTH_SHORT).show();
        else {
            switchSearch(DIGITS_SEARCH);
            ((TextView) findViewById(R.id.result_text)).setText(text);
        }

    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.result_text)).setText("");
        if (hypothesis != null) {
            String text = hypothesis.getHypstr();

            String[] words = text.split(" ");
            for (int i=0; i < words.length; i++) {
                Integer count = wordCount.get(words[i]);
                if (count != null) {
                    wordCount.put(words[i], count + 1);
                } else {
                    wordCount.put(words[i], 1);
                }
            }
            makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
        }
//        System.out.print("WORD COUNT SON: " + wordCount);
        Log.d(TAG, "onResult");
        Log.d(TAG, "WORD COUNT SON: " + wordCount);
    }

    @Override
    public void onBeginningOfSpeech() { Log.d(TAG, "onBegginingof Speech");
    }

    @Override
    public void onEndOfSpeech() {
        Log.d(TAG, "onEndofSpeech");
        if (DIGITS_SEARCH.equals(recognizer.getSearchName()))
//                || FORECAST_SEARCH.equals(recognizer.getSearchName()))
            switchSearch(KWS_SEARCH);
    }

    private void switchSearch(String searchName) {
        Log.d(TAG, "onSWITCHSEARCh");
        recognizer.stop();
        recognizer.startListening(searchName);
        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.caption_text)).setText(caption);
    }

    private void setupRecognizer(File assetsDir) {
        File modelsDir = new File(assetsDir, "models");
        recognizer = defaultSetup()
                .setAcousticModel(new File(modelsDir, "hmm/en-us-semi"))
                .setDictionary(new File(modelsDir, "dict/cmu07a.dic"))
                .setRawLogDir(assetsDir).setKeywordThreshold(1e-20f)
                .getRecognizer();
        recognizer.addListener(this);

        // Create keyword-activation search.
        recognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
        recognizer.addKeyphraseSearch(KWS_STOP, STOP_SEARCH);
        // Create grammar-based searches.
        File menuGrammar = new File(modelsDir, "grammar/menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
        File digitsGrammar = new File(modelsDir, "grammar/digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
        // Create language model search.
//        File languageModel = new File(modelsDir, "lm/weather.dmp");
//        recognizer.addNgramSearch(FORECAST_SEARCH, languageModel);
    }
}
