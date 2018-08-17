package io.bluedb.disk.recovery;

import java.io.Serializable;

import io.bluedb.api.exceptions.BlueDbException;
import io.bluedb.disk.collection.BlueCollectionOnDisk;
import io.bluedb.disk.segment.Range;
import io.bluedb.disk.segment.rollup.RollupTarget;

public class PendingRollup<T extends Serializable> implements Serializable, Recoverable<T>{

	private static final long serialVersionUID = 1L;

	long timeCreated;
	long segmentGroupingNumber;
	long min;
	long max;
	long recoverableId;
	
	public PendingRollup(long segmentGroupingNumber, long min, long max) {
		timeCreated = System.currentTimeMillis();
		this.segmentGroupingNumber = segmentGroupingNumber;
		this.min = min;
		this.max = max;
	}

	public PendingRollup(RollupTarget rollupTarget) {
		this(rollupTarget.getSegmentGroupingNumber(), rollupTarget.getRange().getStart(), rollupTarget.getRange().getEnd());
	}

	@Override
	public void apply(BlueCollectionOnDisk<T> collection) throws BlueDbException {
		Range range = new Range(min, max);
		collection.rollup(range);
	}

	@Override
	public long getTimeCreated() {
		return timeCreated;
	}

	@Override
	public long getRecoverableId() {
		return recoverableId;
	}

	@Override
	public void setRecoverableId(long recoverableId) {
		this.recoverableId = recoverableId;
	}
}
