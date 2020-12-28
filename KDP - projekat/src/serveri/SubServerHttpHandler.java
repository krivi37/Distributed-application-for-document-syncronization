package serveri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class SubServerHttpHandler {
	private serveri.SubServerHttpParser.req_type req_t;
	private BufferedOutputStream out;
	private String request;
	private Socket clientSocket;
	private SubServerHttpParser parser;
	private boolean found;
	
	
	public void handle() {
		try {
		 switch(req_t) {
	        case GET: {
	        		//Pronalazenje fajla
	        		Pattern depth;
	        		String depth_pattern = "/";
	        		depth = Pattern.compile(depth_pattern);
	        		Matcher match = depth.matcher(request);
	        		int dubina = 1;
	        		while(match.find())dubina++;
	        		Path current = Paths.get("./subserver"+Podserver.server.getLocalPort());
	        		String req = request.replace('/', '\\');
	        		Stream<Path> stream =
	                        Files.find(current, dubina,
	                                (path, basicFileAttributes) -> {
	                                    File file = path.toFile();
	                                    return !file.isDirectory() &&
	                                            file.getPath().contains(req);//filtriranje fajlova kojima putanja sadrzi putanju iz zahtjeva
	                                });
	        		
	                String file_path = "";
	                Path putanje[] = stream.toArray(Path[]::new);
	                stream.close();
	                if(putanje.length != 0) {
	                	file_path = putanje[0].toString();
	                }
	        		String ime_fajla = parser.getFileName();
	        		File fajl = new File(file_path);
	        		if(fajl.isFile()) {
	        	
	        			long length = fajl.length();
	        			out.write("HTTP/1.0 200 OK\r\n".getBytes());
	        			out.write("Content-Type: application/octet-stream\r\n".getBytes());
	        			out.write(("Content-Length: "+length+"\r\n").getBytes());
	        			out.write("Content-Transfer-Encoding: binary\r\n".getBytes());
	        			out.write(("Content-Disposition: attachment; filename=\""+ime_fajla+"\"\r\n").getBytes());
	        			out.write("Connection: close\r\n".getBytes());
	        			out.write("\r\n".getBytes());
	        			byte[] mybytearray = new byte[(int) fajl.length()];
	        			FileInputStream fis = new FileInputStream(fajl);
	        			BufferedInputStream bis = new BufferedInputStream(fis);
	        			bis.read(mybytearray, 0, mybytearray.length);
	        			out.write(mybytearray, 0, mybytearray.length);
	        			out.flush();
	        			out.close();
	        			bis.close();
	        			clientSocket.close();
	        		}
	              
	        		else {
	        			
	        			
	        			//Ako nema lokalno salje zahtjev serveru i prima zahtjev
	        			URL url = new URL("http://192.168.1.100:8080/"+request);
	        			try{
	        				HttpURLConnection con = (HttpURLConnection) url.openConnection();
	        				con.setConnectTimeout(5000);
	        				con.setRequestMethod("GET");
	        				con.setRequestProperty("Server-Socket", ""+Podserver.server.getLocalPort());
		        			if(con.getResponseCode() == con.HTTP_OK) found = true;
		        			else found = false;
		        	
		
		        			if(found) {
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
		        				
		        				String file_name_pattern ="/.*$";
		        				Pattern file_name;
		        				file_name = Pattern.compile(file_name_pattern);
		        				Matcher matcher = file_name.matcher(request);
		        				if(matcher.find()) {
		        					String putanja = matcher.replaceFirst("");
		        					fajl = new File(("./subserver"+Podserver.server.getLocalPort()+"/" + putanja));
			        				fajl.mkdir();
		        				}
			    	        	fajl = new File("./subserver"+Podserver.server.getLocalPort()+"/"+request);
			    	        	try (FileOutputStream fileOuputStream = new FileOutputStream(fajl)){
			    	        	    fileOuputStream.write(data);
			    	        	 }
		        				
		        				long length = data.length;
			        			out.write("HTTP/1.0 200 OK\r\n".getBytes());
			        			out.write("Content-Type: application/octet-stream\r\n".getBytes());
			        			out.write(("Content-Length: "+length+"\r\n").getBytes());
			        			out.write("Content-Transfer-Encoding: binary\r\n".getBytes());
			        			out.write(("Content-Disposition: attachment; filename=\""+ime_fajla+"\"\r\n").getBytes());
			        			out.write("Connection: close\r\n".getBytes());
			        			out.write("\r\n".getBytes());
			        			out.write(data, 0, data.length);
			        			out.flush();
			        			out.close();
			        			clientSocket.close();
		        					
		        				}
		        			else {
		        				String poruka = "<h1>Dokument nije pronadjen<h1>";
			        			long length = poruka.length();
			        			out.write("HTTP/1.0 404 Not Found\r\n".getBytes());
			        			out.write("Content-Type: text/html\r\n".getBytes());
			        			out.write(("Content-Length: "+length+"\r\n").getBytes());
			        			out.write("Connection: close\r\n".getBytes());
			        			out.write("\r\n".getBytes());
			        			out.write(poruka.getBytes());
			        			out.flush();
			        			out.close();
			        			clientSocket.close();
		        				return;
		        			}
	        				
	        				
	        			}catch (java.net.ConnectException|java.net.SocketTimeoutException e){
	        				String poruka = "<h1>Dokument nije pronadjen na podserveru, a centralni server nije dostupan<h1>";
	        				long length = poruka.length();
	        				out.write("HTTP/1.0 500 Internal Server Error\r\n".getBytes());
	        				out.write("Content-Type: text/html\r\n".getBytes());
	        				out.write(("Content-Length: "+length+"\r\n").getBytes());
	        				out.write("Connection: close\r\n".getBytes());
	        				out.write("\r\n".getBytes());
	        				out.write(poruka.getBytes());
	        				out.flush();
	        				out.close();
	        				clientSocket.close();
	        				return;
	        			
	        			}
	        		}
	        		break;
	        	}
	
	        case DELETE:{
	        	out.write("HTTP/1.0 200 OK\r\n".getBytes());
	        	out.write("Connection: close\r\n".getBytes());
	        	out.write("\r\n".getBytes());
	        	out.flush();
	        	out.close();
	        	clientSocket.close();
	        	File fajl = new File("./subserver"+Podserver.server.getLocalPort()+"/"+request);
	        	if(fajl.exists())fajl.delete();
	        	break;
	        }
	      
	        default:{
	        	out.write("HTTP/1.0 401 Unauthorized\r\n".getBytes());
	        	out.write("Connection: close\r\n".getBytes());
	        	out.write("\r\n".getBytes());
	        	out.flush();
	        	out.close();
	        	clientSocket.close();
	        	break;
	        }
	        
	        }
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public SubServerHttpHandler(SubServerHttpParser.req_type req ,BufferedOutputStream out, String request, Socket client, SubServerHttpParser parser) {
		this.req_t = req;
		this.out = out;
		this.request = request;
		this.clientSocket = client;
		this.parser = parser;
		this.found = true;
	}
}
