import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

public class WebFrame extends JFrame {
    private JTextField maxWorkersField;
    private JButton singleFetchButton;
    private JButton multiFetchButton;
    private JButton stopButton;
    private JLabel runningLabel;
    private JLabel completedLabel;
    private JLabel elapsedLabel;
    private JProgressBar progress;

    private DefaultTableModel model;
    private JTable table;

    private static final String DEFAULT_RUNNING_LABEL = "Running: ";
    private static final String DEFAULT_COMPLETED_LABEL = "Completed: ";
    private static final String DEFAULT_ELAPSED_LABEL = "Elapsed: ";

    private static final String URL_FILE = "links.txt";

    private Launcher launch;
    private AtomicInteger runningCount;
    private AtomicInteger completedCount;

    private class Launcher extends Thread {

        private Semaphore sem;
        private Semaphore interruptLock;

        public Launcher(int maxWorkers){
            runningCount = new AtomicInteger(0);
            completedCount = new AtomicInteger(0);
            sem = new Semaphore(maxWorkers);
            interruptLock = new Semaphore(1);
        }

        private void interruptWorkers(WebWorker[] workers){
            try {
                interruptLock.acquire();
            } catch (InterruptedException e) { }
            for(int i = 0; i < workers.length; i++){
                if(workers[i] == null) break;
                workers[i].interrupt();
            }
            interruptLock.release();
        }

        public void run(){
            updateRunningCount(1);
            long start = System.currentTimeMillis();
            WebWorker[] workers = new WebWorker[model.getRowCount()];
            for(int i = 0; i < model.getRowCount(); i++){
                try {
                    sem.acquire();
                } catch (InterruptedException e) {
                    setButtonsToDefault();
                    interruptWorkers(workers);
                    updateRunningCount(-1);
                    return;
                }
                try {
                    interruptLock.acquire();
                } catch (InterruptedException e) {
                }
                workers[i] = new WebWorker((String)model.getValueAt(i, 0), i, WebFrame.this, sem);
                workers[i].start();
                interruptLock.release();
            }
            for(int i = 0; i < model.getRowCount(); i++){
                try {
                    workers[i].join();
                } catch (InterruptedException e) {
                    setButtonsToDefault();
                    interruptWorkers(workers);
                    updateRunningCount(-1);
                    return;
                }
            }
            long end = System.currentTimeMillis();
            updateElapsed(end - start);
            setButtonsToDefault();
            updateRunningCount(-1);
        }

    }


    private void setButtonsToDefault(){
        setButtonAvailability(singleFetchButton, true);
        setButtonAvailability(multiFetchButton, true);
        setButtonAvailability(stopButton, false);
    }

    public void updateData(String result, int row){
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                model.setValueAt(result, row, 1);
            }
        });
    }

    public void updateRunningCount(int update){
        final int newUpd = update;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                runningCount.addAndGet(newUpd);
                runningLabel.setText(DEFAULT_RUNNING_LABEL + runningCount.get());
            }
        });
    }

    public void updateCompletedCount(boolean flag){
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if(flag){
                    completedCount.set(0);
                    progress.setValue(0);
                } else {
                    completedCount.addAndGet(1);
                    progress.setValue(completedCount.get());
                }
                completedLabel.setText(DEFAULT_COMPLETED_LABEL + completedCount.get());
            }
        });
    }

    private void updateElapsed(long elapsedTime){
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                elapsedLabel.setText(DEFAULT_ELAPSED_LABEL + elapsedTime);
            }
        });
    }

    private void setButtonAvailability(JButton button, boolean flag){
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                button.setEnabled(flag);
            }
        });
    }

    private void clearTable(){
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                for(int i = 0; i < model.getRowCount(); i++){
                    model.setValueAt("", i, 1);
                }
            }
        });
    }

    private void initActionListeners(){
        singleFetchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                clearTable();
                updateElapsed(0);
                setButtonAvailability(singleFetchButton, false);
                setButtonAvailability(multiFetchButton, false);
                setButtonAvailability(stopButton, true);
                launch = new Launcher(1);
                launch.start();
            }
        });

        multiFetchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                clearTable();
                if(!maxWorkersField.getText().equals("")) {
                    updateElapsed(0);
                    setButtonAvailability(singleFetchButton, false);
                    setButtonAvailability(multiFetchButton, false);
                    setButtonAvailability(stopButton, true);
                    launch = new Launcher(Integer.parseInt(maxWorkersField.getText()));
                    launch.start();
                }
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                setButtonsToDefault();
                launch.interrupt();
            }
        });
    }

    private void readData(){
        Scanner reader = null;
        try {
            reader = new Scanner(new File(URL_FILE));
        } catch(FileNotFoundException ex) {
            System.out.println("Unable to open file '" + URL_FILE + "'");
        }
        String str;
        while (reader.hasNext()){
            str = reader.nextLine();
            String[] row = new String[2];
            row[0] = str;
            row[1] = "";
            model.addRow(row);
        }
        reader.close();
    }

    private void initGUI(){
        setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[] { "url", "status"}, 0);
        readData();
        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(600,300));
        add(scrollpane, BorderLayout.CENTER);

        maxWorkersField = new JTextField();
        maxWorkersField.setMaximumSize(new Dimension(10, 1));
        singleFetchButton = new JButton("Single Thread Fetch");
        multiFetchButton = new JButton("Concurrent Fetch");
        stopButton = new JButton("Stop");
        progress = new JProgressBar(0, model.getRowCount());

        runningLabel = new JLabel(DEFAULT_RUNNING_LABEL);
        completedLabel = new JLabel(DEFAULT_COMPLETED_LABEL);
        elapsedLabel = new JLabel(DEFAULT_ELAPSED_LABEL);

        JPanel southPanel = new JPanel(new GridLayout(8, 1));
        southPanel.add(singleFetchButton);
        southPanel.add(multiFetchButton);
        southPanel.add(maxWorkersField);
        southPanel.add(runningLabel);
        southPanel.add(completedLabel);
        southPanel.add(elapsedLabel);
        southPanel.add(progress);
        southPanel.add(stopButton);
        stopButton.setEnabled(false);

        add(southPanel, BorderLayout.SOUTH);

        setLocationByPlatform(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    public WebFrame(){
        initGUI();
        initActionListeners();
    }

    public static void main(String[] args){
        WebFrame frame = new WebFrame();
    }

}
