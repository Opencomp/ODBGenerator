package Opencomp;

import com.google.gson.Gson;
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

import org.hsqldb.jdbcDriver;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.*;
import java.util.List;

class Pupil {
    String id;
    String name;
    String first_name;
    String birthday;
    String level;
}

class Classroom {
    String error;
    List<Pupil> pupils;
}

public class ServletGenerateODB extends HttpServlet {
	String apiUrl;
	
    /**
	 * 
	 */
	private static final long serialVersionUID = -1589756158656851734L;

	public void init(ServletConfig config) throws ServletException {
	    super.init(config);
	    apiUrl = config.getInitParameter("apiUrl");
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String apikey = request.getParameter("apikey");
        String classroom_id = request.getParameter("classroom_id");

        if(apikey == null || classroom_id == null){
            response.sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
            return;
        }

        String json = readUrl(new URL(apiUrl + "/classrooms/getJson/" + apikey + "/" + classroom_id));

        if(isJSONValid(json)){
            Gson gson = new Gson();
            Classroom classroom = gson.fromJson(json, Classroom.class);
            
            if(classroom.error.equals("INVALID_APIKEY")){
            	response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            
            if(classroom.error.equals("UNKNOWN_CLASSROOM")){
            	response.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            ServletContext cntxt = this.getServletContext();

            File dir = new File(cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id);
            try{
            	deleteDir(dir);
            	dir.mkdir();
            }catch (Exception e){
            	e.printStackTrace();
            }
            
            try {
                ZipFile zipFile = new ZipFile(cntxt.getRealPath("/WEB-INF/")+"/"+"master.odb");
                zipFile.extractAll(cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id);
            } catch (ZipException e) {
                e.printStackTrace();
            }

            //On renomme les fichiers HSQLDB pour pouvoir les ouvrir hors de LibreOffice
            renameFile(cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/backup",cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/db.backup");
            renameFile(cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/data",cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/db.data");
            renameFile(cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/properties",cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/db.properties");
            renameFile(cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/script",cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/db.script");

            //Create our JDBC connection based on the temp filename used above
            Connection con = null; //Database objects
            Statement statement = null;
            new jdbcDriver(); //Instantiate the jdbcDriver from HSQL
            try {
                con = DriverManager.getConnection("jdbc:hsqldb:file:" + cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id + "/database/db;shutdown=true", "SA", "");
            } catch (SQLException e) {
                e.printStackTrace();
            }

            try {
            	int i = 1;
                for (Pupil pupil : classroom.pupils)
                {
                	if(i == 1){
                		statement = con.createStatement();
                        statement.executeUpdate("UPDATE \"Eleves\" SET \"Code\" = '"+pupil.id+"', \"Nom\" = '"+pupil.name+"', \"Prénom\" = '"+pupil.first_name+"', \"naiss\" = '"+pupil.birthday+"', \"level\" = '"+pupil.level+"' WHERE \"ID\" = 1");
                	}else{
                		statement = con.createStatement();
                        statement.executeUpdate("INSERT INTO \"Eleves\" VALUES ('"+i+"', '"+pupil.id+"', '"+pupil.name+"', '"+pupil.first_name+"', '"+pupil.birthday+"', '"+pupil.level+"')");
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

            //On renomme les fichiers HSQLDB pour pouvoir les ouvrir dans LibreOffice
            renameFile(cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/db.backup",cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/backup");
            renameFile(cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/db.data",cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/data");
            renameFile(cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/db.properties",cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/properties");
            renameFile(cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/db.script",cntxt.getRealPath("/WEB-INF/")+"/"+classroom_id+"/database/script");

            try {
                // Initiate ZipFile object with the path/name of the zip file.
                ZipFile zipFile = new ZipFile(cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id + ".odb");

                // Initiate Zip Parameters which define various properties such
                // as compression method, etc.
                ZipParameters parameters_mimetype = new ZipParameters();
                parameters_mimetype.setCompressionMethod(Zip4jConstants.COMP_STORE);
                parameters_mimetype.setCompressionLevel(Zip4jConstants.COMP_STORE);

                ZipParameters parameters = new ZipParameters();
                parameters.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
                parameters.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);

                // Add folder to the zip file
                zipFile.addFile(new File(cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id + "/mimetype"), parameters_mimetype);
                zipFile.addFolder(cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id + "/Configurations2", parameters);
                zipFile.addFile(new File(cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id + "/content.xml"), parameters);
                zipFile.addFolder(cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id + "/database", parameters);
                zipFile.addFolder(cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id + "/forms", parameters);
                zipFile.addFolder(cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id + "/META-INF", parameters);
                zipFile.addFolder(cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id + "/reports", parameters);
                zipFile.addFile(new File(cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id + "/settings.xml"), parameters);

            } catch (ZipException e) {
                e.printStackTrace();
            }

            deleteDir(new File(cntxt.getRealPath("/WEB-INF/")+"/"+ classroom_id));


            response.setContentType("application/vnd.oasis.opendocument.database");
            response.setHeader("Content-Disposition", "filename=\""+classroom_id+".odb\"");
            String fName = "/WEB-INF/"+classroom_id+".odb";
            BufferedInputStream buf = new BufferedInputStream(cntxt.getResourceAsStream(fName));

            //On renvoie le fichier .odb généré
            ServletOutputStream myOut = response.getOutputStream();
            int readBytes;

            //On lit depuis le fichier généré et on dirige la sortie vers la ServletOutputStream
            while ((readBytes = buf.read()) != -1) {
                myOut.write(readBytes);
            }
        }
    }

    public static boolean isJSONValid(String test) {
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

    public static String readUrl(URL url)
    {
        StringBuffer sb = new StringBuffer();
        try {
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            BufferedReader br =  new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = null;
            while ( (line = br.readLine() ) != null)
            {
                sb.append(line);
            }
        } catch (MalformedURLException mue)
        {
            mue.printStackTrace();
        } catch (IOException ioe)
        {
            ioe.printStackTrace();
        } finally {

        }
        return sb.toString();
    }

    public static boolean deleteDir(File dir) {
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

    public boolean renameFile(String from, String to) throws IOException{
        File file = new File(from);

        // File (or directory) with new name
        File file2 = new File(to);
        if(file2.exists()) throw new java.io.IOException("file exists");

        // Rename file (or directory)
        boolean success = file.renameTo(file2);

        return success;
    }
}
