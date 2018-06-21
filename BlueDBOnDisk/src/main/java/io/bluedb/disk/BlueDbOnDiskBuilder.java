package io.bluedb.disk;

import java.nio.file.Path;
import java.nio.file.Paths;

public class BlueDbOnDiskBuilder {
	private Path path = Paths.get(".", "bluedb");
	private Class<?>[] registeredClasses;
	
	public BlueDbOnDiskBuilder setPath(Path path) {
		this.path = path;
		return this;
	}
	
	public BlueDbOnDiskBuilder setRegisteredClasses(Class<?>...registeredClasses) {
		this.registeredClasses = registeredClasses;
		return this;
	}
	
	public BlueDbOnDisk build() {
		return new BlueDbOnDisk(path, registeredClasses);
	}
}
