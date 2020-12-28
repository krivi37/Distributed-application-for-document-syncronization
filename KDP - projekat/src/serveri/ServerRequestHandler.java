package serveri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class ServerRequestHandler {
	private serveri.ServerHttpParser.req_type req_t;
	private BufferedOutputStream out;
	private String request;
	private Socket clientSocket;
	private ServerHttpParser parser;
	boolean found = false;
	
	
	public void handle() {
		try {
		 switch(req_t) {
	        case GET: {
	        		//Pronalazenje fajla
	        		Pattern depth;
	        		String depth_pattern = "/";
	        		depth = Pattern.compile(depth_pattern);
	        		Matcher matcher = depth.matcher(request);
	        		int dubina = 1;
	        		while(matcher.find())dubina++;
	        		
	        		Path current = Paths.get(".");
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
	        			bis.close();
	        			out.write(mybytearray, 0, mybytearray.length);
	        			out.flush();
	        			out.close();
	        			clientSocket.close();
	        			this.found = true;
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
	        			this.found = false;
	        		}
	        		break;
	        	}
	        
	        case PUT: {
	        	out.write("HTTP/1.0 200 OK\r\n".getBytes());
	        	out.write("Connection: close\r\n".getBytes());
	        	out.write("\r\n".getBytes());
	        	out.flush();
	        	out.close();
	        	clientSocket.close();
	        	byte[] data = parser.getData();
	        	File fajl = new File(parser.getFilePath());
	        	fajl.mkdir();
	        	fajl = new File("./"+request);
	        	try (FileOutputStream fileOuputStream = new FileOutputStream(fajl)){
	        	    fileOuputStream.write(data);
	        	 }
	            break;
	        	}
	
	        case DELETE:{
	        	File fajl = new File("./"+request);
	        	if(fajl.exists()) {
	        		fajl.delete();
	        		out.write("HTTP/1.0 200 OK\r\n".getBytes());
	        		out.write("Connection: close\r\n".getBytes());
	        		out.write("\r\n".getBytes());
	        		out.flush();
	        		out.close();
	        		clientSocket.close();
	        	}
	        	else {
	        		out.write("HTTP/1.0 404 Not Found\r\n".getBytes());
	        		out.write("Connection: close\r\n".getBytes());
	        		out.write("\r\n".getBytes());
	        		out.flush();
	        		out.close();
	        		clientSocket.close();
	        	}
	        	break;
	        }
	      
	        
	        }
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public ServerRequestHandler(serveri.ServerHttpParser.req_type req ,BufferedOutputStream out, String request, Socket client, ServerHttpParser parser) {
		this.req_t = req;
		this.out = out;
		this.request = request;
		this.clientSocket = client;
		this.parser = parser;
	}
}
