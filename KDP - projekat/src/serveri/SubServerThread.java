package serveri;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SubServerThread implements Runnable {
	
	Socket clientSocket; 
	SubServerHttpParser.req_type req_t;
	SubServerHttpParser parser;
	
	public SubServerThread(Socket client) {
		this.clientSocket = client;
	}

	@Override
	public void run() {
		try {
			
			BufferedOutputStream out;
			
			System.out.println("Klijent konektovan");
			InputStream in = clientSocket.getInputStream(); 
			out = new BufferedOutputStream(clientSocket.getOutputStream());
			StringBuilder sb = new StringBuilder();
			Pattern content_length,numbers;
			String content_length_pattern = "Content-Length:\\s\\d*";
			String number_pattern = "\\d+";
			Matcher matcher;
			content_length = Pattern.compile(content_length_pattern);
			numbers = Pattern.compile(number_pattern);
			
			
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
	        parser = new SubServerHttpParser(s);
	        parser.resolve();
	        req_t = parser.getType();
	        String request = parser.getRequest(); 
	        if(req_t == null)return;
	        
	        SubServerHttpHandler handler = new SubServerHttpHandler(req_t, out, request, clientSocket, parser);
	        handler.handle();
	        in.close();
			
			
		}catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
	}
}
