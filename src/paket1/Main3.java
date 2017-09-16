package paket1;

import java.io.InputStream;
import java.io.PrintWriter;
//import java.io.OutputStream;
import java.util.Scanner;

//import javax.swing.JFrame;
//import javax.swing.JSlider;

import com.fazecast.jSerialComm.*;

public class Main3 {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		/*JFrame windows = new JFrame();
		JSlider slider = new JSlider();
		slider.setMaximum(1023);
		windows.add(slider);
		windows.pack();
		windows.setVisible(true);*/
		
		//-----sada ide rad sa serijalizacijom
		boolean flag = true;
		
		//byte[] ba = {'A'}; 

		SerialPort comPort[] = SerialPort.getCommPorts();
		System.out.println("Izaberite port: ");
		int i = 1;
		for(SerialPort port : comPort) {
			System.out.println(i++ + ". " + port.getSystemPortName());
		}
		
		Scanner SK = new Scanner(System.in);
		int chosenPort = SK.nextInt();
		
		SerialPort port = comPort[chosenPort - 1];
		
		
		port.openPort();
		
		if(port.openPort()) {
			System.out.println("Uspesno otvoren port.");
			Thread thread = new Thread() {
				@Override public void run() {
					try {Thread.sleep(100);}catch(Exception e) {}
					
					PrintWriter output = new PrintWriter(port.getOutputStream());
					
					while(flag) {
						String unos = SK.nextLine();
						output.print(unos);
						output.flush();
					}
					
				}
			};
			thread.start();
		}
		
		port.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
		InputStream in = port.getInputStream();
		//OutputStream out = port.writeBytes(ba, chosenPort); 
		try
		{
		   while(flag) {
			   System.out.print((char)in.read());
		   }
		   in.close();
		} catch (Exception e) { e.printStackTrace(); }
		port.closePort();
		System.out.println("Port uspesno zatvoren.");

	}

}
