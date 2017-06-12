package angers.univ.ctalarmain.qrludo.utils;

import android.os.AsyncTask;

public class QuestionDelayCounter extends AsyncTask<Integer,Void,Boolean> {
    public QDCResponse delegate = null;
    @Override
    protected Boolean doInBackground(Integer... params) {
        try {
            Thread.sleep(params[0]*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    protected void onPostExecute(Boolean result) {
        delegate.processFinish(result);
    }

}
