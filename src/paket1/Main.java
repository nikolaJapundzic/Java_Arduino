package paket1;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Scanner;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.WindowConstants;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import com.fazecast.jSerialComm.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFCell;

public class Main {

	static SerialPort chosenPort;
	static int x = 0;
	//public static final String DATABASE_URL="jdbc:sqlite:Akvizicija_sa_vage.db";
	
	public static void main(String[] args) throws IOException{
		// TODO Auto-generated method stub
		JFrame windows = new JFrame();
		
		
		JSlider slider = new JSlider();
		slider.setMaximum(100);
		slider.setOrientation(JSlider.VERTICAL);
		windows.add(slider);
		//windows.pack();
		
		//Prozor koji sadrzi elemente
		windows.setTitle("Akvizicija sa vage");
		windows.setSize(400, 400);
		windows.setLayout(new BorderLayout());
		windows.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		windows.setResizable(false);
		
		//ComboBOX
		JComboBox<String> portList = new JComboBox<String>();
		JButton connectButton = new JButton("Connect");
		JButton dbToxlsButton = new JButton("Save .xls");
		JPanel topPanel = new JPanel();
		topPanel.add(portList);
		topPanel.add(connectButton);
		topPanel.add(dbToxlsButton);
		//topPanel.add(exitButton);
		windows.add(topPanel, BorderLayout.NORTH);
		
		//Pravljenje grafika
		XYSeries series = new XYSeries("Senzor mase");
		XYSeriesCollection dataset = new XYSeriesCollection(series);
		JFreeChart chart = ChartFactory.createXYLineChart("Senzor mase", "Vreme (sekunde)", "ADC series", dataset);
		windows.add(new ChartPanel(chart), BorderLayout.CENTER);
		
		//PRAVLJENJE EXIT BUTTON-A
		JButton exitButton = new JButton("EXIT");
		windows.add(exitButton, BorderLayout.SOUTH);
		
		
		windows.setVisible(true);
		
		
		//-----sada ide rad sa serijalizacijom
		
		// OVDE POCINJE BIRANJE PORTA KOJI CEMO DA KORISTIMO
		boolean flag = true;

		SerialPort comPort[] = SerialPort.getCommPorts();
		
		
		
		//System.out.println("Izaberite port: ");
		int i = 1;
		for(SerialPort port : comPort) {
			//System.out.println(i++ + ". " + port.getSystemPortName());
			portList.addItem(port.getSystemPortName());
			
		}
		
		//PRAVLJENJE BAZE PODATAKA
		Connection c = null;
        try {
            //Inicjalizujemo drajver za SQLite
            Class.forName("org.sqlite.JDBC");
            //Upostavljamo konekciju sa bazom
            c = DriverManager.getConnection("jdbc:sqlite:Akvizicija_sa_vage.db");
            //SQL naredbe koje zelimo da posaljemo bazi
        } catch ( Exception e )
        /*Hvatamo bilo kakav izuzetak koji moze da znaci
           da ne mozemo da uspostavimo konekciju sa bazom
         */
        {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        } finally{
            try {
                /*Zatvaramo konekciju sa bazom u slucaju da se desi neki
                   izuzetak ili ako sve uspe uspesno da se izvrsi
                 */
                c.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        //System.out.println("Uspesno kreirao bazu podataka");
        
        
        //KREIRANJE TABELA
        //Connection c = null;
        Statement stmt = null;
        try {
            Class.forName("org.sqlite.JDBC");
            c = DriverManager.getConnection("jdbc:sqlite:Akvizicija_sa_vage.db");
            //System.out.println("Uspesno konektovano na bazu");

            /*
               Sve kolone imaju postavljeno NOT NULL mora se svakoj
               koloni navesti vrednost
               Kolona id je proglasena za primarni kljuc sa kljucnim recima
               PRRIMARY KEY
             */
            stmt = c.createStatement();
            String sql = "CREATE TABLE artikal " +
                    "(id      INT PRIMARY KEY     NOT NULL," +
                    " vrednost   TEXT    NOT NULL, " +
                    " jedinica    TEXT     NOT NULL, " +
                    " vreme    TEXT     NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();


        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        } finally{
            try {
                /*Zatvaramo konekciju sa bazom u slucaju da se desi neki
                   izuzetak ili ako sve uspe uspesno da se izvrsi
                 */
                c.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        //System.out.println("Tabele kreirane uspesno");
        
        
        
        
		
		
		connectButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				if(connectButton.getText().equals("Connect")) {
					chosenPort = SerialPort.getCommPort(portList.getSelectedItem().toString());
					chosenPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 100, 0);
					if(chosenPort.openPort()) {
						connectButton.setText("Disconect");
						portList.setEnabled(false);
						exitButton.setEnabled(false);
						dbToxlsButton.setEnabled(false);
					}
					
					Thread tred = new Thread() {
						@Override public void run(){
							InputStream in = chosenPort.getInputStream();

							int vrednost = 0;
							String broj = "0";
							try
							{
							   while(flag) {
								 
								   char kar = (char)in.read();
								   //System.out.println(kar);
								   
								   if(Character.isDigit(kar)) {
									   broj = broj + kar;
									   continue;
								   }
								   
								   vrednost = Integer.parseInt(broj);
								   //System.out.println(vrednost);
								   //slider.setValue(vrednost);
								   series.add(x++, vrednost);
								   broj = "0";
								   windows.repaint();
								   
								   
								   //UZORKOVANJE TRENUTNOG VREMENA
								   DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

								   // Get the date today using Calendar object.
								   Date today = Calendar.getInstance().getTime();        
								   // Using DateFormat format method we can create a string 
								   // representation of a date with the defined format.
								   String reportDate = df.format(today);
								   // Print what date is today!
								   //System.out.println("Report Date: " + reportDate);
								   
								   //POPUNJAVANJE TABELA
								   Connection c = null;
							       Statement stmt = null;
								   try {
							            Class.forName("org.sqlite.JDBC");
							            c = DriverManager.getConnection("jdbc:sqlite:Akvizicija_sa_vage.db");

							            //System.out.println("Uspesno konektovano na bazu");

							            stmt = c.createStatement();
							            String sql = "INSERT INTO artikal (id,vrednost,jedinica,vreme) " +
							                    "VALUES ("+x+", '"+vrednost+"', '[kg]', '"+reportDate+"' );";
							            stmt.executeUpdate(sql);
							            stmt.close();


							        } catch ( Exception e ) {
							            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
							        } finally{
							            try {
							                /*Zatvaramo konekciju sa bazom u slucaju da se desi neki
							                   izuzetak ili ako sve uspe uspesno da se izvrsi
							                 */
							                c.close();
							            } catch (SQLException e) {
							                e.printStackTrace();
							            }
							        }
							        //System.out.println("Uspesno ubacene vrednosti");
								   
							   }
							} catch (Exception e) {}
						}
					};
					tred.start();
				}else {
					//diskonektuj
					//flag = false;
					chosenPort.closePort();
					portList.setEnabled(true);
					exitButton.setEnabled(true);
					dbToxlsButton.setEnabled(true);
					connectButton.setText("Connect");
					//series.clear();
					
				}
			}
		});
		
		
		
		dbToxlsButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				Thread tred2 = new Thread() {
					@Override
					public void run(){
						String fajl = "-.txt";	
						File f = new File(fajl);
						
						try {
							f.createNewFile();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						FileWriter fw;
						try {
							fw = new FileWriter(f);
							BufferedWriter bw = new BufferedWriter(fw);
							
							//OVDE POCINJE CITANJE IZ BAZE PODATAKA I PISANJE U TXT
							Connection c = null;
						    Statement stmt = null;
							try {
								Class.forName("org.sqlite.JDBC");
					            c = DriverManager.getConnection("jdbc:sqlite:Akvizicija_sa_vage.db");
					            stmt = c.createStatement();
								ResultSet rs = stmt.executeQuery( "SELECT * FROM artikal" );
								
								//PRIPREMA ZA EXCEL
								HSSFWorkbook wb = new HSSFWorkbook();
		                        HSSFSheet sheet = wb.createSheet("Excel Sheet");
		                        HSSFRow rowhead = sheet.createRow((short) 0);
		                        rowhead.createCell((short) 0).setCellValue("id");
		                        rowhead.createCell((short) 1).setCellValue("vrednost");
		                        rowhead.createCell((short) 2).setCellValue("jedinica");
		                        rowhead.createCell((short) 3).setCellValue("vreme");
		                        int index = 1;
		                        while (rs.next()) {

		                                HSSFRow row = sheet.createRow((short) index);
		                                row.createCell((short) 0).setCellValue(rs.getInt(1));
		                                row.createCell((short) 1).setCellValue(rs.getString(2));
		                                row.createCell((short) 2).setCellValue(rs.getString(3));
		                                row.createCell((short) 3).setCellValue(rs.getString(4));
		                                index++;
		                        }
		                        FileOutputStream fileOut = new FileOutputStream("Prikupljeni_podatci.xls");
		                        wb.write(fileOut);
		                        fileOut.close();
		                        rs.close();
		                        stmt.close();
		                        c.close();	
								
								
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} catch (ClassNotFoundException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				};
				tred2.start();
				
				
			}
		});
		
		exitButton.addActionListener(new ActionListener() {
			@Override public void actionPerformed(ActionEvent arg0) {
				//BRISE DB FILE
				boolean result = new File("Akvizicija_sa_vage.db").delete();
				boolean resultat = new File("-.txt").delete();
				//IZLAZI IZ PROGRAMA
				System.exit(0);
			}
		});
		
		
		//-------------------
		

	}

}
