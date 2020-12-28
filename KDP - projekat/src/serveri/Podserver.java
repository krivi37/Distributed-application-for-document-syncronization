package serveri;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import serveri.ServerHttpParser.req_type;

public class Podserver {
	
	public static ServerSocket server;
	static req_type req_t;
	static ServerHttpParser parser;
	public static ExecutorService pool;
	public static int MAXRunningThreads = 5;
	public static volatile boolean ok = false;
	public static String zahtjev;
	
	public static void main(String[] args) {
		
		JFrame frame = new JFrame();
		JLabel labela = new JLabel();
		JButton dugme = new JButton("Unesi");
		JTextArea area = new JTextArea();
		labela.setBounds(0, 0, 200, 30);
		area.setBounds(0, 200, 150, 30);
		dugme.setBounds(0, 150, 100, 100);
		frame.setSize(300, 300);
		frame.setLayout(new BorderLayout());
		frame.add(dugme, BorderLayout.SOUTH);
		frame.add(labela, BorderLayout.NORTH);
		frame.add(area, BorderLayout.CENTER);
		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		labela.setText("Unesite ip adresu racunara");
		dugme.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(area.getText().contentEquals("")||area.getText()==null) {
					labela.setText("Morate unijeti IP adresu");
					return;
				}
				dugme.setVisible(false);
				area.setVisible(false);
				zahtjev = area.getText();
				ok = true;
				
			}
			
		});
		
		frame.setVisible(true);
		
		int port = 3000;

		boolean success = false;
		while(!ok);
		try {
			InetAddress addr = InetAddress.getByName(zahtjev);
			while(!success) {
				try {
					server = new ServerSocket(port, 50, addr);
					success = true;
				}catch (IOException e) {
					port += 30;
					if (port>4500)return;
				}
			}
			System.out.println("Server startovan na adresi: "+zahtjev+":"+port);
			labela.setText("Server startovan na adresi: "+zahtjev+":"+port);
			labela.setBounds(50, 70, 200, 30);
			pool= Executors.newFixedThreadPool(MAXRunningThreads);
        	File dir = new File("./subserver"+Podserver.server.getLocalPort());
        	if(dir.exists()) {
        	Path rootPath = dir.toPath();
        	Files.walk(rootPath)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .peek(System.out::println)
            .forEach(File::delete);
        	}
        	dir.delete();
        	dir.mkdir();
        	
			while (true) {
				Socket client = server.accept();
				pool.execute(new SubServerThread(client));
				
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public Podserver() {

	}
	
}
