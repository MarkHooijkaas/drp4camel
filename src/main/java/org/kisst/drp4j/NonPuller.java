package org.kisst.drp4j;

import java.io.File;

public class NonPuller implements ResourcePuller {
	private final File dir;

	public NonPuller(File dir) { this.dir = dir; }

	@Override public void pull() {}
	@Override public File getLocalDirectory() { return dir; }
}
