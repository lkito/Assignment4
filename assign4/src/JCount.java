import javax.swing.*;
import javax.swing.plaf.SliderUI;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class JCount extends JPanel {
    private static final int NUM_COUNTERS = 4;
    private static final int DEFAULT_COUNT_AMOUNT = 1_000_000;
    private JTextField textField;
    private JButton startButton;
    private JButton stopButton;
    private JLabel label;
    private int count;
    private Worker wk;

    private class Worker extends Thread {
        private static final int UPDATE_AMOUNT = 10_000;
        private static final int SLEEP_AMOUNT = 50;
        private int countTo;

        public Worker(int countTo){
            this.countTo = countTo;
        }

        private void setLabelText(String str){
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    label.setText(str);
                }
            });
        }

        public void run() {
            int curCount = 0;
            setLabelText("0");
            boolean finishedCounting = false;
            while(curCount < countTo){
                curCount++;
                if(curCount % UPDATE_AMOUNT == 0){
                    final String printAmount = "" + curCount;
                    setLabelText("" + printAmount);
                    try {
                        sleep(SLEEP_AMOUNT);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
                if(curCount == countTo) finishedCounting = true;
            }
            if(finishedCounting) setLabelText("" + countTo);
        }
    }

    private void initListeners(){
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                String txt = textField.getText();
                int amount = DEFAULT_COUNT_AMOUNT;
                if(txt.matches("\\d+")){
                    amount = Integer.parseInt(txt);
                }
                if(wk != null) wk.interrupt();
                wk = new Worker(amount);
                wk.start();
            }
        });

        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if(wk != null) wk.interrupt();
            }
        });
    }

    public JCount(){
        wk = null;
        count = 0;
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        startButton = new JButton("Start");
        stopButton = new JButton("Stop");
        label = new JLabel("0");
        textField = new JTextField("", 10);

        initListeners();

        add(textField);
        add(label);
        add(startButton);
        add(stopButton);
        add(Box.createRigidArea(new Dimension(0,40)));
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame();
        frame.setLayout(new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS));
        for(int i = 0; i < NUM_COUNTERS; i++) {
            frame.add(new JCount());
        }
        frame.setLocationByPlatform(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }



    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}