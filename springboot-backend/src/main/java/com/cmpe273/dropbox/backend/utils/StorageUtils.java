package com.cmpe273.dropbox.backend.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.*;

public class StorageUtils {

  /** Reads the contents of an InputStream and does nothing with it. */
  public static void readStream(InputStream is, String fileName) throws IOException {
	  
	  String fn = "/Users/subashkumarsaladi/Downloads/"+fileName;
	  File targetFile = new File(fn);
	    OutputStream outStream = new FileOutputStream(targetFile);

	    //byte[] buffer = new byte[8 * 1024];
	    //int bytesRead;
	   // while ((bytesRead = initialStream.read(buffer)) != -1) {
	      //  outStream.write(buffer, 0, bytesRead);
	   // }
    byte[] inputBuffer = new byte[8*1024];
    int bytesRead;
    while ((bytesRead = is.read(inputBuffer)) != -1) {
    	outStream.write(inputBuffer, 0, bytesRead);
    }
    // The caller is responsible for closing this InputStream.
    
    outStream.close();
    is.close();
  }

  /**
   * A helper class to provide input streams of any size. The input streams will be full of null
   * bytes.
   */
  static class ArbitrarilyLargeInputStream extends InputStream {

    private long bytesRead;
    private final long streamSize;

    public ArbitrarilyLargeInputStream(long streamSizeInBytes) {
      bytesRead = 0;
      this.streamSize = streamSizeInBytes;
    }

    @Override
    public int read() throws IOException {
      if (bytesRead >= streamSize) {
        return -1;
      }
      bytesRead++;
      return 0;
    }
  }
}
