
import java.awt.BorderLayout; 
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import com.fazecast.jSerialComm.SerialPort;

public class ArduinoGui {
	
	static SerialPort chosenPort;
	static int x = 0;

	public static void main(String[] args) {
		
		 //2016/11/16 12:08:43
		// create and configure the window
		JFrame window = new JFrame();
		window.setTitle("Sensor Graph GUI");
		window.setSize(600, 400);
		window.setLayout(new BorderLayout());
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// create a drop-down box and connect button, then place them at the top of the window
		JComboBox<String> portList = new JComboBox<String>();
		JButton connectButton = new JButton("Connect");
		JMenuBar bar = new JMenuBar();
		JMenu menu = new JMenu("Options");
		JMenuItem reset = new JMenuItem("Reset");
		JMenuItem quit = new JMenuItem("Quit"); bar.add(menu);
		reset.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK));
		quit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, KeyEvent.META_DOWN_MASK));
		menu.add(quit);
		menu.add(reset);
	    window.setJMenuBar(bar);
		JPanel topPanel = new JPanel();
		topPanel.add(portList);
		topPanel.add(connectButton);
		window.add(topPanel, BorderLayout.NORTH);
		ConfigurationBuilder configurationbuilder = new ConfigurationBuilder();
		configurationbuilder.setDebugEnabled(true).setOAuthConsumerKey("ClbXOu75dRNNZYfRRFQT6KeWx")
		.setOAuthConsumerSecret("2BHyP2vZnibQJR22wACkX9b24UE5fk6Wq7Uu33a9AEBlejOIQU")
		.setOAuthAccessToken("944772459894591488-WI5550LoaOyr6bX33BQhZF4mVRvs7hh")
		.setOAuthAccessTokenSecret("Jh0EXBmh1x4roxmlUwllHcS7ApjP59EWyJsDGrQWVny0Z");
		TwitterFactory tf = new TwitterFactory(configurationbuilder.build());
		twitter4j.Twitter twitter = tf.getInstance();
		
		// populate the drop-down box
		SerialPort[] portNames = SerialPort.getCommPorts();
		for(int i = 0; i < portNames.length; i++)
			portList.addItem(portNames[i].getSystemPortName());
		
		// create the line graph
		XYSeries series = new XYSeries("Temperature Sensor Readings");
		XYSeries series2 = new XYSeries("Other Readings");
		XYSeriesCollection dataset = new XYSeriesCollection(series);
		dataset.addSeries(series2);
		
		JFreeChart chart = ChartFactory.createXYLineChart("Temperature Readings", "Time (seconds)", "Analog Reading", dataset);
		window.add(new ChartPanel(chart), BorderLayout.CENTER);
		
		// configure the connect button and use another thread to listen for data
		connectButton.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent arg0) {
				if(connectButton.getText().equals("Connect")) {
					// attempt to connect to the serial port
					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_SCANNER, 0, 0);
					if(chosenPort.openPort()) {
						connectButton.setText("Disconnect");
						portList.setEnabled(false);
					}
					
					// create a new thread that listens for incoming text and populates the graph
					Thread thread = new Thread(){
						@Override public void run() {
							Scanner scanner = new Scanner(chosenPort.getInputStream());
							while(scanner.hasNextLine()) {
								try {
									DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
									Date date = new Date();
									String line = scanner.nextLine();
								    double number = Double.parseDouble(line); 
								    String line2 = scanner.nextLine();
								    double number2 = Double.parseDouble(line2);
								    if(number > 26) {
										try {
											 twitter.updateStatus("It's too hot right now  "+ dateFormat.format(date));
										} catch (TwitterException e1) {
											e1.printStackTrace();
										}	
									}
									else if (number < 18) {
											try {
												 twitter.updateStatus("It's too cold right now  " + dateFormat.format(date));
											} catch (Exception e1) {		
												e1.printStackTrace();
											}	
									}
								    
								    series2.add(x,number2);
									series.add(x++, number);
									
									window.repaint();
								} catch(Exception e) {}
							}
							scanner.close();
						}
					};
					thread.start();
				} else {
					// disconnect from the serial port
					chosenPort.closePort();
					portList.setEnabled(true);
					connectButton.setText("Connect");
					series.clear();
					x = 0;
				}
			}
		});
		
		// show the window
		window.setVisible(true);


}

}

