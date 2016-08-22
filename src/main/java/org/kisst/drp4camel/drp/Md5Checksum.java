package org.kisst.drp4camel.drp;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Checksum {

	public static byte[] createChecksum(File file)  {
		try(InputStream fis =  new FileInputStream(file)) {

			byte[] buffer = new byte[1024];
			MessageDigest complete = MessageDigest.getInstance("MD5");
			int numRead;

			do {
				numRead = fis.read(buffer);
				if (numRead > 0) {
					complete.update(buffer, 0, numRead);
				}
			} while (numRead != -1);
			return complete.digest();
		}
		catch (FileNotFoundException e) { throw new RuntimeException(e);}
		catch (IOException e) {throw new RuntimeException(e);}
		catch (NoSuchAlgorithmException e) {throw new RuntimeException(e);}
	}

	public static String getMD5Checksum(File file)  {
		byte[] b = createChecksum(file);
		String result = "";
		for (int i=0; i < b.length; i++)
			result += Integer.toString( ( b[i] & 0xff ) + 0x100, 16).substring( 1 );
		return result;
	}
}
