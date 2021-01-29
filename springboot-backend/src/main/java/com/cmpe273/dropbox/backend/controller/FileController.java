package com.cmpe273.dropbox.backend.controller;

import com.cmpe273.dropbox.backend.entity.Userfiles;
import com.cmpe273.dropbox.backend.entity.Userlog;
import com.cmpe273.dropbox.backend.entity.Users;
import com.cmpe273.dropbox.backend.service.FileService;
import com.cmpe273.dropbox.backend.service.UserFilesService;
import com.cmpe273.dropbox.backend.service.UserLogService;
import com.cmpe273.dropbox.backend.service.UserService;
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
import java.util.Scanner;

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

    //Save the uploaded file to this folder
    private static String UPLOADED_FOLDER = /*System.getProperty("user.dir") + */"./public/uploads/";
    
    //private static String UPLOADED_FOLDER = /*System.getProperty("user.dir") + */"/Users/subashkumarsaladi/Desktop/testing_dropbox/";


    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = "application/json")
    public ResponseEntity<com.cmpe273.dropbox.backend.entity.Files> fileupload(@RequestParam("file") MultipartFile multipartFile,
                                                                               @RequestParam("fileparent") String fileparent, HttpSession session) throws JSONException {

        String email = (String) session.getAttribute("email");
        System.out.println("subashkumarsaladi controller ****");

        com.cmpe273.dropbox.backend.entity.Files newFile = new com.cmpe273.dropbox.backend.entity.Files();
         
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
  		    
             //FileInputStream fileInputStream = new FileInputStream("/Users/satyasameeradevu/eclipse-workspace/IB-PRE/target/IB-PRE-0.0.1-SNAPSHOT/uploads/keerthi.txt");

             Credentials credentials = GoogleCredentials.fromStream(new FileInputStream("/Users/subashkumarsaladi/Desktop/arboreal-height-273317-57289372ded5.json"));
             Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
            
             BlobInfo blobInfo =
                 storage.create(
                     BlobInfo
                         .newBuilder(bucketName, filepath)
                         .setAcl(new ArrayList<>(Arrays.asList(Acl.of(User.ofAllUsers(), Role.OWNER))))
                         .build(),
                         fileInputStreams);

             String fileUrl = blobInfo.getMediaLink();
             newFile.setFilepath(fileUrl);
             System.out.println("Download file  " + fileUrl);
            
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

    @GetMapping(path = "/getfolderfiles", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<com.cmpe273.dropbox.backend.entity.Files>> getFilesInFolder(@RequestParam String filepath) {

      //  JSONObject jObject = new JSONObject(filepath);

        List<com.cmpe273.dropbox.backend.entity.Files> filesList = fileService.getFileByFileparent(filepath);

        return new ResponseEntity(filesList, HttpStatus.OK);
    }

    @GetMapping(path = "/", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<com.cmpe273.dropbox.backend.entity.Files>> getUserFiles(HttpSession session) {

        String email = (String) session.getAttribute("email");
        if(email==null){
            return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);
        }
        String projectId = "arboreal-height-273317";

	    // The ID of your GCS bucket
	    String bucketName = "project-sam";
	    
        
        /**Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        Bucket bucket = storage.get(bucketName);
        Page<Blob> blobs = bucket.list();

        for (Blob blob : blobs.iterateAll()) {
          System.out.println(blob.getName());
        }**/
        
        
        //Storage storage = StorageOptions.newBuilder().setProjectId(projectId).build().getService();
        
        //Page<Blob> blobs = bucket.list();

        //for (Blob blob : blobs.iterateAll()) {
         // System.out.println(blob.getName());
        //}
        
        
        List<Userfiles> userFilesList = userFilesService.getUserFilesByEmail(email);

        List<com.cmpe273.dropbox.backend.entity.Files> filesList = new ArrayList<>();
        for (Userfiles userfiles : userFilesList) {

            com.cmpe273.dropbox.backend.entity.Files file = fileService.getFileByFilepath(userfiles.getFilepath(), "");
            if(file!=null)
                filesList.add(file);
        }

        return new ResponseEntity(filesList, HttpStatus.OK);
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
            	Credentials credentials = GoogleCredentials.fromStream(new FileInputStream("/Users/subashkumarsaladi/Desktop/arboreal-height-273317-57289372ded5.json"));
                Storage storage = StorageOptions.newBuilder().setProjectId(projectId).setCredentials(credentials).build().getService();
                storage.delete(bucketName, filepath);
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
        String shareEmail = jObject.getString("shareEmail");

        Users user = userService.getUserDetails(shareEmail);

        if(user==null){

                return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);

        }

        String email = (String) session.getAttribute("email");

        if(email==null){
            return new ResponseEntity(null, HttpStatus.UNAUTHORIZED);
        }

        Userfiles userfiles = new Userfiles();

        userfiles.setEmail(shareEmail);
        userfiles.setFilepath(file.getFilepath());

        userFilesService.addUserFile(userfiles);

        fileService.updateSharedCount(file.getFilepath(), file.getSharedcount() + 1);


        Userlog userlog = new Userlog();

        userlog.setEmail(email);
        userlog.setFilename(file.getFilename());
        userlog.setFilepath(file.getFilepath());
        if(file.getIsfile().equals("T"))
            userlog.setAction("File shared with "+shareEmail);

        else
            userlog.setAction("Folder shared with "+shareEmail);

        userlog.setActiontime(new Date().toString());
        userlog.setIsfile(file.getIsfile());
        userLogService.addUserLog(userlog);

        return new ResponseEntity(null, HttpStatus.OK);

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