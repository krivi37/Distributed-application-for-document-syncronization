package serveri;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.Socket;
import java.net.URL;

public class DeleteThread implements Runnable {
	
	CentralniServer.Podserver podserver;
	String request;
	
	@Override
	public void run() {
			boolean try_again = true;
			URL url;
			while(try_again) {
				try {
					if(Thread.interrupted()) {
						System.out.println("interrupted");
						podserver.successful_delete = false;
						break;
					}
					url = new URL("http:/"+podserver.adresa.toString()+":"+podserver.port+"/"+request);
					HttpURLConnection con = (HttpURLConnection) url.openConnection();
					con.setConnectTimeout((int) (CentralniServer.m/3));
					con.setRequestMethod("DELETE");
					con.getResponseCode();
					try_again = false;
					podserver.successful_delete = true;
				} catch (IOException e) {
					if(CentralniServer.time_passed) {
						podserver.successful_delete = false;
						try_again = false;

					}
				}
			}

	}

	public DeleteThread(CentralniServer.Podserver podserver, String request) {
		this.podserver = podserver;
		this.request = request;
	}
}
