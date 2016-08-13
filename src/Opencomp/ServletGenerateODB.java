package Opencomp;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.hsqldb.jdbcDriver;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.gson.Gson;

class Pupil {
    String id;
    String name;
    String first_name;
    String birthday;
    List<Level> levels;
    
    public String getBirthday(){
    	Date date = javax.xml.bind.DatatypeConverter.parseDateTime(this.birthday).getTime();
    	DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
    	return formatter.format(date);
    }
}

class Classroom {
	String code;
    List<Pupil> pupils;
}

class Level {
    String title;
}

@SuppressWarnings("serial")
public class ServletGenerateODB extends HttpServlet {
	String apiUrl;
	String json;

	public void init(ServletConfig config) throws ServletException {
	    super.init(config);
	    apiUrl = config.getInitParameter("apiUrl");
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        //Only GET method is allowed
		response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String apikey = request.getParameter("apikey");
        String classroom_id = request.getParameter("classroom_id");

        if(apikey == null || classroom_id == null){
        	//Some required parameters are missing
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED,"Vous devez spécifier les paramètres apikey et classroom_id");
            return;
        }
       
        json = readUrl(new URL(apiUrl + "/classrooms/view/" + classroom_id + ".json?api_key=" + apikey),response);
        
    	switch(json){
	    	case "504":
	    		response.sendError(HttpServletResponse.SC_GATEWAY_TIMEOUT);
	        	return;
	    	case "401":
	    		response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
	        	return;
	    	case "403":
	    		response.sendError(HttpServletResponse.SC_FORBIDDEN);
        		return;
	    	case "404":
	    		response.sendError(HttpServletResponse.SC_NOT_FOUND);
        		return;
	    		
    	}
        
         	
        if(isJSONValid(json)){
        	
        	//Parsing our JSON to the Classroom class
            Gson gson = new Gson();
            Classroom classroom = gson.fromJson(json, Classroom.class);

            //Get servlet context (used to get path of Dynamic Web Project)
            ServletContext cntxt = this.getServletContext();
            
            //Creating directory for associated classroom
            String workdir = cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id;
            File dir = new File(workdir);
        	deleteDir(dir);
        	dir.mkdir();
        	
        	//Unzipping files from ODB file into previously created directory
            try {
                ZipFile zipFile = new ZipFile(cntxt.getRealPath("/WEB-INF/")+"/"+"master.odb");
                zipFile.extractAll(workdir);
            } catch (ZipException e) {
                e.printStackTrace();
            }

            //Renaming HSQLDB files to be able to open them outside LibreOffice
            renameFile(workdir+"/database/backup",workdir+"/database/db.backup");
            renameFile(workdir+"/database/data",workdir+"/database/db.data");
            renameFile(workdir+"/database/properties",workdir+"/database/db.properties");
            renameFile(workdir+"/database/script",workdir+"/database/db.script");

            //Create our JDBC connection based on the temp filename used above
            Connection con = null; //Database objects
            Statement statement = null;
            new jdbcDriver(); //Instantiate the jdbcDriver from HSQL
            try {
                con = DriverManager.getConnection("jdbc:hsqldb:file:" + workdir + "/database/db;shutdown=true", "SA", "");
            } catch (SQLException e) {
                e.printStackTrace();
            }

            //Our master.odb file contain one record in our unique table.
            //We need to do that, otherwise, data file is not create
            try {
            	int i = 1;
                for (Pupil pupil : classroom.pupils)
                {
                	if(i == 1){
                		//We update first line for first iteration
                		statement = con.createStatement();
                        statement.executeUpdate("UPDATE \"Eleves\" SET \"Code\" = '*00"+pupil.id+"*', \"Nom\" = '"+pupil.name+"', \"Prénom\" = '"+pupil.first_name+"', \"naiss\" = '"+pupil.getBirthday()+"', \"level\" = '"+pupil.levels.get(0).title+"' WHERE \"ID\" = 1");
                	}else{
                		//Adding the next records
                		statement = con.createStatement();
                        statement.executeUpdate("INSERT INTO \"Eleves\" VALUES ('"+i+"', '*00"+pupil.id+"*', '"+pupil.name+"', '"+pupil.first_name+"', '"+pupil.getBirthday()+"', '"+pupil.levels.get(0).title+"')");
                	}
                	i++;
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            //Close all the database objects
            try {
                statement.close();
                con.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            //Renaming HSQLDB files to be able to open them in LibreOffice
            renameFile(workdir+"/database/db.backup",workdir+"/database/backup");
            renameFile(workdir+"/database/db.data",workdir+"/database/data");
            renameFile(workdir+"/database/db.properties",workdir+"/database/properties");
            renameFile(workdir+"/database/db.script",workdir+"/database/script");

            try {
                // Initiate ZipFile object with the path/name of the zip file.
                ZipFile zipFile = new ZipFile(workdir + ".odb");

                // Initiate Zip Parameters which define various properties such
                // as compression method, etc.
                ZipParameters parameters_mimetype = new ZipParameters();
                parameters_mimetype.setCompressionMethod(Zip4jConstants.COMP_STORE);
                parameters_mimetype.setCompressionLevel(Zip4jConstants.COMP_STORE);

                ZipParameters parameters = new ZipParameters();
                parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
                parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

                // Add folder to the zip file
                zipFile.addFile(new File(workdir + "/mimetype"), parameters_mimetype);
                zipFile.addFolder(workdir + "/Configurations2", parameters);
                zipFile.addFile(new File(workdir + "/content.xml"), parameters);
                zipFile.addFolder(workdir + "/database", parameters);
                zipFile.addFolder(workdir + "/forms", parameters);
                zipFile.addFolder(workdir + "/META-INF", parameters);
                zipFile.addFolder(workdir + "/reports", parameters);
                zipFile.addFile(new File(workdir + "/settings.xml"), parameters);

            } catch (ZipException e) {
                e.printStackTrace();
            }

            deleteDir(new File(workdir));

            response.setContentType("application/vnd.oasis.opendocument.database");
            response.setHeader("Content-Disposition", "filename=\""+classroom_id+".odb\"");
            String fName = "/WEB-INF/"+classroom_id+".odb";
            BufferedInputStream buf = new BufferedInputStream(cntxt.getResourceAsStream(fName));

            //Returning ODB file
            ServletOutputStream myOut = response.getOutputStream();
            int readBytes;

            //Reading from generated file and redirecting output to ServletOutputStream
            while ((readBytes = buf.read()) != -1) {
                myOut.write(readBytes);
            }
        }else{
        	//Malformed JSON
        	response.sendError(HttpServletResponse.SC_BAD_GATEWAY);
    		return;
        }
    }

    private static boolean isJSONValid(String test) {
        try {
            new JSONObject(test);
        } catch (JSONException ex) {
            try {
                new JSONArray(test);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }

    private static String readUrl(URL url, HttpServletResponse response)
    {
        StringBuffer sb = new StringBuffer();
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15 * 1000);
            BufferedReader br =  new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ( (line = br.readLine() ) != null)
            {
                sb.append(line);
            }
        }
        catch (SocketTimeoutException ste){
        	sb.append("504");
        	ste.printStackTrace();
        }
        catch (MalformedURLException mue){
            mue.printStackTrace();
        } 
        catch (IOException ioe){
        	try {
				sb.append(Integer.toString(conn.getResponseCode()));
			} catch (IOException e) {
				e.printStackTrace();
			}
            ioe.printStackTrace();
        } 
        finally {

        }
        return sb.toString();
    }

    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    private boolean renameFile(String from, String to) throws IOException{
        File file = new File(from);

        // File (or directory) with new name
        File file2 = new File(to);
        if(file2.exists()) throw new java.io.IOException("file exists");

        // Rename file (or directory)
        boolean success = file.renameTo(file2);

        return success;
    }
}
