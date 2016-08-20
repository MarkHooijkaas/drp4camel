package org.kisst.drp4j;

import java.io.File;

/**
 * Class to abstract a remote repository from which files can be pulled.
 */
public interface ResourcePuller {
	/**
	 * Pull files from the repository to a local place
	 */
	public void pull();

	/**
	 * returns the local directory where files are pulled to.
	 */
	public File getLocalDirectory();
}
