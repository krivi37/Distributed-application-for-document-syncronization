package klijent;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import javax.swing.*;

import serveri.Podserver;  

public class Client {

	public static void main(String[] args) {
		
		String file_name_pattern ="/.*$";
		String extract_path_pattern ="http://.*/";
		Pattern file_name, extract_path;
		file_name = Pattern.compile(file_name_pattern);
		extract_path = Pattern.compile(extract_path_pattern);
		
		
		JFileChooser chooser = new JFileChooser(); 
		
		JFrame f= new JFrame("Klijent");
		JButton g = new JButton("GET");
		JButton p = new JButton("PUT");
		JLabel labela = new JLabel("<html>UNESITE URL I KLIKNITE GET ILI<br/>UNESITE ADRESU SERVERA PA KLIKNITE PUT</html>");
		labela.setForeground(Color.DARK_GRAY);
		JLabel response = new JLabel();
		response.setVisible(false);
		JTextArea area = new JTextArea();
		area.setBounds(0, 70, 400, 50);
		response.setBounds(0, 150, 400, 70);
		p.setBounds(450,10,95,30); 
		g.setBounds(450, 40, 95, 30);
		labela.setBounds(0, 10, 400, 50);
		f.setSize(600, 500);
		
		p.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent event) { 
				String zahtjev = area.getText();
				try {
					labela.setText("UNESITE ADRESU SERVERA");
					labela.setForeground(Color.BLUE);
					File directory = new File("./client");
					chooser.setCurrentDirectory(directory);
					chooser.showOpenDialog(null);
					chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
					if(chooser.getSelectedFile()==null) {
						response.setText("MORATE ODABRATI FAJL");
						response.setForeground(Color.RED);
						response.setVisible(true);
						return;
					}
					File fajl = chooser.getSelectedFile();
					long length = fajl.length();
					byte[] mybytearray = new byte[(int) fajl.length()];
        			FileInputStream fis = new FileInputStream(fajl);
        			BufferedInputStream bis = new BufferedInputStream(fis);
        			bis.read(mybytearray, 0, mybytearray.length);
        			bis.close();
					
        			String file_name = fajl.getName();
        			if(zahtjev.isEmpty()) {
        				response.setText("MORATE UNIJETI ADRESU");
						response.setForeground(Color.RED);
						response.setVisible(true);
						return;
        			}
					URL url;
					url = new URL(zahtjev+"/"+file_name);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.setConnectTimeout(5000);
					con.setRequestMethod("PUT");
					con.addRequestProperty("Content-Length",""+length);
					con.setDoOutput(true);
					OutputStream out = con.getOutputStream();
					out.write(mybytearray, 0, mybytearray.length);
        			out.flush();
        			out.close();

					if(con.getResponseCode() == con.HTTP_OK) {
						response.setText("FILE UPDATED/UPLOADED");
						response.setForeground(Color.GREEN);
						response.setVisible(true);
					}
					
					else if(con.getResponseCode() == con.HTTP_UNAUTHORIZED) {
						response.setText("UNAUTHORIZED");
						response.setForeground(Color.RED);
						response.setVisible(true);
					}
					
					else {
						response.setText("SERVER ERROR");
						response.setForeground(Color.RED);
						response.setVisible(true);
					}
					
					labela.setText("UNESITE URL");
					labela.setForeground(Color.DARK_GRAY);
				} catch (IOException e) {
					response.setText("LOSE UNESENA ADRESA SERVERA");
					response.setForeground(Color.RED);
					response.setVisible(true);
					e.printStackTrace();
				}
				
				
			}
			
		});
		
		
		g.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent event) { 
				String zahtjev = area.getText();
				try {
					URL url = null;
					try {
					url = new URL(zahtjev);
					}catch(Exception e) {
					 	response.setText("LOS URL - UKUCAJTE OPET");
						response.setForeground(Color.RED);
						response.setVisible(true);
						return;
					}
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.setConnectTimeout(5000);
					con.setRequestMethod("GET");
					if(con.getResponseCode() == con.HTTP_OK) {
						
						
						InputStream in = con.getInputStream();
        				String content_length = con.getHeaderField("Content-Length");
        				int len = Integer.parseInt(content_length);
        				int count, control_count = 0;
        				byte[] buffer = new byte[2048];
        				
        				ByteArrayOutputStream buf = new ByteArrayOutputStream();
        				
        				while ((count = in.read(buffer)) != -1)
        				{
        				    control_count+=count;
        				    buf.write(buffer, 0, count);
        				    buf.flush();
        				    if (control_count >= len)break;
        				}
        				
        				byte[] data = buf.toByteArray();
        				buf.close();
        				con.disconnect();
        				
        				File fajl;
        				Matcher matcher = extract_path.matcher(zahtjev);
        				String file = matcher.replaceFirst("");
        				matcher = file_name.matcher(file);
        				String putanja = matcher.replaceFirst("");
        				if(matcher.find()) {
        					fajl = new File(("./client"+"/" + putanja));
	        				fajl.mkdir();
        				}
        				else { 
        					fajl = new File("./client/");
        					fajl.mkdir();
        				}
	    	        	fajl = new File("./client"+"/"+file);
	    	        	try (FileOutputStream fileOuputStream = new FileOutputStream(fajl)){
	    	        	    fileOuputStream.write(data);
	    	        	 }
	    	        	
	    	        	response.setText("FILE SAVED SUCCESFULY AT : "+fajl.getAbsolutePath());
						response.setForeground(Color.GREEN);
						response.setVisible(true);
	    	        	
					}
					else if(con.getResponseCode() == con.HTTP_NOT_FOUND) {
						response.setText("NOT FOUND");
						response.setForeground(Color.RED);
						response.setVisible(true);
					}
					
					else if(con.getResponseCode() == con.HTTP_UNAUTHORIZED) {
						response.setText("UNAUTHORIZED");
						response.setForeground(Color.RED);
						response.setVisible(true);
					}
					
				} catch (IOException e) {
					response.setText("SERVER ERROR");
					response.setForeground(Color.RED);
					response.setVisible(true);
					e.printStackTrace();
				}
				
				
			}
			
		});
		
		
		f.add(g);
		f.add(p);
		f.add(labela);
		f.add(area);
		f.add(response);
		f.setLayout(null);  
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
		
		

	}

}


