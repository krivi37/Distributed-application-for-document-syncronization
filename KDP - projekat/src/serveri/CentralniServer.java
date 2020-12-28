package serveri;


import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.*;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;

import serveri.ServerThread;


public class CentralniServer {

	public static ExecutorService pool, delete_pool;
	public static int MAXRunningThreads;
	public static volatile boolean ok = false;
	
	public static class Podserver{
		public InetAddress adresa;
		public int port;
		public int timeouts;
		public boolean available;
		public boolean successful_delete = true;
		
		public Podserver(InetAddress adresa, int port) {
			this.adresa = adresa;
			this.port = port;
			timeouts = 0;
			available = true;
		}
	}
	
	public static boolean time_passed = true;
	public static Vector<Podserver> podserveri;
	public static Map<String, Vector<Podserver>>mapa;
	public static long m;
	public static int k;
	public static String address;
	
	public static synchronized void addPodserver(InetAddress address, int port) {
		boolean found = false;
		for (Podserver pod: podserveri) {
			if(pod.adresa.equals(address) && pod.port == port) {
				found = true;
				if (!pod.available) {
					pod.available = true;
				}
				return;
			}
		}
		if(!found) {
			Podserver pod = new Podserver(address, port);
			podserveri.add(pod);
		}
		
	}
	
	public static synchronized void AzurirajMapu(InetAddress address, int port, String request) {
		Vector<CentralniServer.Podserver> vektor;
		if(CentralniServer.mapa.containsKey(request)) {
			vektor = CentralniServer.mapa.get(request);
    	}
		else {
			vektor = new Vector<CentralniServer.Podserver>();
		}
		
		Podserver podserver = null;
		for (Podserver pod: podserveri) {
			if(pod.adresa.equals(address) && pod.port == port) {
				podserver = pod;
				break;
			}
		}
		vektor.add(podserver);
		CentralniServer.mapa.put(request, vektor);
	}
	
	public static synchronized void AzurirajPodservere(String request) {
		time_passed = false;
		Vector<CentralniServer.Podserver> vektor;
		if(CentralniServer.mapa.containsKey(request)) {
			vektor = CentralniServer.mapa.get(request);
			for(Podserver pod: vektor) {
				if(pod.available) delete_pool.execute(new DeleteThread(pod, request));
			}
    	}
		try {
			Thread.sleep(m);
			time_passed = true;
			delete_pool.shutdownNow();
			delete_pool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);//mozda se moglo odmah m staviti umjesto long.max_value
			File log = new File("./log.txt");
			BufferedWriter writer = new BufferedWriter(new FileWriter(log, true));
			writer.append("//////////////////////////////\n");
			if(CentralniServer.mapa.containsKey(request)) {
				vektor = CentralniServer.mapa.get(request);
			for (Podserver pod: vektor) {
				writer.append(pod.adresa.toString()+":"+pod.port+" : "+pod.successful_delete+'\n');
				if (!pod.successful_delete) {
					pod.timeouts++;
					if(pod.timeouts == CentralniServer.k) {
						pod.available = false;
						pod.timeouts = 0;
					}
				}
				}
			}
			writer.flush();
			writer.close();
			delete_pool = Executors.newFixedThreadPool(MAXRunningThreads);
			mapa.remove(request);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		mapa = new ConcurrentHashMap<String, Vector<Podserver>>();
		m = Long.parseLong(args[0]);
		k = Integer.parseInt(args[1]);
		JFrame frame = new JFrame("Server");
		JButton dugme = new JButton("Pokreni");
		frame.setSize(400, 400);
		dugme.setBounds(0, 250, 100, 100);
		JLabel labela = new JLabel("Unesite broj niti");
		JLabel adresa = new JLabel("Unesite adresu");
		JTextArea area1 = new JTextArea();
		labela.setBounds(0, 0, 250, 50);
		adresa.setBounds(0, 50, 50, 30);
		JTextArea area = new JTextArea();
		area.setBounds(0, 50, 150, 30);
		area1.setBounds(0,200, 150, 30);
		
		dugme.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(area.getText().contentEquals("")||area.getText()==null||area1.getText().contentEquals("")||area1.getText()==null)return;
				MAXRunningThreads = Integer.parseInt(area.getText());
				address = area1.getText();
				area1.setVisible(false);
				adresa.setVisible(false);
				dugme.setVisible(false);
				area.setVisible(false);
				ok = true;
			}
			
		});
		
		frame.setLayout(new BorderLayout());
		frame.add(dugme);
		frame.add(area);
		frame.add(labela);
		frame.add(area1);
		frame.add(adresa);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(frame.EXIT_ON_CLOSE);
		
		while(!ok);
		File log = new File("./log");
		if(log.exists())log.delete();
		try {
			podserveri = new Vector<Podserver>();
			pool= Executors.newFixedThreadPool(MAXRunningThreads);
			delete_pool = Executors.newFixedThreadPool(MAXRunningThreads);
			final int port = 8080;
			InetAddress addr = InetAddress.getByName(address);
			final ServerSocket server = new ServerSocket(8080,50,addr);
			labela.setBounds(40, 100, 300, 150);
			labela.setText("Server startovan na adresi: "+address+":"+port);
			System.out.println("Server startovan na adresi: "+address+":"+port);
			while (true) {
				Socket client = server.accept();
				pool.execute(new ServerThread(client));
				
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}