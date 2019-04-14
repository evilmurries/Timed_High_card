
import javax.swing.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TimerDisplay extends JPanel{
   


   
   public JButton startStopButton;
   
   public JTextArea timerText;
   
   private final int WIDTH = 250;
   private final int HEIGHT = 100;
   
   private String timeString;
   
   public TimerDisplay() {
      super();
      timeString = String.format(" %s:0%s " , 0, 0);
      this.setSize(WIDTH, HEIGHT);
      initTimerText();
      initStartStopButton();
   }
   
   public TimerDisplay(String timeIn) {
      this();
      timeString = timeIn;
      timerText.invalidate();
   }
   
   public void update(final TimerModel timer) {
      timerText.setText(timer.toString());
      timerText.repaint();
      timerText.validate();
      startStopButton.repaint();
      startStopButton.validate();
      repaint();
      validate();
   }
   
   public void initStartStopButton() {
      startStopButton = new JButton("Start");
      startStopButton.setVisible(true);
      add(startStopButton);
   }
   
   private void initTimerText() {
      timerText = new JTextArea(timeString);
      timerText.setBackground(Color.BLACK);
      timerText.setForeground(Color.GREEN);
      timerText.setFont(new Font(Font.DIALOG, Font.BOLD, 27));
      add(timerText);
   }
}



class TimerModel extends Thread
{
   private int minutes, seconds;
   private boolean timerOn, timerStarted;
   private final static int WAIT = 1500;
   private final TimerDisplay display;
   
  public TimerModel(TimerDisplay displayIn) {
     minutes = 0;
     seconds = 0;
     timerOn = false;
     display = displayIn;
  }
  
  public void run() {
     timerStarted = true;
     while(timerStarted)
     {
        doNothing(WAIT);
        if(seconds < 59 && timerOn) {
           seconds++;
        }
     
     else if((seconds >= 59) && timerOn) {
        seconds = 0;
        minutes++;
     }
     if(timerOn) {
        printTime();
        display.update(this);
     }
     }
  }
  
  public void stopTimer() {
     timerOn = false;
  }
  
  public void resumeTimer() {
     timerOn = true;
  }
  
  public boolean timerOn() {
     return timerOn;
  }
  
  public int getMinutes() {
     return minutes;
  }
  
  public int getSeconds() {
     return seconds;
  }
  
  public int getMS() {
     return (minutes * 60000) + (seconds *1000);
  }
  
  public boolean started() {
     return timerStarted;
  }
  
  public TimerDisplay getDisplayObject() {
     return display;
  }
  
  public String toString() {
     if(seconds < 10)
        return String.format(" %s:0%s ", minutes, seconds);
     else
        return String.format(" %s:0%s ", minutes, seconds);
  }
  
  private void printTime() {
     System.out.printf(toString());
  }
  
  private void doNothing(int waitTime) {
     try {
        Thread.sleep(waitTime);
     }
     catch(InterruptedException e) {
        e.printStackTrace();
        System.exit(0);
     }
  }
}
