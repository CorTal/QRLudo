package angers.univ.ctalarmain.qrludo.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import angers.univ.ctalarmain.qrludo.R;

import static java.lang.String.valueOf;

/**
 * Created by etudiant on 28/04/17.
 */

public class OptionActivity extends AppCompatActivity {

    SeekBar sb_speedSpeech;
    Spinner spin_language;
    TextView tv_SSValue;
    Button b_valider;
    SharedPreferences settings;
    List<String> languages;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        languages = Arrays.asList(getResources().getStringArray(R.array.languages_array));
        setContentView(R.layout.activity_option);

        sb_speedSpeech = (SeekBar) findViewById(R.id.sb_SpeedSpeech);
        tv_SSValue = (TextView) findViewById(R.id.tv_SSValue);


        settings = getSharedPreferences(MainActivity.PREFS_NAME,0);
        float speedSpeech = settings.getFloat("speechSpeed",MainActivity.SPEEDSPEECH_DEFAULT);
        int progress = (int)((speedSpeech-0.8)*5);
        sb_speedSpeech.setProgress(progress);
        tv_SSValue.setText(String.format(Locale.ENGLISH,"%.2f",sb_speedSpeech.getProgress()*0.2 + 0.8));
        sb_speedSpeech.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv_SSValue.setText(String.format(Locale.ENGLISH,"%.2f",sb_speedSpeech.getProgress()*0.2 + 0.8));
                seekBar.setProgress(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        String language = settings.getString("languageSpeech",MainActivity.LOCALE_DEFAULT.getLanguage());

        String[] lang_list;
        lang_list= getResources().getStringArray(R.array.languages_array);


        spin_language = (Spinner) findViewById(R.id.spin_languages);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.display_lang_array, android.R.layout.simple_spinner_item);
// Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
// Apply the adapter to the spinner
        spin_language.setAdapter(adapter);
        int index = 0;
        for(int i = 0; i < languages.size(); i++)
        {
            if(languages.get(i).equals(language))
            {
                index = i;
            }
        }
        spin_language.setSelection(index);

        b_valider = (Button) findViewById(R.id.b_valider);
        b_valider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences.Editor edit = settings.edit();
                edit.putFloat("speechSpeed",Float.parseFloat(tv_SSValue.getText().toString()));

                edit.putString("languageSpeech",languages.get(spin_language.getSelectedItemPosition()));

                edit.apply();
                Intent intent = new Intent();
                if(getParent() == null){
                    setResult(RESULT_OK,intent);
                }else {
                    getParent().setResult(RESULT_OK, intent);
                }
                finish();
            }
        });


    }

    @Override
    public void onBackPressed() {
        if(getParent() == null){
            setResult(RESULT_CANCELED);
        }else {
            getParent().setResult(RESULT_CANCELED);
        }
        super.onBackPressed();

    }
}
