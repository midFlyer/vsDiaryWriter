package test.java.dbTests;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import net.viperfish.journal.dbProvider.JavaSerializationEntryDatabase;
import net.viperfish.journal.framework.EntryDatabase;
import net.viperfish.journal.framework.Journal;
import net.viperfish.utils.file.CommonFunctions;

public class SerializationDBTest extends DatabaseTest {

	private JavaSerializationEntryDatabase db;

	@Override
	protected EntryDatabase getDB(File dataDir) {
		File f = new File(dataDir, "javaDBS");
		try {
			CommonFunctions.initFile(f);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (db == null) {
			db = new JavaSerializationEntryDatabase();
		}
		return db;
	}

	@Test
	public void testPersistence() {
		JavaSerializationEntryDatabase database = new JavaSerializationEntryDatabase();
		for (int i = 0; i < 10; ++i) {
			Journal j = new Journal();
			j.setSubject("testPersist");
			j.setContent("testPersist");
			database.addEntry(j);
		}
		JavaSerializationEntryDatabase.serialize(new File("testPersist"), database);
		database = JavaSerializationEntryDatabase.deSerialize(new File("testPersist"));
		int count = 0;
		for (Journal i : database.getAll()) {
			Assert.assertEquals("testPersist", i.getContent());
			Assert.assertEquals("testPersist", i.getSubject());
			count++;
		}
		Assert.assertEquals(10, count);
		CommonFunctions.delete(new File("testPersist"));
	}

}
