package serveri;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ServerHttpParser {
	
	//podaci
	private String ulaz, file;
	private String request, file_path;
	private byte[] podaci;
	BufferedReader in;
	enum req_type {GET, PUT, DELETE, OK};
	
	private Pattern get, put, delete, http_spec, request_type, content_length, numbers, file_name, response_ok, path;
	private Matcher matcher;
	
	//regexi
	private String request_type_pattern = "((^GET\\s/\\s*|^PUT\\s/\\s*|^DELETE\\s/\\s*).+\\s+HTTP/1.1)|HTTP/1.1\\s200\\sOK";
	private String get_pattern = "^GET\\s/\\s*";
	private String put_pattern = "^PUT\\s/\\s*";
	private String delete_pattern = "^DELETE\\s/\\s*";
	private String http_spec_pattern ="\\s+HTTP/1.1$";
	private String content_length_pattern = "Content-Length:\\s\\d*";
	private String number_pattern = "\\d+";
	private String file_name_pattern ="(.*/)*";
	private String response_ok_pattern = "HTTP/1.1\\s200\\sOK";
	private String path_pattern = "/.*$";
	
	private req_type req_t;
	
	
	public void resolve() {
		matcher = request_type.matcher(ulaz);
		String temp = null;
		if(matcher.find())temp = matcher.group();
		else return;
		
		matcher = get.matcher(temp);
		if(matcher.find())
		{	
			request = matcher.replaceFirst("");
			matcher = http_spec.matcher(request);
			request = matcher.replaceFirst("");
			System.err.println(request);
			req_t = req_type.GET;
			matcher = file_name.matcher(request);
			file = matcher.replaceFirst("");
			return;
		}
		
		matcher = put.matcher(temp);
		if(matcher.find()) {
			request = matcher.replaceFirst("");
			matcher = http_spec.matcher(request);
			request = matcher.replaceFirst("");
			System.err.println(request);
			matcher = path.matcher(request);
			matcher.find();
			file_path = matcher.replaceFirst("");
			System.err.println(file_path);
			
			req_t = req_type.PUT;
			
			matcher = content_length.matcher(ulaz);
			matcher.find();
			String content = matcher.group();
			
			matcher = numbers.matcher(content);
			matcher.find();
			
			String duzina = matcher.group();
			
			int length = Integer.parseInt(duzina);
			return;
		}
		
		matcher = delete.matcher(temp);
		if(matcher.find()) {
			request = matcher.replaceFirst("");
			matcher = http_spec.matcher(request);
			request = matcher.replaceFirst("");
			System.err.println(request);
			req_t = req_type.DELETE;
			return;
		}
		
		matcher = response_ok.matcher(temp);
		if(matcher.find()) {
			request = matcher.replaceFirst("");
			matcher = http_spec.matcher(request);
			request = matcher.replaceFirst("");
			System.err.println(request);

			matcher = content_length.matcher(ulaz);
			if(!matcher.find())return;//ako nije response na get request nece imati content length polje
			
			String content = matcher.group();
			
			matcher = numbers.matcher(content);
			
			matcher.find();
			
			
			req_t = req_type.OK;
			
			String duzina = matcher.group();
			int length = Integer.parseInt(duzina);
		}
		
	}
	

	public req_type getType() {
		return this.req_t;
	}
	
	public String getRequest() {
		return this.request;
	}
	
	public byte[] getData() {
		return this.podaci;
	}
	
	public String getFileName() {
		return this.file;
	}
	
	public String getFilePath() {
		return this.file_path;
	}
	
	public ServerHttpParser(String s, byte[]podaci) {
		this.ulaz = s;
		get = Pattern.compile(get_pattern);
		put = Pattern.compile(put_pattern);
		delete = Pattern.compile(delete_pattern);
		http_spec = Pattern.compile(http_spec_pattern);
		request_type = Pattern.compile(request_type_pattern);
		content_length = Pattern.compile(content_length_pattern);
		numbers = Pattern.compile(number_pattern);
		file_name = Pattern.compile(file_name_pattern);
		path = Pattern.compile(path_pattern);
		response_ok = Pattern.compile(response_ok_pattern);
		this.podaci = podaci;
		
	}
}
