package io.bluedb.disk.segment;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import io.bluedb.api.keys.BlueKey;
import io.bluedb.api.keys.TimeFrameKey;
import io.bluedb.disk.collection.BlueCollectionImpl;

public class SegmentManager<T extends Serializable> {

	protected static final String SEGMENT_SUFFIX = "_seg";
	// TODO better split up
	protected static final long LEVEL_3 = TimeUnit.HOURS.toMillis(1);
	protected static final long LEVEL_2 = TimeUnit.DAYS.toMillis(1);
	protected static final long LEVEL_1 = LEVEL_2 * 30;
	protected static final long LEVEL_0 = LEVEL_1 * 12;

	private final BlueCollectionImpl<T> collection;
	
	public SegmentManager(BlueCollectionImpl<T> collection) {
		this.collection = collection;
	}

	// TODO test
	public Segment<T> getFirstSegment(BlueKey key) {
		long groupingNumber = key.getGroupingNumber();
		Path segmentPath = getPath(groupingNumber);
		return toSegment(segmentPath);
	}

	// TODO test
	public List<Segment<T>> getAllSegments(BlueKey key) {
		return getExistingSegmentFiles(key).stream()
				.map((f) -> (toSegment(f.toPath())))
				.collect(Collectors.toList());
	}

	// TODO test
	public List<Segment<T>> getExistingSegments(long minValue, long maxValue) {
		return getExistingSegmentFiles(minValue, maxValue).stream()
				.map((f) -> (toSegment(f.toPath())))
				.collect(Collectors.toList());
	}

	// TODO test
	protected Path getPath(BlueKey key) {
		long groupingNumber = key.getGroupingNumber();
		return getPath(groupingNumber);
	}

	// TODO test
	protected Path getPath(long groupingNumber) {
		String level0 = getRangeFileName(groupingNumber, LEVEL_0);
		String level1 = getRangeFileName(groupingNumber, LEVEL_1);
		String level2 = getRangeFileName(groupingNumber, LEVEL_2);
		String level3 = getRangeFileName(groupingNumber, LEVEL_3);
		return Paths.get(collection.getPath().toString(), level0, level1, level2, level3 + SEGMENT_SUFFIX);
	}

	// TODO test
	protected List<Path> getAllPossibleSegmentPaths(BlueKey key) {
		if (key instanceof TimeFrameKey) {
			TimeFrameKey timeFrameKey = (TimeFrameKey)key;
			return getAllPossibleSegmentPaths(timeFrameKey.getStartTime(), timeFrameKey.getEndTime());
		} else {
			Path path = getPath(key);
			return Arrays.asList(path);
		}
	}

	// TODO test
	protected List<Path> getAllPossibleSegmentPaths(long minTime, long maxTime) {
		List<Path> paths = new ArrayList<>();
		minTime = minTime - (minTime % LEVEL_3);
		long i = minTime;
		while (i <= maxTime) {
			Path path = getPath(i);
			paths.add(path);
			i += LEVEL_3;
		}
		return paths;
	}

	protected List<File> getExistingSegmentFiles(BlueKey key) {
		if (key instanceof TimeFrameKey) {
			TimeFrameKey timeFrameKey = (TimeFrameKey)key;
			return getExistingSegmentFiles(timeFrameKey.getStartTime(), timeFrameKey.getEndTime());
		} else {
			return getExistingSegmentFiles(key.getGroupingNumber(), key.getGroupingNumber());
		}
		
	}

	protected List<File> getExistingSegmentFiles(long minValue, long maxValue) {
		Deque<File> foldersToSearch = new ArrayDeque<>();
		File collectionFolder = collection.getPath().toFile();
		foldersToSearch.addLast(collectionFolder);
		
		List<File> results = new ArrayList<>();
		while (!foldersToSearch.isEmpty()) {
			File folder = foldersToSearch.pop();
			results.addAll(getSegmentFilesInRange(folder, minValue, maxValue));
			foldersToSearch.addAll(getNonsegmentSubfoldersInRange(folder, minValue, maxValue));
		}
		
		return results;
	}

	// TODO test
	protected Segment<T> toSegment(Path path) {
		return new Segment<T>(path, collection.getSerializer());
	}

	protected static List<File> getNonsegmentSubfoldersInRange(File folder, long minValue, long maxValue) {
		List<File> folderContents = Arrays.asList(folder.listFiles());
		return folderContents.stream()
			.filter((f) -> f.isDirectory())
			.filter((f) -> !isSegment(f))
			.filter((f) -> folderNameRangeContainsRange(f, minValue, maxValue))
			.collect(Collectors.toList());
	}

	protected static List<File> getSegmentFilesInRange(File folder, long minValue, long maxValue) {
		List<File> folderContents = Arrays.asList(folder.listFiles());
		return folderContents.stream()
			.filter((f) -> isSegment(f))
			.filter((f) -> folderNameRangeContainsRange(f, minValue, maxValue))
			.collect(Collectors.toList());
	}

	protected static boolean isSegment(File folder) {
		return folder.isDirectory() && folder.getName().contains(SEGMENT_SUFFIX);
	}
//	protected static boolean isFileNameALongInRange(File file, long minValue, long maxValue) {
//		try {
//			String fileName = file.getName();
//			long fileNameAsLong = Long.valueOf(fileName);
//			return fileNameAsLong >= minValue && fileNameAsLong <= maxValue;
//		} catch(Exception e) {
//			return false;
//		}
//	}
	
	protected static boolean folderNameRangeContainsRange(File file, long minValue, long maxValue) {
		try {
			String[] fileNameSplit = file.getName().split("_");
			long folderMinValue = Long.valueOf(fileNameSplit[0]);
			long folderMaxValue = Long.valueOf(fileNameSplit[1]);
			return folderMaxValue >= minValue && folderMinValue <= maxValue;
		} catch(Exception e) {
			return false;
		}
	}

	protected static String getRangeFileName(long groupingValue, long multiple) {
		long low = roundDownToMultiple(groupingValue, multiple);
		long high = Math.min(Long.MAX_VALUE - multiple + 1, low) + multiple - 1;  // prevent overflow
		return String.valueOf(low) + "_" + String.valueOf(high);
	}

	protected static long roundDownToMultiple(long value, long multiple) {
		return value - (value % multiple);
	}
}
