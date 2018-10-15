package io.bluedb.disk.collection;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import io.bluedb.disk.collection.index.BlueIndexOnDisk;
import io.bluedb.disk.segment.Segment;
import io.bluedb.disk.segment.SegmentEntityIterator;
import io.bluedb.disk.segment.SegmentManager;
import io.bluedb.disk.serialization.BlueEntity;

public class LastEntityFinder {

	final private SegmentManager<?> segmentManager;

	public LastEntityFinder(final BlueIndexOnDisk<?, ?> index) {
		segmentManager = index.getSegmentManager();
	}

	public LastEntityFinder(final BlueCollectionOnDisk<?> collection) {
		segmentManager = collection.getSegmentManager();
	}

	public BlueEntity<?> getLastEntity() {
		List<Segment<Serializable>> segments = getSegmentsInReverseOrder();
		while (!segments.isEmpty()) {
			BlueEntity<?> last = null;
			Segment<?> segment = segments.remove(0);
			try (SegmentEntityIterator<?> segmentIterator = segment.getIterator(Long.MIN_VALUE, Long.MAX_VALUE)) {
				while(segmentIterator.hasNext()) {
					last = segmentIterator.next();
				}
			}
			if (last != null) {
				return last;
			}
		}
		return null;
	}

	public List<Segment<Serializable>> getSegmentsInReverseOrder() {
		List<?> existingSegmentsUntyped = segmentManager.getAllExistingSegments();
		@SuppressWarnings("unchecked")
		List<Segment<Serializable>> segments = (List<Segment<Serializable>>) existingSegmentsUntyped;
		Collections.sort(segments);
		Collections.reverse(segments);
		return segments;
	}
}
