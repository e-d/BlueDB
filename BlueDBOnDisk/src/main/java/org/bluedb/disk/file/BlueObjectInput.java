package org.bluedb.disk.file;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;
import org.bluedb.api.exceptions.BlueDbException;
import org.bluedb.disk.lock.BlueReadLock;
import org.bluedb.disk.lock.LockManager;
import org.bluedb.disk.serialization.BlueSerializer;
import org.bluedb.disk.serialization.validation.SerializationException;

public class BlueObjectInput<T> implements Closeable, Iterator<T> {

	private final BlueReadLock<Path> readLock;
	private final Path path;
	private final BlueSerializer serializer;
	private final DataInputStream dataInputStream;
			
	private T next = null;
	private byte[] nextBytes = null;
	private byte[] lastBytes = null;

	public BlueObjectInput(BlueReadLock<Path> readLock, BlueSerializer serializer) throws BlueDbException {
		this.readLock = readLock;
		this.path = readLock.getKey();
		this.serializer = serializer;
		if (path.toFile().exists()) {
			dataInputStream = openDataInputStream(path.toFile());
		} else {
			dataInputStream = null;
		}
	}

	protected static <T> BlueObjectInput<T> getTestInput(Path path, BlueSerializer serializer, DataInputStream dataInputStream) {
		return new BlueObjectInput<T>(path, serializer, dataInputStream);
	}

	private BlueObjectInput(Path path, BlueSerializer serializer, DataInputStream dataInputStream) {
		LockManager<Path> lockManager = new LockManager<Path>();
		readLock = lockManager.acquireReadLock(path);
		this.serializer = serializer;
		this.path = null;
		this.dataInputStream = dataInputStream;
	}

	public Path getPath() {
		return path;
	}

	@Override
	public void close() {
		if (dataInputStream != null) {
			try {
				dataInputStream.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		readLock.close();
	}

	@Override
	public boolean hasNext() {
		return peek() != null;
	}

	@Override
	public T next() {
		if (next == null) {
			next = nextValidObjectFromFile();
		}
		T response = next;
		lastBytes = nextBytes;
		next = null;
		nextBytes = null;
		return response;
	}

	public byte[] nextWithoutDeserializing() {
		if (next == null) {
			nextBytes = nextBytesFromFile();
		}  // otherwise you've already peeked ahead
		lastBytes = nextBytes;
		byte[] response = nextBytes;
		next = null;
		nextBytes = null;
		return response;
	}

	public byte[] getLastBytes() {
		return lastBytes;
	}

	public T peek() {
		if (next == null) {
			next = nextValidObjectFromFile();
		}
		return next;
	}

	private T nextValidObjectFromFile() {
		while(true) {
			try {
				return nextFromFile();
			} catch(SerializationException t) {
				t.printStackTrace(); // Object was corrupted. Print stack trace but try loading the next one
			}
		}
	}

	private T nextFromFile() throws SerializationException {
		nextBytes = nextBytesFromFile();
		if (nextBytes == null) {
			return null;
		}
		Object object = serializer.deserializeObjectFromByteArray(nextBytes);
		@SuppressWarnings("unchecked")
		T t = (T) object;
		return t;
	}

	protected byte[] nextBytesFromFile() {
		if (dataInputStream == null) {
			return null;
		}
		try {
			int objectLength = dataInputStream.readInt();
			byte[] nextBytes = new byte[objectLength];
			dataInputStream.readFully(nextBytes, 0, objectLength);
			return nextBytes;
		} catch (EOFException e) {
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	protected static DataInputStream openDataInputStream(File file) throws BlueDbException {
		try {
			return new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new BlueDbException("cannot open input stream on file " + file.toPath(), e);
		}
	}
}