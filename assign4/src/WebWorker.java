import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Semaphore;
import javax.swing.*;

public class WebWorker extends Thread {
    private String urlString;
    private int row;
    private WebFrame frame;
    private static final String ERROR = "err";
    private static final String INTERRUPTION = "interrupted";
    private static final String SUCCESS = "OK";
    private Semaphore sem;


    public WebWorker(String urlString, int row, WebFrame frame, Semaphore sem){
        this.urlString = urlString;
        this.row = row;
        this.frame = frame;
        this.sem = sem;
    }

    private void updateGUI(String error, int downloadSize, long runTime, String date){
        String result;
        if(error != SUCCESS){
            result = error;
        } else {
            result = date + " " + runTime + "ms " + downloadSize + " bytes";
        }
        frame.updateData(result, row);

    }

    private void download(){
        InputStream input = null;
        StringBuilder contents = null;
        try {
            long start = System.currentTimeMillis();
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();

            // Set connect() to throw an IOException
            // if connection does not succeed in this many msecs.
            connection.setConnectTimeout(5000);

            connection.connect();
            input = connection.getInputStream();

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            char[] array = new char[1000];
            int len;
            contents = new StringBuilder(1000);
            while ((len = reader.read(array, 0, array.length)) > 0) {
                if (isInterrupted()) {
                    updateGUI(INTERRUPTION, 0, 0, null);
                    break;
                }
                contents.append(array, 0, len);
                Thread.sleep(100);
            }
            if (!isInterrupted()) {
                // Successful download if we get here
                long end = System.currentTimeMillis();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
                updateGUI(SUCCESS, contents.length(), end - start, simpleDateFormat.format(new Date()));
            }

        }
        // Otherwise control jumps to a catch...
        catch (MalformedURLException ignored) {
            updateGUI(ERROR, 0, 0, null);
        } catch (InterruptedException exception) {
            updateGUI(INTERRUPTION, 0, 0, null);
        } catch (IOException ignored) {
            updateGUI(ERROR, 0, 0, null);
        }
        // "finally" clause, to close the input stream
        // in any case
        finally {
            try {
                if (input != null) input.close();
            } catch (IOException ignored) {}
        }
        sem.release();
        frame.updateRunningCount(-1);
        frame.updateCompletedCount(false);
    }

    public void run() {
        frame.updateRunningCount(1);
        download();
    }

    public static void main(String[] args) {
        WebWorker worker = new WebWorker("ptth/:/syntaxerror.com/", 1, null, new Semaphore(1));
        worker.start();
    }
}
