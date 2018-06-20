package io.bluedb.disk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.nustaq.serialization.FSTConfiguration;
import io.bluedb.api.Condition;
import io.bluedb.api.exceptions.BlueDbException;
import io.bluedb.api.keys.BlueKey;
import io.bluedb.api.keys.TimeFrameKey;
import io.bluedb.api.keys.TimeKey;

public class Blutils {
	private static final FSTConfiguration serializer = FSTConfiguration.createDefaultConfiguration();

	public static void save(String path, Object o) throws BlueDbException {
		byte[] bytes = serializer.asByteArray(o);
		try (FileOutputStream fos = new FileOutputStream(path)) {
			fos.write(bytes);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			// TODO delete the file
			throw new BlueDbException("error writing to disk (" + path +")", e);
		}
	}

	public static List<File> listFiles(Path path, String suffix) {
		File folder = path.toFile();
		File[] filesInFolder = folder.listFiles();
		List<File> results = new ArrayList<>();
		for (File file: filesInFolder) {
			if(file.getName().endsWith(suffix)) {
				results.add(file);
			}
		}
		return results;
	}

	public static void writeToDisk(Path path, Object data) throws IOException {
		File file = path.toFile();
		File folder = new File(file.getParent());
		folder.mkdirs();
		try (FileOutputStream fos = new FileOutputStream(file)) {
			byte[] bytes = serializer.asByteArray(data);
			fos.write(bytes);
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
	}

	public static <X extends Serializable> boolean meetsConditions(List<Condition<X>> conditions, X object) {
		for (Condition<X> condition: conditions) {
			if (!condition.test(object)) {
				return false;
			}
		}
		return true;
	}

	public static boolean meetsTimeConstraint(BlueKey key, long minTime, long maxTime) {
		if (key instanceof TimeFrameKey) {
			TimeFrameKey timeKey = (TimeFrameKey) key;
			return timeKey.getEndTime() >= minTime && timeKey.getStartTime() <= maxTime;
		}
		if (key instanceof TimeKey) {
			TimeKey timeKey = (TimeKey) key;
			return timeKey.getTime() >= minTime && timeKey.getTime() <= maxTime;
		}
		return true;
	}
}