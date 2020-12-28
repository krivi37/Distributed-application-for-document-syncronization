package serveri;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import serveri.ServerHttpParser.req_type;


public class ServerThread implements Runnable {
	
	Socket clientSocket; 
	req_type req_t;
	ServerHttpParser parser;
	
	@Override
	public void run() {
		try {
		BufferedOutputStream out;
		
		System.out.println("Klijent konektovan");
		InputStream in = clientSocket.getInputStream(); 
		out = new BufferedOutputStream(clientSocket.getOutputStream());
		StringBuilder sb = new StringBuilder();
		Pattern content_length,numbers, port_num;
		String content_length_pattern = "Content-Length:\\s\\d*";
		String number_pattern = "\\d+";
		String port_num_pattern = "Server-Socket:\\s\\d*";
		Matcher matcher;
		content_length = Pattern.compile(content_length_pattern);
		numbers = Pattern.compile(number_pattern);
		port_num = Pattern.compile(port_num_pattern);
		
		
		//dohvatanje hedera i payloada
		ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int count, control_count=0, offset=0, bodyLength = 1000000;
		byte[] buffer = new byte[2048];
		boolean eohFound = false;
		boolean notPut = true;
		while ((count = in.read(buffer)) != -1)
		{
			offset = 0;
		    if(!eohFound){
		        String string = new String(buffer, 0, count);
		        int indexOfEOH = string.indexOf("\r\n\r\n");
		        if(indexOfEOH != -1) {
		        	 count = count-indexOfEOH-4;
		             offset = indexOfEOH+4;
		             eohFound = true;
		            String ss = string.substring(0, indexOfEOH);
		            sb.append(ss);
		            eohFound = true;
		            
		            matcher = content_length.matcher(sb.toString());
					if(matcher.find()) {
					String content = matcher.group();
					
					matcher = numbers.matcher(content);
					matcher.find();
					
					String duzina = matcher.group();
					
					bodyLength = Integer.parseInt(duzina);
					notPut = false;
					}
		            
		        } else {
		        	sb.append(string);
		            count = 0;
		        }
		    }
		    
		    control_count+=count;
		    buf.write(buffer, offset, count);
		    buf.flush();
		    if(notPut)break;
		    if (!notPut && control_count >= bodyLength)break;
		}
		byte[] data = buf.toByteArray();
		buf.close();

        String s = sb.toString();
        if(s.equals("")) return;
        parser = new ServerHttpParser(s, data);
        parser.resolve();
        req_t = parser.getType();
        String request = parser.getRequest(); 
        if(req_t == null)return;
        
        ServerRequestHandler handler = new ServerRequestHandler(req_t, out, request, clientSocket, parser);
        handler.handle();
        in.close();
        
        if(req_t == req_type.GET) {
        	int server_port = 0;
        	matcher = port_num.matcher(sb.toString());
			if(matcher.find()) {
			String content = matcher.group();
				
			matcher = numbers.matcher(content);
			matcher.find();
			String duzina = matcher.group();
			server_port = Integer.parseInt(duzina);
        	
			}
        	CentralniServer.addPodserver(clientSocket.getInetAddress(), server_port );
        	if(handler.found)CentralniServer.AzurirajMapu(clientSocket.getInetAddress(), server_port , request);
        }
        	
        else if (req_t == req_type.DELETE||req_t == req_type.PUT) {
        	CentralniServer.AzurirajPodservere(request);
        }
        
        
		}catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public ServerThread(Socket s){
		this.clientSocket = s;
	}

}
