package angers.univ.ctalarmain.qrludo.activities;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import angers.univ.ctalarmain.qrludo.R;
import angers.univ.ctalarmain.qrludo.utils.BasicImageDownloader;
import angers.univ.ctalarmain.qrludo.utils.BasicImageDownloader.OnImageLoaderListener;
import angers.univ.ctalarmain.qrludo.utils.BasicImageDownloader.ImageError;




public class ImageDialog extends Activity {

    private ImageView mDialog;
    private final int RES_PLACEHOLDER = R.drawable.placeholder_grey;
    private final int RES_ERROR = R.drawable.error_orange;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_dialog_layout);
        String html = getIntent().getStringExtra("EXTRA_HTML");
        final String name = getIntent().getStringExtra("EXTRA_NAME");


        mDialog = (ImageView)findViewById(R.id.image);
        mDialog.setClickable(true);

        mDialog.setImageResource(RES_PLACEHOLDER);
        final TextView tvPercent = (TextView) findViewById(R.id.tvPercent);
        final ProgressBar pbLoading = (ProgressBar) findViewById(R.id.pbImageLoading);
        final BasicImageDownloader downloader = new BasicImageDownloader(new OnImageLoaderListener() {
            @Override
            public void onError(ImageError error) {
                Toast.makeText(ImageDialog.this, "Error code " + error.getErrorCode() + ": " +
                        error.getMessage(), Toast.LENGTH_LONG).show();
                error.printStackTrace();
                mDialog.setImageResource(RES_ERROR);
                tvPercent.setVisibility(View.GONE);
                pbLoading.setVisibility(View.GONE);
            }

            @Override
            public void onProgressChange(int percent) {
                pbLoading.setProgress(percent);
                tvPercent.setText(percent + "%");
            }

            @Override
            public void onComplete(Bitmap result) {
                        /* save the image - I'm gonna use JPEG */
                final Bitmap.CompressFormat mFormat = Bitmap.CompressFormat.JPEG;
                        /* don't forget to include the extension into the file name */
                final File myImageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "image_test" + File.separator +  name + "." + mFormat.name().toLowerCase());
                BasicImageDownloader.writeToDisk(myImageFile, result, new BasicImageDownloader.OnBitmapSaveListener() {
                    @Override
                    public void onBitmapSaved() {
                        Toast.makeText(ImageDialog.this, "Image saved as: " + myImageFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onBitmapSaveError(ImageError error) {
                        Toast.makeText(ImageDialog.this, "Error code " + error.getErrorCode() + ": " +
                                error.getMessage(), Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }


                }, mFormat, false);

                tvPercent.setVisibility(View.GONE);
                pbLoading.setVisibility(View.GONE);
                mDialog.setImageBitmap(result);
                mDialog.startAnimation(AnimationUtils.loadAnimation(ImageDialog.this, android.R.anim.fade_in));
            }
        });
        downloader.download(html, true);

        //finish the activity (dismiss the image dialog) if the user clicks
        //anywhere on the image
        mDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }
}