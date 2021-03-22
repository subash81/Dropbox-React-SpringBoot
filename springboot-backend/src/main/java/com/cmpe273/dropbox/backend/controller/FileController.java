package com.cmpe273.dropbox.backend.controller;

import com.cmpe273.dropbox.backend.entity.Userfiles;
import com.cmpe273.dropbox.backend.entity.Userlog;
import com.cmpe273.dropbox.backend.entity.Users;
import com.cmpe273.dropbox.backend.service.FileService;
import com.cmpe273.dropbox.backend.service.StorageFactoryService;
import com.cmpe273.dropbox.backend.service.UserFilesService;
import com.cmpe273.dropbox.backend.service.UserLogService;
import com.cmpe273.dropbox.backend.service.UserService;
import com.google.api.gax.paging.Page;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.Acl.Role;
import com.google.cloud.storage.Acl.User;
import com.google.gson.Gson;
import com.cmpe273.dropbox.backend.utils.PailierHomomorphic;
import com.cmpe273.dropbox.backend.utils.StorageUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.util.Base64;
import com.google.api.client.http.InputStreamContent;
import java.util.*;


@Controller    // This means that this class is a Controller
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping(path="/files") // This means URL's start with /demo (after Application path)
public class FileController {
    @Autowired
    private FileService fileService;

    @Autowired
    private UserFilesService userFilesService;

    @Autowired
    private UserLogService userLogService;

    @Autowired
    private UserService userService;
    
    private static final String metadataEmail = "email";
    private static final String metadataEmailValue = "subashskumar@hmail.com";
    private Map<String, String> newMetadata;
    

    //Save the uploaded file to this folder
    private static String UPLOADED_FOLDER = /*System.getProperty("user.dir") + */"./public/uploads/";
    
    //private static String UPLOADED_FOLDER = /*System.getProperty("user.dir") + */"/Users/subashkumarsaladi/Desktop/testing_dropbox/";


    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
    public ResponseEntity<com.cmpe273.dropbox.backend.entity.Files> fileupload(@RequestParam("file") MultipartFile multipartFile,
                                                                               @RequestParam("fileparent") String fileparent, HttpSession session) throws JSONException, GeneralSecurityException {

        String email = (String) session.getAttribute("email");
        System.out.println("subashkumarsaladi controller ****");

        com.cmpe273.dropbox.backend.entity.Files newFile = new com.cmpe273.dropbox.backend.entity.Files();
        newMetadata = new HashMap<>();
        newMetadata.put(metadataEmail, email);
	    // SQL server changes start
		/*
		 * try {
		 * 
		 * String filepath = ""; if(!StringUtils.isEmpty(fileparent)){
		 * 
		 * filepath = fileparent+"/" + multipartFile.getOriginalFilename();
		 * 
		 * } else{
		 * 
		 * filepath = UPLOADED_FOLDER + email.split("\\.")[0] + "/" +
		 * multipartFile.getOriginalFilename();
		 * 
		 * }
		 * 
		 * byte[] bytes = multipartFile.getBytes(); Path path = Paths.get(filepath);
		 * Files.write(path, bytes);
		 * 
		 * 
		 * newFile.setFilename(multipartFile.getOriginalFilename());
		 * newFile.setFileparent(fileparent); newFile.setIsfile("T");
		 * newFile.setOwner(email); newFile.setSharedcount(0); newFile.setStarred("F");
		 * newFile.setFilepath(filepath);
		 * 
		 * fileService.uploadFile(newFile);
		 * 
		 * Userfiles userfiles = new Userfiles();
		 * 
		 * userfiles.setEmail(email); userfiles.setFilepath(filepath);
		 * 
		 * userFilesService.addUserFile(userfiles);
		 * 
		 * Userlog userlog = new Userlog();
		 * 
		 * userlog.setAction("File Upload"); userlog.setEmail(email);
		 * userlog.setFilename(multipartFile.getOriginalFilename());
		 * userlog.setFilepath(filepath); userlog.setIsfile("F");
		 * userlog.setActiontime(new Date().toString());
		 * 
		 * userLogService.addUserLog(userlog);
		 * 
		 * 
		 * } catch (IOException e) { e.printStackTrace();
		 * 
		 * return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);
		 * 
		 * }
		 */
	    // SQL server changes end
	    
        // GCS start
        //final Part filePart = request.getPart("myFile");
  		//String fileName = getFileName(filePart);
  	    // The ID of your GCP project
	    String projectId = "arboreal-height-273317";

	    // The ID of your GCS bucket
	    String bucketName = "project-sam";
	    
        try {      	 
        	String filepath = "";
            if(!StringUtils.isEmpty(fileparent)){

                filepath = fileparent+"/" + multipartFile.getOriginalFilename();

            }
            else{
            	//filepath = UPLOADED_FOLDER + email.split("\\.")[0] + "/" + multipartFile.getOriginalFilename();
                filepath =  email.split("\\.")[0] + "/" + multipartFile.getOriginalFilename();

            }

            System.out.println("upload file  " + filepath);
            
            /** uncomment this if you need to write file in selected folder
            byte[] bytes = multipartFile.getBytes();
            Path path = Paths.get(filepath);
            Files.write(path, bytes);**/
            

            newFile.setFilename(multipartFile.getOriginalFilename());
            System.out.println("upload file  filename " + newFile.getFilename());
            newFile.setFileparent(fileparent);
            newFile.setIsfile("T");
            newFile.setOwner(email);
            newFile.setSharedcount(0);
            newFile.setStarred("F");
            newFile.setFilepath(filepath);
    		 InputStream fileInputStreams = multipartFile.getInputStream();
    		//Random rp = new Random(); 
    		  //System.out.println("upload file  Random p " + rp.);
 			//Random rq = new Random();
 			  //System.out.println("upload file  Random q " + rq);
    		 PailierHomomorphic pailierHomomorphic = new PailierHomomorphic();
    		 //newFile.setP(rp);
    		 //newFile.setQ(rq);
    		 ByteArrayOutputStream bOutput = null;
    		 bOutput = pailierHomomorphic.encryptOriginalToCipher(pailierHomomorphic, fileInputStreams, newFile);
    		 System.out.println("upload file  bOutputStream "+bOutput); 
    		 InputStream fileInputStreamsFromPailier = new ByteArrayInputStream(bOutput.toByteArray());
    		 InputStream fileInputStreamsDB = multipartFile.getInputStream();
  		    
             //FileInputStream fileInputStream = new FileInputStream("/Users/satyasameeradevu/eclipse-workspace/IB-PRE/target/IB-PRE-0.0.1-SNAPSHOT/uploads/keerthi.txt");

             Credentials credentials = GoogleCredentials.fromStream(new FileInputStream("/Users/subashkumarsaladi/Desktop/arboreal-height-273317-57289372ded5.json"));
				/*// original working file upload before AES encryption
				 * Storage storage =
				 * StorageOptions.newBuilder().setProjectId(projectId).setCredentials(
				 * credentials).build().getService();
				 * 
				 * BlobInfo blobInfo = storage.create( BlobInfo .newBuilder(bucketName,
				 * filepath) .setAcl(new ArrayList<>(Arrays.asList(Acl.of(User.ofAllUsers(),
				 * Role.OWNER)))) .build(), fileInputStreams);
				 * 
				 * String fileUrl = blobInfo.getMediaLink(); newFile.setFilepath(fileUrl);
				 * System.out.println("Download file  " + fileUrl);
				 */
             
             //updating below line to new output stream
             InputStreamContent mediaContent = new InputStreamContent("text/plain", fileInputStreamsFromPailier);
             com.google.api.services.storage.model.StorageObject content = new com.google.api.services.storage.model.StorageObject();
             content.setMetadata(newMetadata);
             com.google.api.services.storage.Storage storage = StorageFactoryService.getService();
             
             com.google.api.services.storage.Storage.Objects.Insert insertObject =
                 storage.objects().insert(bucketName, content, mediaContent).setName(newFile.getFilename());
             // The client library's default gzip setting may cause objects to be stored with gzip encoding,
             // which can be desirable in some circumstances but has some disadvantages as well, such as
             // making it difficult to read only a certain range of the original object.
             insertObject.getMediaHttpUploader().setDisableGZipContent(true);

             //insertObject.set(metadataEmail, "subash@gmail.com");
             //insertObject.setUserProject(email);
             //insertObject.set("email", metadataEmailValue);
             //insertObject.set(newMetadata);
             KeyGenerator keyGen = KeyGenerator.getInstance("AES");
             keyGen.init(256);
             SecretKey skey = keyGen.generateKey();
             String encryption_key = Base64.encodeBase64String(skey.getEncoded());
             MessageDigest digest = MessageDigest.getInstance("SHA-256");
             String encryption_key_sha256 = Base64.encodeBase64String(digest.digest(skey.getEncoded()));
             newFile.setEncryption_key(encryption_key);
             newFile.setEncryption_key_sha256(encryption_key_sha256);

             // Now set the CSEK headers
             final  com.google.api.client.http.HttpHeaders httpHeaders = new com.google.api.client.http.HttpHeaders();
             httpHeaders.set("x-goog-encryption-algorithm", "AES256");
             httpHeaders.set("x-goog-encryption-key", encryption_key);
             httpHeaders.set("x-goog-encryption-key-sha256", encryption_key_sha256);
             
             insertObject.setRequestHeaders(httpHeaders);

             try {
               insertObject.execute();
               System.out.println("insertObject email: " + insertObject.get(metadataEmail));
             } catch (GoogleJsonResponseException e) {
               System.out.println("Error uploading: " + e.getContent());
               System.exit(1);
             }
              newFile.setFileContent(saveFileContent(fileInputStreamsDB, "UTF-8"));
              fileService.uploadFile(newFile);
    		  
    		  Userfiles userfiles = new Userfiles();
    		  
    		  userfiles.setEmail(email); userfiles.setFilepath(filepath);
    		  
    		  userFilesService.addUserFile(userfiles);
    		  
    		  Userlog userlog = new Userlog();
    		  
    		  userlog.setAction("File Upload"); userlog.setEmail(email);
    		  userlog.setFilename(multipartFile.getOriginalFilename());
    		  userlog.setFilepath(filepath); userlog.setIsfile("F");
    		  userlog.setActiontime(new Date().toString());
    		  
    		  userLogService.addUserLog(userlog);
             //response.getOutputStream().println("</td></table>");
             //response.getOutputStream().println("<p>Upload another file <a href=\"http://localhost:8080/IB-PRE\">here</a>.</p>");
         } catch (IOException e) {
           // e.printStackTrace();
            e.printStackTrace();
            System.out.println("exception in servlet processing ");
            //response.getOutputStream().println("<p>Sorry, Your upload to Google Cloud Server failed. Please try again !!</p>");
                return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);

        } 
     // GCS end

        return new ResponseEntity<com.cmpe273.dropbox.backend.entity.Files>(newFile, HttpStatus.OK);
    }
    
    
    public static String saveFileContent(
    		   InputStream fis,
    		   String          encoding) throws IOException
    		 {
    		   try( BufferedReader br =
    		           new BufferedReader( new InputStreamReader(fis, encoding )))
    		   {
    		      StringBuilder sb = new StringBuilder();
    		      String line;
    		      while(( line = br.readLine()) != null ) {
    		         sb.append( line );
    		         sb.append( '\n' );
    		      }
    		      return sb.toString();
    		   }
    		   
    		}

    @GetMapping(path = "/getfolderfiles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<com.cmpe273.dropbox.backend.entity.Files>> getFilesInFolder(@RequestParam String filepath) {

      //  JSONObject jObject = new JSONObject(filepath);

        List<com.cmpe273.dropbox.backend.entity.Files> filesList = fileService.getFileByFileparent(filepath);

        return new ResponseEntity(filesList, HttpStatus.OK);
    }

    @GetMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<com.cmpe273.dropbox.backend.entity.Files>> getUserFiles(HttpSession session) throws FileNotFoundException, IOException {

        String email = (String) session.getAttribute("email");
        if(email==null){
            return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);
        }
        //String projectId = "arboreal-height-273317";

	    // The ID of your GCS bucket
	    //String bucketName = "project-sam";
	    //Credentials credentials = GoogleCredentials.fromStream(new FileInputStream("/Users/subashkumarsaladi/Desktop/arboreal-height-273317-57289372ded5.json"));
        //Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
        //Bucket bucket = storage.get(bucketName);
        //Page<Blob> blobs = bucket.list();
///List<String> filesList = new ArrayList<String>();
        //for (Blob blob : blobs.iterateAll()) {
          //System.out.println(blob.getName());
          //System.out.println("Metadata "+blob.getStorage());
             // filesList.add(blob.getName());
       // }
        
        
        

        List<String> objectsList = new ArrayList<>();
            
   		try {
   			String bucketNameNew = "project-sam";		 
   	   		com.google.api.services.storage.Storage storageNew = StorageFactoryService.getService();
   	   	objectsList = listObjects(storageNew, bucketNameNew, email);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
                    
        
        //Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        
        //Page<Blob> blobs = bucket.list();

        //for (Blob blob : blobs.iterateAll()) {
         // System.out.println(blob.getName());
        //}
        
        
		
		  List<Userfiles> userFilesList = userFilesService.getUserFilesByEmail(email);
		  
		  List<com.cmpe273.dropbox.backend.entity.Files> filesList = new ArrayList<>();
		  for (Userfiles userfiles : userFilesList) {
		  
		 com.cmpe273.dropbox.backend.entity.Files file =
		 fileService.getFileByFilepath(userfiles.getFilepath(), ""); if(file!=null)
		  filesList.add(file); }

		  Set<com.cmpe273.dropbox.backend.entity.Files> finalFilesList = new HashSet<>();
		  for(com.cmpe273.dropbox.backend.entity.Files dbFile : filesList) {
			  if(objectsList.contains(dbFile.getFilename())) {
				  finalFilesList.add(dbFile);
			  } 
		  }
		 

        return new ResponseEntity(finalFilesList, HttpStatus.OK);
    }

    /** SQL server start
    @PostMapping(path = "/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteFile(@RequestBody com.cmpe273.dropbox.backend.entity.Files file, HttpSession session) throws JSONException {
        System.out.println(file.getFilepath());

        String email = (String) session.getAttribute("email");

        if(email==null){
            return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);
        }

        String filepath = UPLOADED_FOLDER + file.getOwner().split("\\.")[0] + "/" + file.getFilename();
        System.out.println("file controller  delete method filepath **** "+filepath);
        Path path = Paths.get(filepath);
        System.out.println("file controller  delete method path **** "+path);

        if (file.getOwner().equals(email)) {

            try {
                Files.delete(path);

                userFilesService.deleteUserFilesByFilepath(file.getFilepath());
                fileService.deleteFile(file.getFilepath());


                Userlog userlog = new Userlog();


                userlog.setEmail(email);
                userlog.setFilename(file.getFilename());
                userlog.setFilepath(filepath);
                if(file.getIsfile().equals("T"))
                    userlog.setAction("File Delete");

                else
                    userlog.setAction("Folder Delete");

                userlog.setActiontime(new Date().toString());
                userlog.setIsfile(file.getIsfile());
                userLogService.addUserLog(userlog);

            } catch (IOException e) {
                e.printStackTrace();

                    return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);

            }
        } else {

            userFilesService.deleteUserFilesByEmailAndFilepath(file.getFilepath(), email);
            fileService.updateSharedCount(file.getFilepath(), file.getSharedcount() - 1);

        }

        return new ResponseEntity(null, HttpStatus.OK);

    }SQL server end **/
    
    @PostMapping(path = "/delete", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> deleteFile(@RequestBody com.cmpe273.dropbox.backend.entity.Files file, HttpSession session) throws JSONException {
        System.out.println(file.getFilepath());

        String email = (String) session.getAttribute("email");

        if(email==null){
            return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);
        }

        //String filepath = UPLOADED_FOLDER + file.getOwner().split("\\.")[0] + "/" + file.getFilename();
        //filepath =  email.split("\\.")[0] + "/" + multipartFile.getOriginalFilename();
        String filepath = email.split("\\.")[0] + "/" + file.getFilename();
        System.out.println("file controller  delete method filepath **** "+filepath);
        Path path = Paths.get(filepath);
        
        
        // The ID of your GCP project
	    String projectId = "arboreal-height-273317";

	    // The ID of your GCS bucket
	    String bucketName = "project-sam";

        // The ID of your GCS object
        // String objectName = "your-object-name";
        
        

        if (file.getOwner().equals(email)) {

            try {
            	//String bucketName = "project-sam";		 
           		com.google.api.services.storage.Storage storage = StorageFactoryService.getService();
           		deleteObject(storage, bucketName, file.getFilename() , file.getEncryption_key(), file.getEncryption_key_sha256());
            	//Credentials credentials = GoogleCredentials.fromStream(new FileInputStream("/Users/subashkumarsaladi/Desktop/arboreal-height-273317-57289372ded5.json"));
                //Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
                //storage.delete(bucketName, filepath);
                //returns boolean, based on it, display message to user
                System.out.println("file controller  GCS delete method done **** ");
                //Files.delete(path);

                //userFilesService.deleteUserFilesByFilepath(file.getFilepath());
                //fileService.deleteFile(file.getFilepath());


                Userlog userlog = new Userlog();


                userlog.setEmail(email);
                userlog.setFilename(file.getFilename());
                userlog.setFilepath(filepath);
                if(file.getIsfile().equals("T"))
                    userlog.setAction("File Delete");

                else
                    userlog.setAction("Folder Delete");

                userlog.setActiontime(new Date().toString());
                userlog.setIsfile(file.getIsfile());
                userLogService.addUserLog(userlog);
                
                

            } catch (IOException e) {
                e.printStackTrace();

                    return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);

            } catch (Exception e) {         	 
                e.printStackTrace();
                System.out.println("exception in servlet processing ");
                //response.getOutputStream().println("<p>Sorry, Your upload to Google Cloud Server failed. Please try again !!</p>");
            }
        } else {

            userFilesService.deleteUserFilesByEmailAndFilepath(file.getFilepath(), email);
            fileService.updateSharedCount(file.getFilepath(), file.getSharedcount() - 1);

        }

        return new ResponseEntity(null, HttpStatus.OK);

    }

    @PostMapping(path = "/sharefile", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> shareFile(@RequestBody String data, HttpSession session) throws JSONException {

        JSONObject jObject = new JSONObject(data);
        Gson gson = new Gson();
        JSONObject filedata = (JSONObject) jObject.get("filedata");
        com.cmpe273.dropbox.backend.entity.Files file = gson.fromJson(filedata.toString(), com.cmpe273.dropbox.backend.entity.Files.class);
        //String shareEmail = jObject.getString("shareEmail");

        //Users user = userService.getUserDetails(shareEmail);

        //if(user==null){

               // return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);

        //}

        String email = (String) session.getAttribute("email");

        if(email==null){
            return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);
        }

		/*// original code for sharefile
		 * Userfiles userfiles = new Userfiles();
		 * 
		 * userfiles.setEmail(shareEmail); userfiles.setFilepath(file.getFilepath());
		 * 
		 * userFilesService.addUserFile(userfiles);
		 * 
		 * fileService.updateSharedCount(file.getFilepath(), file.getSharedcount() + 1);
		 * 
		 * 
		 * Userlog userlog = new Userlog();
		 * 
		 * userlog.setEmail(email); userlog.setFilename(file.getFilename());
		 * userlog.setFilepath(file.getFilepath()); if(file.getIsfile().equals("T"))
		 * userlog.setAction("File shared with "+shareEmail);
		 * 
		 * else userlog.setAction("Folder shared with "+shareEmail);
		 * 
		 * userlog.setActiontime(new Date().toString());
		 * userlog.setIsfile(file.getIsfile()); userLogService.addUserLog(userlog);
		 */
        
        
        try {
   		 String bucketName = "project-sam";		 
   		com.google.api.services.storage.Storage storage = StorageFactoryService.getService();

     
            
            InputStream objectData =
                    downloadObject(storage, bucketName, file.getFilename() , file.getEncryption_key(), file.getEncryption_key_sha256());
                
            PailierHomomorphic pailierHomomorphic = new PailierHomomorphic();
            ByteArrayOutputStream bOutput=null;
            bOutput = pailierHomomorphic.decryptOriginalToCipher(pailierHomomorphic, objectData, file);
            System.out.println("download file  bOutputStream "+bOutput); 
   		 InputStream fileInputStreamsDecryptedFromPailier = new ByteArrayInputStream(bOutput.toByteArray());
            StorageUtils.readStream(fileInputStreamsDecryptedFromPailier,file.getFilename());
            
            
            //response.getOutputStream().println("<p>Thanks for uploading! Here are the list of files you uploaded in Google Cloud Server:</p>");
   			
             //response.getOutputStream().println("<p>Upload another file <a href=\"http://localhost:8080/gcs\">here</a>.</p>");
         } catch (Exception e) {         	 
             e.printStackTrace();
             System.out.println("exception in servlet processing ");
             //response.getOutputStream().println("<p>Sorry, Your upload to Google Cloud Server failed. Please try again !!</p>");
         }
   		System.out.println("File downloaded at client successfully");

        return new ResponseEntity(null, HttpStatus.OK);

    }
    
    public static InputStream downloadObject(
    		com.google.api.services.storage.Storage storage,
    	      String bucketName,
    	      String objectName,
    	      String base64CseKey,
    	      String base64CseKeyHash)
    	      throws Exception {

    	    // Set the CSEK headers
    	    final com.google.api.client.http.HttpHeaders httpHeaders = new com.google.api.client.http.HttpHeaders();
    	    httpHeaders.set("x-goog-encryption-algorithm", "AES256");
    	    httpHeaders.set("x-goog-encryption-key", base64CseKey);
    	    httpHeaders.set("x-goog-encryption-key-sha256", base64CseKeyHash);

    	    com.google.api.services.storage.Storage.Objects.Get getObject = storage.objects().get(bucketName, objectName);
    	    //storage.objects().

    	    // If you're using AppEngine, turn off setDirectDownloadEnabled:
    	    //      getObject.getMediaHttpDownloader().setDirectDownloadEnabled(false);

    	    getObject.setRequestHeaders(httpHeaders);
    	    //.get(metadataEmail);
    	    //System.out.println("email downloading: " +  getObject.get(metadataEmail));

    	    try {
    	      return getObject.executeMediaAsInputStream();
    	    } catch (GoogleJsonResponseException e) {
    	      System.out.println("Error downloading: " + e.getContent());
    	      System.exit(1);
    	      return null;
    	    }
    	  }
    
    public static void deleteObject(
    		com.google.api.services.storage.Storage storage,
    	      String bucketName,
    	      String objectName,
    	      String base64CseKey,
    	      String base64CseKeyHash)
    	      throws Exception {

    	    // Set the CSEK headers
    	    final com.google.api.client.http.HttpHeaders httpHeaders = new com.google.api.client.http.HttpHeaders();
    	    httpHeaders.set("x-goog-encryption-algorithm", "AES256");
    	    httpHeaders.set("x-goog-encryption-key", base64CseKey);
    	    httpHeaders.set("x-goog-encryption-key-sha256", base64CseKeyHash);

    	    com.google.api.services.storage.Storage.Objects.Delete deleteObject = storage.objects().delete(bucketName, objectName);

    	    // If you're using AppEngine, turn off setDirectDownloadEnabled:
    	    //      getObject.getMediaHttpDownloader().setDirectDownloadEnabled(false);

    	    deleteObject.setRequestHeaders(httpHeaders);

    	    try {
    	       deleteObject.execute();
    	    } catch (GoogleJsonResponseException e) {
    	      System.out.println("Error downloading: " + e.getContent());
    	      System.exit(1);
    	    }
    	  }
    
    public static List<String> listObjects(
    		com.google.api.services.storage.Storage storage,
    	      String bucketName, String userEmail)
    	      throws Exception {

    	    // Set the CSEK headers
    	    final com.google.api.client.http.HttpHeaders httpHeaders = new com.google.api.client.http.HttpHeaders();
    	    httpHeaders.set("x-goog-encryption-algorithm", "AES256");
    	    //httpHeaders.set("x-goog-encryption-key", base64CseKey);
    	    //httpHeaders.set("x-goog-encryption-key-sha256", base64CseKeyHash);

    	    com.google.api.services.storage.Storage.Objects.List listOfObjects = storage.objects().list(bucketName);

    	    // If you're using AppEngine, turn off setDirectDownloadEnabled:
    	    //      getObject.getMediaHttpDownloader().setDirectDownloadEnabled(false);

    	    //listOfObjects.setRequestHeaders(httpHeaders);
    	    
    	    List<String> filesList = new ArrayList<String>();

    	    try {
    	    	Objects returnedObjects = listOfObjects.execute();
    	    	for (StorageObject obj : returnedObjects.getItems()) {
    	              System.out.println(obj.getName());
    	              //System.out.println("email from list key ** "+ obj.get("email"));
    	              if(null != obj.getMetadata()) {
    	            	  System.out.println("metaData from list key ** "+ obj.getMetadata().get("email"));
    	            	  if(obj.getMetadata().get("email").equalsIgnoreCase(userEmail)) {
    	            		  filesList.add(obj.getName());
    	            	  }
    	            	  //(obj.getMetadata().get("email").equalsIgnoreCase(userEmail) == true) ? filesList.add(obj.getName()): null;
    	              }
    	              
    	            }
    	    	
    	    } catch (GoogleJsonResponseException e) {
    	      System.out.println("Error downloading: " + e.getContent());
    	      System.exit(1);
    	    }
    	    return filesList;
    	  }

    @PostMapping(path = "/makefolder", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<com.cmpe273.dropbox.backend.entity.Files> makeFolder(@RequestBody String data, HttpSession session) throws JSONException, IOException {

        JSONObject jObject = new JSONObject(data);
        String folderName = jObject.getString("filename");
        String folderparent = jObject.getString("fileparent");
        String email = (String) session.getAttribute("email");

        if(email==null){
            return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);
        }

        //String folderpath = UPLOADED_FOLDER + email.split("\\.")[0]+"/"+folderName;
        String folderpath =  email.split("\\.")[0]+"/"+folderName;

        com.cmpe273.dropbox.backend.entity.Files file= new com.cmpe273.dropbox.backend.entity.Files();

        file.setFilename(folderName);
        file.setFilepath(folderpath);
        file.setSharedcount(0);
        file.setOwner(email);
        file.setFileparent(folderparent);
        file.setStarred("F");
        file.setIsfile("F");

        Path path = Paths.get(folderpath);
        Files.createDirectories(path);

        fileService.uploadFile(file);

        Userfiles userfiles = new Userfiles();

        userfiles.setEmail(email);
        userfiles.setFilepath(folderpath);

        userFilesService.addUserFile(userfiles);

        Userlog userlog = new Userlog();

        userlog.setEmail(email);
        userlog.setFilename(file.getFilename());
        userlog.setFilepath(file.getFilepath());
        userlog.setAction("Make Folder");
        userlog.setActiontime(new Date().toString());
        userlog.setIsfile("F");
        userLogService.addUserLog(userlog);


        return new ResponseEntity(file, HttpStatus.OK);

    }

    @PostMapping(path = "/star", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> starFile(@RequestBody String data) throws JSONException {

        JSONObject jObject = new JSONObject(data);
        String filepath = jObject.getString("filepath");
        String starred = jObject.getString("starred");

        fileService.markStar(filepath, starred);
        return new ResponseEntity(null, HttpStatus.OK);

    }

    @GetMapping(path = "/{filename}"/*, produces = MediaType.APPLICATION_JSON_VALUE*/)
    public ResponseEntity<InputStreamResource> downloadFile(@RequestParam String filepath, @PathVariable("filename") String filename) {
System.out.println("download file filepath "+filepath);
System.out.println("download file filename "+filename);
        File file2Upload = new File(filepath);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.set(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=" + filename.replace(" ", "_"));
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        InputStreamResource resource = null;
        try {
            resource = new InputStreamResource(new FileInputStream(file2Upload));
        } catch (FileNotFoundException e) {
            e.printStackTrace();

                return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);

        }

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(file2Upload.length())
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .body(resource);
    }

}