package org.bluedb.disk.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.bluedb.api.exceptions.BlueDbException;
import org.bluedb.disk.Blutils;
import org.bluedb.disk.lock.BlueWriteLock;
import org.bluedb.disk.lock.LockManager;
import org.bluedb.disk.serialization.BlueSerializer;
import org.bluedb.disk.serialization.ThreadLocalFstSerializer;
import junit.framework.TestCase;

public class FileUtilsTest extends TestCase {


	LockManager<Path> lockManager;
	private List<File> filesToDelete;
	private Path testPath;

	@Override
	protected void setUp() throws Exception {
		BlueSerializer serializer = new ThreadLocalFstSerializer(new Class[] {});
		FileManager fileManager = new FileManager(serializer);
		lockManager = fileManager.getLockManager();
		filesToDelete = new ArrayList<>();
		testPath = Paths.get(".", "test_" + this.getClass().getSimpleName());
		filesToDelete.add(testPath.toFile());
	}

	@Override
	protected void tearDown() throws Exception {
		for (File file : filesToDelete)
			Blutils.recursiveDelete(file);
	}

	
	@Test
	public void test_constructor() {
		new FileUtils(); // just to get test coverage to 100%
	}

	@Test
	public void test_getFolderContents_suffix() throws Exception {
		String suffix = ".foo";
		File nonExistant = new File("forever_or_never_whatever");
		filesToDelete.add(nonExistant);
		List<File> emptyFileList = new ArrayList<>();
		assertEquals(emptyFileList, FileUtils.getFolderContents(nonExistant.toPath(), suffix));

		File emptyFolder = new File("bah_bah_black_sheep");
		filesToDelete.add(emptyFolder);
		emptyFolder.mkdirs();
		assertEquals(emptyFileList, FileUtils.getFolderContents(emptyFolder.toPath(), suffix));

		File nonEmptyFolder = new File("owa_tana_siam");
		filesToDelete.add(nonEmptyFolder);
		nonEmptyFolder.mkdirs();
		File fileWithSuffix = createFile(nonEmptyFolder, "legit" + suffix);
		File fileWithSuffix2 = createFile(nonEmptyFolder, "legit.stuff" + suffix);
		createFile(nonEmptyFolder, "not" + suffix + ".this");
		createFile(nonEmptyFolder, "junk");
		List<File> filesWithSuffix = FileUtils.getFolderContents(nonEmptyFolder.toPath(), suffix);
		assertEquals(2, filesWithSuffix.size());
		assertTrue(filesWithSuffix.contains(fileWithSuffix));
		assertTrue(filesWithSuffix.contains(fileWithSuffix2));
	}

	@Test
	public void test_ensureFileExists() throws Exception {
		Path targetFilePath = Paths.get(testPath.toString(), "test_ensureFileExists");
		filesToDelete.add(targetFilePath.toFile());
		try (BlueWriteLock<Path> lock = lockManager.acquireWriteLock(targetFilePath)) {
			FileUtils.ensureFileExists(targetFilePath);
			assertTrue(targetFilePath.toFile().exists());
			FileUtils.deleteFile(lock);
		}
	}

	@Test
	public void test_ensureFileExists_already_existing() throws Exception {
		Path targetFilePath = Paths.get(testPath.toString(), "test_ensureFileExists");
		filesToDelete.add(targetFilePath.toFile());
		try (BlueWriteLock<Path> lock = lockManager.acquireWriteLock(targetFilePath)) {
			FileUtils.ensureFileExists(targetFilePath);
			assertTrue(targetFilePath.toFile().exists());
			FileUtils.ensureFileExists(targetFilePath);  // make sure we can do it after it already exists
			assertTrue(targetFilePath.toFile().exists());
			FileUtils.deleteFile(lock);
		}
	}

	@Test
	public void test_createEmptyFile() throws Exception {
		Path targetFilePath = Paths.get(testPath.toString(), "test_ensureFileExists");
		filesToDelete.add(targetFilePath.toFile());
		try (BlueWriteLock<Path> lock = lockManager.acquireWriteLock(targetFilePath)) {
			FileUtils.ensureDirectoryExists(targetFilePath.toFile());
			assertFalse(targetFilePath.toFile().exists());
			FileUtils.createEmptyFile(targetFilePath);
			assertTrue(targetFilePath.toFile().exists());
			FileUtils.createEmptyFile(targetFilePath);  // make sure a second call doesn't error out
			FileUtils.deleteFile(lock);
		}

		Path fileInNonExistingFolder = Paths.get(testPath.toString(), "non_existing_folder", "test_ensureFileExists");
		try {
			assertFalse(fileInNonExistingFolder.toFile().exists());
			FileUtils.createEmptyFile(fileInNonExistingFolder);
			fail();
		} catch (BlueDbException e) {
		}
	}

	@Test
	public void test_ensureDirectoryExists() {
		Path targetFilePath = Paths.get(testPath.toString(), "test_ensureDirectoryExists",
				"test_ensureDirectoryExists");
		try (BlueWriteLock<Path> lock = lockManager.acquireWriteLock(targetFilePath)) {
			assertFalse(targetFilePath.getParent().toFile().exists());
			FileUtils.ensureDirectoryExists(targetFilePath.toFile());
			assertTrue(targetFilePath.getParent().toFile().exists());
			FileUtils.ensureDirectoryExists(targetFilePath.toFile()); // make sure a second one doesn't error out
		}
	}

	@Test
	public void test_createTempFilePath() {
		Path withParent = Paths.get("grandparent", "parent", "target");
		Path tempWithParent = FileUtils.createTempFilePath(withParent);
		Path expectedTempWithParent = Paths.get("grandparent", "parent", "_tmp_target");
		assertEquals(expectedTempWithParent, tempWithParent);

		Path withoutParent = Paths.get("target");
		Path tempWithoutParent = FileUtils.createTempFilePath(withoutParent);
		Path expectedTempWithoutParent = Paths.get("_tmp_target");
		assertEquals(expectedTempWithoutParent, tempWithoutParent);
	}

	@Test
	public void test_isTempFile() throws Exception {
		File file = Paths.get(".", "whatever").toFile();
		File tempFile = FileUtils.createTempFilePath(file.toPath()).toFile();
		assertFalse(FileUtils.isTempFile(file));
		assertTrue(FileUtils.isTempFile(tempFile));
		assertFalse(FileUtils.isTempFile(null));
	}

	@Test
	public void test_moveWithoutLock() throws Exception {
		File srcFolder = createTempFolder("test_moveWithoutLock_src");
		File dstFolder = createTempFolder("test_moveWithoutLock_dst");
		Path srcFilePath = Paths.get(srcFolder.toPath().toString(), "test_file");
		Path dstFilePath = Paths.get(dstFolder.toPath().toString(), "test_file");
		File srcFile = srcFilePath.toFile();
		File dstFile = dstFilePath.toFile();

		assertFalse(srcFile.exists());
		srcFilePath.toFile().createNewFile();
		assertTrue(srcFile.exists());
		assertFalse(dstFile.exists());
		FileUtils.moveWithoutLock(srcFilePath, dstFilePath);
		assertFalse(srcFile.exists());
		assertTrue(dstFile.exists());
	}

	@Test
	public void test_moveWithoutLock_exception() throws Exception {
		File srcFolder = createTempFolder("test_moveWithoutLock_exception_src");
		File dstFolder = createTempFolder("test_moveWithoutLock_exception_dst");
		Path srcFilePath = Paths.get(srcFolder.toPath().toString(), "test_file");
		Path dstFilePath = Paths.get(dstFolder.toPath().toString(), "test_file");
		File srcFile = srcFilePath.toFile();
		File dstFile = dstFilePath.toFile();

		try {
			assertFalse(srcFile.exists());
			assertFalse(dstFile.exists());
			FileUtils.moveWithoutLock(srcFilePath, dstFilePath);
			fail();
		} catch (BlueDbException e) {
		}
	}

	@Test
	public void test_copyFileWithoutLock() throws Exception {
		File srcFolder = createTempFolder("test_copyFileWithoutLock_src");
		File dstFolder = createTempFolder("test_copyFileWithoutLock_dst");
		Path srcFilePath = Paths.get(srcFolder.toPath().toString(), "test_file");
		Path dstFilePath = Paths.get(dstFolder.toPath().toString(), "test_file");
		File srcFile = srcFilePath.toFile();
		File dstFile = dstFilePath.toFile();

		assertFalse(srcFile.exists());
		srcFilePath.toFile().createNewFile();
		assertTrue(srcFile.exists());
		assertFalse(dstFile.exists());
		FileUtils.copyFileWithoutLock(srcFilePath, dstFilePath);
		assertTrue(srcFile.exists());
		assertTrue(dstFile.exists());
	}

	@Test
	public void test_copyFileWithoutLock_exception() throws Exception {
		File srcFolder = createTempFolder("test_copyFileWithoutLock_exception_src");
		File dstFolder = createTempFolder("test_copyFileWithoutLock_exception_dst");
		Path srcFilePath = Paths.get(srcFolder.toPath().toString(), "test_file");
		Path dstFilePath = Paths.get(dstFolder.toPath().toString(), "test_file");
		File srcFile = srcFilePath.toFile();
		File dstFile = dstFilePath.toFile();

		try {
			assertFalse(srcFile.exists());
			assertFalse(dstFile.exists());
			FileUtils.copyFileWithoutLock(srcFilePath, dstFilePath);
			fail();
		} catch (BlueDbException e) {
		}
	}

	@Test
	public void test_copyDirectoryWithoutLock() throws Exception {
		File testFolder = createTempFolder("test_copyDirectoryWithoutLock");
		Path srcPath = Paths.get(testFolder.toPath().toString(), "src");
		Path srcFilePath = Paths.get(srcPath.toString(), "file");
		Path srcSubfolderPath = Paths.get(srcPath.toString(), "subfolder");
		Path srcSubfolderFilePath = Paths.get(srcSubfolderPath.toString(), "subfolder_file");

		Path dstPath = Paths.get(testFolder.toPath().toString(), "dst");
		Path dstFilePath = Paths.get(dstPath.toString(), "file");
		Path dstSubfolderPath = Paths.get(dstPath.toString(), "subfolder");
		Path dstSubfolderFilePath = Paths.get(dstSubfolderPath.toString(), "subfolder_file");

		srcPath.toFile().mkdirs();
		srcFilePath.toFile().createNewFile();
		srcSubfolderPath.toFile().mkdirs();
		srcSubfolderFilePath.toFile().createNewFile();
		assertTrue(srcPath.toFile().exists());
		assertTrue(srcFilePath.toFile().exists());
		assertTrue(srcSubfolderPath.toFile().exists());
		assertTrue(srcSubfolderFilePath.toFile().exists());

		assertFalse(dstPath.toFile().exists());
		assertFalse(dstFilePath.toFile().exists());
		assertFalse(dstSubfolderPath.toFile().exists());
		assertFalse(dstSubfolderFilePath.toFile().exists());
		FileUtils.copyDirectoryWithoutLock(srcPath, dstPath);
		assertTrue(dstPath.toFile().exists());
		assertTrue(dstFilePath.toFile().exists());
		assertTrue(dstSubfolderPath.toFile().exists());
		assertTrue(dstSubfolderFilePath.toFile().exists());
	}

	@Test
	public void test_copyDirectoryWithoutLock_not_a_directory() throws Exception {
		Path tempFile = null;
		tempFile = Files.createTempFile(this.getClass().getSimpleName() + "_test_copyDirectoryWithoutLock_not_a_directory", ".tmp");
		tempFile.toFile().deleteOnExit();

		Path destination = Paths.get("never_going_to_happen");
		try {
			FileUtils.copyDirectoryWithoutLock(tempFile, destination);
			fail();
		} catch (BlueDbException e) {
		}
	}


	private File createFile(File parentFolder, String fileName) throws IOException {
		File file = Paths.get(parentFolder.toPath().toString(), fileName).toFile();
		file.createNewFile();
		return file;
	}

	private File createTempFolder(String tempFolderName) throws IOException {
		Path tempFolderPath = Files.createTempDirectory(tempFolderName);
		File tempFolder = tempFolderPath.toFile();
		tempFolder.deleteOnExit();
		filesToDelete.add(tempFolder);
		return tempFolder;
	}
}
