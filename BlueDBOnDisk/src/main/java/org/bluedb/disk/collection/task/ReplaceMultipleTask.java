package org.bluedb.disk.collection.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bluedb.api.Mapper;
import org.bluedb.api.exceptions.BlueDbException;
import org.bluedb.api.keys.BlueKey;
import org.bluedb.disk.collection.BlueCollectionOnDisk;
import org.bluedb.disk.query.BlueQueryOnDisk;
import org.bluedb.disk.recovery.IndividualChange;
import org.bluedb.disk.recovery.PendingBatchChange;
import org.bluedb.disk.recovery.RecoveryManager;
import org.bluedb.disk.serialization.BlueEntity;
import org.bluedb.disk.serialization.BlueSerializer;

public class ReplaceMultipleTask<T extends Serializable> extends QueryTask {
	private final BlueCollectionOnDisk<T> collection;
	private final BlueQueryOnDisk<T> query;
	private final Mapper<T> mapper;


	public ReplaceMultipleTask(BlueCollectionOnDisk<T> collection, BlueQueryOnDisk<T> query, Mapper<T> mapper) {
		this.collection = collection;
		this.query = query;
		this.mapper = mapper;
	}

	@Override
	public void execute() throws BlueDbException {
		List<BlueEntity<T>> entities = query.getEntities();
		List<IndividualChange<T>> changes;
		try {
			changes = createChanges(entities, mapper);
		} catch(Throwable t) {
			t.printStackTrace();
			throw new BlueDbException("Error updating values", t);
		}
		
		Collections.sort(changes);
		PendingBatchChange<T> change = PendingBatchChange.createBatchChange(changes);

		RecoveryManager<T> recoveryManager = collection.getRecoveryManager();
		recoveryManager.saveChange(change);
		change.apply(collection);
		recoveryManager.markComplete(change);
	}

	private List<IndividualChange<T>> createChanges(List<BlueEntity<T>> entities, Mapper<T> mapper) {
		List<IndividualChange<T>> updates = new ArrayList<>();
		for (BlueEntity<T> entity: entities) {
			IndividualChange<T> update = createChange(entity, mapper);
			updates.add(update);
		}
		return updates;
	}
	
	private IndividualChange<T> createChange(BlueEntity<T> entity, Mapper<T> mapper) {
		BlueSerializer serializer = collection.getSerializer();
		BlueKey key = entity.getKey();
		T oldValue = serializer.clone(entity.getValue());
		T newValue = mapper.update(serializer.clone(oldValue));
		return new IndividualChange<T>(key, oldValue, newValue);
	}

	@Override
	public String toString() {
		return "<" + this.getClass().getSimpleName() + " on query " + query.toString() + ">";
	}
}
