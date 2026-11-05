/*******************************************************************************
 * Copyright or © or Copr. Institut des Sciences du Mouvement 
 * (CNRS & Aix Marseille Université)
 * 
 * The DOCoMETER Software must be used with a real time data acquisition 
 * system marketed by ADwin (ADwin Pro and Gold, I and II) or an Arduino 
 * Uno. This software, created within the Institute of Movement Sciences, 
 * has been developed to facilitate their use by a "neophyte" public in the 
 * fields of industrial computing and electronics.  Students, researchers or 
 * engineers can configure this acquisition system in the best possible 
 * conditions so that it best meets their experimental needs. 
 * 
 * This software is governed by the CeCILL-B license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-B
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 * 
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-B license and that you accept its terms.
 * 
 * Contributors:
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [04/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.history;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.FileBasedConfig;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.util.FS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests of experiments history without Eclipse : run them as plain JUnit 5 tests.
 */
public class SnapshotRepositoryTest {
	
	private static final PersonIdent AUTHOR = new PersonIdent("Tester", "tester@docometre");
	
	private Path experiment;
	
	private SnapshotRepository repository;
	
	@BeforeEach
	void setUp() throws Exception {
		experiment = Files.createTempDirectory("DocometreHistoryTest");
		repository = SnapshotRepository.init(experiment.toFile());
	}
	
	@AfterEach
	void tearDown() throws Exception {
		if(repository != null) repository.close();
		deleteDirectory(experiment);
	}
	
	/*
	 * Delete directory once history is closed. Git pack files are read only : they are made
	 * writable first (required on Windows). On Windows, a file which has just been written may
	 * also be locked for a short time by another process (antivirus, indexing service) :
	 * deletion is retried for 5 seconds. If a file is still locked after that, it is most
	 * probably still opened by the history (repository not properly closed).
	 */
	private static void deleteDirectory(Path directory) throws Exception {
		IOException lastException = null;
		for (int i = 0; i < 50; i++) {
			try {
				if(!Files.exists(directory)) return;
				List<Path> paths;
				try (Stream<Path> stream = Files.walk(directory)) {
					paths = stream.sorted(Comparator.reverseOrder()).toList();
				}
				for (Path path : paths) {
					path.toFile().setWritable(true);
					Files.deleteIfExists(path);
				}
				return;
			} catch (IOException e) {
				lastException = e;
				Thread.sleep(100);
			}
		}
		throw new IOException("Unable to delete " + directory + " after 5 s : a file is probably still opened by the history", lastException);
	}
	
	private void write(String path, String content) throws IOException {
		Path file = experiment.resolve(path);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content, StandardCharsets.UTF_8);
	}
	
	private String read(String path) throws IOException {
		return Files.readString(experiment.resolve(path), StandardCharsets.UTF_8);
	}
	
	private boolean exists(String path) {
		return Files.exists(experiment.resolve(path));
	}
	
	private void delete(String path) throws IOException {
		Files.delete(experiment.resolve(path));
	}
	
	private Snapshot snapshot(String name) throws SnapshotException {
		Snapshot snapshot = repository.createSnapshot(name, null, SnapshotType.MANUAL, AUTHOR);
		assertNotNull(snapshot, "Snapshot " + name + " should have been created");
		return snapshot;
	}
	
	private static String toString(List<FileChange> fileChanges) {
		StringBuilder result = new StringBuilder();
		for (FileChange fileChange : fileChanges) result.append(fileChange.kind().name().charAt(0)).append(' ').append(fileChange.path()).append('\n');
		return result.toString();
	}
	
	@Test
	void initCreatesEmptyHistoryWithDefaultFiles() throws Exception {
		assertTrue(SnapshotRepository.isHistorized(experiment.toFile()));
		assertTrue(exists(".gitignore"));
		assertTrue(exists(".gitattributes"));
		assertTrue(repository.getSnapshots().isEmpty());
		assertNull(repository.getLastSnapshot());
		assertTrue(repository.hasChanges());
		assertThrows(SnapshotException.class, () -> SnapshotRepository.init(experiment.toFile()));
	}
	
	@Test
	void settingsDoNotDependOnUserGitConfiguration() throws Exception {
		StoredConfig config = new FileBasedConfig(experiment.resolve(".git/config").toFile(), FS.DETECTED);
		config.load();
		assertEquals("default", config.getString("core", null, "checkstat"));
		assertEquals("false", config.getString("core", null, "autocrlf"));
		assertEquals("false", config.getString("commit", null, "gpgsign"));
		assertEquals(new File(experiment.toFile(), ".git/info/exclude").getAbsolutePath(), new File(config.getString("core", null, "excludesfile")).getAbsolutePath());
		assertEquals("false", config.getString("core", null, "logallrefupdates"));
		// A file modified twice in the same second, with the same size, is detected
		write("Acquisition.process", "v1");
		snapshot("v1");
		write("Acquisition.process", "v2");
		assertTrue(repository.hasChanges());
	}
	
	@Test
	void openExistingHistory() throws Exception {
		write("Acquisition.process", "v1");
		snapshot("Config initiale");
		repository.close();
		repository = SnapshotRepository.open(experiment.toFile());
		assertEquals("Config initiale", repository.getLastSnapshot().name());
		assertThrows(SnapshotException.class, () -> SnapshotRepository.open(experiment.resolve("missing").toFile()));
	}
	
	@Test
	void snapshotIsNotCreatedWhenNothingChanged() throws Exception {
		write("Acquisition.process", "v1");
		snapshot("Config initiale");
		assertFalse(repository.hasChanges());
		assertNull(repository.createSnapshot("Rien", null, SnapshotType.MANUAL, AUTHOR));
		assertEquals(1, repository.getSnapshots().size());
	}
	
	@Test
	void snapshotsAreNumberedAndDescribed() throws Exception {
		write("Acquisition.process", "v1");
		repository.createSnapshot("Config initiale", "Configuration ADwin\n\nvalidée", SnapshotType.MANUAL, AUTHOR);
		write("Acquisition.process", "v2");
		repository.createSnapshot("Sauvegarde\nautomatique", null, SnapshotType.AUTO, AUTHOR);
		List<Snapshot> snapshots = repository.getSnapshots();
		assertEquals(2, snapshots.size());
		Snapshot last = snapshots.get(0);
		Snapshot first = snapshots.get(1);
		assertEquals(2, last.number());
		assertEquals("Sauvegarde automatique", last.name());
		assertEquals("", last.description());
		assertEquals(SnapshotType.AUTO, last.type());
		assertEquals(1, first.number());
		assertEquals("Config initiale", first.name());
		assertEquals("Configuration ADwin\n\nvalidée", first.description());
		assertEquals(SnapshotType.MANUAL, first.type());
		assertEquals("Tester", first.author());
		assertNull(first.restoredFromId());
		assertEquals(first, repository.getSnapshot(first.commitId()));
		assertThrows(IllegalArgumentException.class, () -> repository.createSnapshot(" ", null, SnapshotType.MANUAL, AUTHOR));
	}
	
	@Test
	void restoreGoesBackThenForward() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("Config initiale");
		write("Sujet01/Session01/Essai01/Force.samples", "data");
		write("Acquisition.process", "v2");
		Snapshot s2 = snapshot("Avant filtrage");
		delete("Sujet01/Session01/Essai01/Force.samples");
		write("Filtrage.dataprocess", "10 Hz");
		Snapshot s3 = snapshot("Filtrage");
		
		// Back to S1
		Snapshot r4 = repository.restore(s1, "Restauration de « Config initiale »", AUTHOR);
		assertNotNull(r4);
		assertEquals(4, r4.number());
		assertEquals(SnapshotType.RESTORE, r4.type());
		assertEquals(s1.commitId(), r4.restoredFromId());
		assertTrue(r4.hasSameContent(s1));
		assertEquals("v1", read("Acquisition.process"));
		assertFalse(exists("Filtrage.dataprocess"));
		assertFalse(exists("Sujet01/Session01/Essai01/Force.samples"));
		assertFalse(repository.hasChanges());
		
		// Forward to S3
		Snapshot r5 = repository.restore(s3, "Restauration de « Filtrage »", AUTHOR);
		assertTrue(r5.hasSameContent(s3));
		assertEquals("v2", read("Acquisition.process"));
		assertEquals("10 Hz", read("Filtrage.dataprocess"));
		
		// Back to S2 : deleted data file comes back
		repository.restore(s2, "Restauration de « Avant filtrage »", AUTHOR);
		assertEquals("data", read("Sujet01/Session01/Essai01/Force.samples"));
		assertFalse(exists("Filtrage.dataprocess"));
		
		// Nothing is lost : all snapshots are still in history
		assertEquals(6, repository.getSnapshots().size());
		assertEquals(s3, repository.getSnapshot(s3.commitId()));
	}
	
	@Test
	void restoreRequiresNoChangeAndDoesNothingOnSameContent() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("Config initiale");
		assertNull(repository.restore(s1, "Restauration", AUTHOR));
		write("Acquisition.process", "v2");
		assertThrows(SnapshotException.class, () -> repository.restore(s1, "Restauration", AUTHOR));
		assertEquals("v2", read("Acquisition.process"));
	}
	
	@Test
	void restoreKeepsFilesWhichAreNotHistorized() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("Config initiale");
		write("BinSource/Acquisition.BAS", "generated");
		write("Acquisition.process", "v2");
		snapshot("v2");
		repository.restore(s1, "Restauration", AUTHOR);
		assertEquals("v1", read("Acquisition.process"));
		assertEquals("generated", read("BinSource/Acquisition.BAS"));
	}
	
	@Test
	void restoreOnlyOneFolder() throws Exception {
		write("Acquisition.process", "v1");
		write("Sujet01/a.samples", "a1");
		Snapshot s1 = snapshot("Initial");
		write("Acquisition.process", "v2");
		write("Sujet01/a.samples", "a2");
		write("Sujet01/b.samples", "b2");
		snapshot("Modifications");
		Snapshot restored = repository.restore(s1, "Sujet01/", "Restauration de Sujet01", AUTHOR);
		assertNotNull(restored);
		assertEquals(SnapshotType.RESTORE, restored.type());
		assertEquals("a1", read("Sujet01/a.samples"));
		assertFalse(exists("Sujet01/b.samples"));
		assertEquals("v2", read("Acquisition.process"));
		assertFalse(repository.hasChanges());
		assertNull(repository.restore(s1, "Sujet01", "Restauration de Sujet01", AUTHOR));
	}
	
	@Test
	void restoreOneFileWithItsFoldersOrRemoveIt() throws Exception {
		write("Acquisition.process", "v1");
		write("Sujet01/Session01/Essai n°1/Force.samples", "data1");
		Files.createDirectories(experiment.resolve("Sujet01/Session01/Essai n°2"));
		Files.createDirectories(experiment.resolve("Sujet02"));
		Snapshot s1 = snapshot("n°1");
		deleteDirectory(experiment.resolve("Sujet01"));
		write("Acquisition.process", "v2");
		write("Filtrage.dataprocess", "10 Hz");
		Files.createDirectories(experiment.resolve("Sujet03"));
		snapshot("n°2");
		// Deleted file and its folders come back, other files and folders are not modified
		Snapshot r3 = repository.restore(s1, "Sujet01/Session01/Essai n°1/Force.samples", "Restauration de Force.samples", AUTHOR);
		assertEquals(SnapshotType.RESTORE, r3.type());
		assertEquals("data1", read("Sujet01/Session01/Essai n°1/Force.samples"));
		assertFalse(exists("Sujet01/Session01/Essai n°2"));
		assertEquals("v2", read("Acquisition.process"));
		assertEquals("10 Hz", read("Filtrage.dataprocess"));
		assertTrue(Files.isDirectory(experiment.resolve("Sujet03")));
		assertFalse(repository.hasChanges());
		// A file which did not exist in snapshot is removed
		repository.restore(s1, "Filtrage.dataprocess", "Restauration de Filtrage.dataprocess", AUTHOR);
		assertFalse(exists("Filtrage.dataprocess"));
		assertFalse(repository.hasChanges());
		// A whole folder, with its empty folders
		repository.restore(s1, "Sujet01", "Restauration de Sujet01", AUTHOR);
		assertTrue(Files.isDirectory(experiment.resolve("Sujet01/Session01/Essai n°2")));
		assertFalse(repository.hasChanges());
		assertNull(repository.restore(s1, "Sujet01", "Restauration de Sujet01", AUTHOR));
	}
	
	@Test
	void differencesOfAFile() throws Exception {
		write("Filtrage.dataprocess", "Fonction : Butterworth\nOrdre : 2\nCoupure : 10 Hz\n");
		Snapshot s1 = snapshot("n°1");
		write("Filtrage.dataprocess", "Fonction : Butterworth\nOrdre : 2\nCoupure : 6 Hz\n");
		Snapshot s2 = snapshot("n°2");
		SnapshotRepository.FileDiff diff = repository.getDiff(s1, s2, "Filtrage.dataprocess");
		assertEquals(SnapshotRepository.FileDiff.Result.DIFFERENCES, diff.result());
		assertTrue(diff.text().contains("-Coupure : 10 Hz"), diff.text());
		assertTrue(diff.text().contains("+Coupure : 6 Hz"), diff.text());
		assertTrue(diff.text().contains(" Ordre : 2"), diff.text());
		// Current file, missing file
		write("Filtrage.dataprocess", "Fonction : Butterworth\nOrdre : 4\nCoupure : 6 Hz\n");
		assertTrue(repository.getDiff(s2, null, "Filtrage.dataprocess").text().contains("+Ordre : 4"));
		assertEquals(SnapshotRepository.FileDiff.Result.IDENTICAL, repository.getDiff(s2, s2, "Filtrage.dataprocess").result());
		assertTrue(repository.getDiff(null, s1, "Filtrage.dataprocess").text().contains("+Coupure : 10 Hz"));
		assertTrue(repository.getDiff(s1, null, "missing.chart").result() == SnapshotRepository.FileDiff.Result.IDENTICAL);
		// Binary file
		Files.write(experiment.resolve("Force.samples"), new byte[] {0, 1, 2, 0, 3});
		assertEquals(SnapshotRepository.FileDiff.Result.BINARY, repository.getDiff(s2, null, "Force.samples").result());
	}
	
	@Test
	void changesSinceLastSnapshotAndBetweenSnapshots() throws Exception {
		write("Acquisition.process", "v1");
		write("Filtrage.dataprocess", "10 Hz");
		Snapshot s1 = snapshot("Initial");
		write("Filtrage.dataprocess", "6 Hz");
		delete("Acquisition.process");
		write("EMG.chart", "chart");
		assertEquals("D Acquisition.process\nA EMG.chart\nM Filtrage.dataprocess\n", toString(repository.getChanges()));
		Snapshot s2 = snapshot("Modifications");
		assertTrue(repository.getChanges().isEmpty());
		assertEquals("D Acquisition.process\nA EMG.chart\nM Filtrage.dataprocess\n", toString(repository.getChanges(s1, s2)));
		assertEquals("A Acquisition.process\nD EMG.chart\nM Filtrage.dataprocess\n", toString(repository.getChanges(s2, s1)));
		assertEquals(5, repository.getChanges(null, s1).size()); // with .gitignore, .gitattributes and folders list
	}
	
	@Test
	void restoreChangesTakeCurrentModificationsIntoAccount() throws Exception {
		write("Acquisition.process", "v1");
		write("Filtrage.dataprocess", "10 Hz");
		Snapshot s1 = snapshot("Initial");
		write("Force.chart", "chart");
		write("Acquisition.process", "v2");
		snapshot("Graphique");
		// Not yet in a snapshot
		write("Filtrage.dataprocess", "6 Hz");
		write("EMG.chart", "chart");
		delete("Acquisition.process");
		assertEquals("A Acquisition.process\nD EMG.chart\nM Filtrage.dataprocess\nD Force.chart\n", toString(repository.getRestoreChanges(s1)));
		Snapshot auto = repository.createSnapshot("Sauvegarde automatique", null, SnapshotType.AUTO, AUTHOR);
		assertEquals(toString(repository.getChanges(auto, s1)), toString(repository.getRestoreChanges(s1)));
		assertTrue(repository.getRestoreChanges(auto).isEmpty());
	}
	
	@Test
	void readFilesOfOldSnapshots() throws Exception {
		write("Filtrage.dataprocess", "10 Hz");
		Snapshot s1 = snapshot("Initial");
		write("Filtrage.dataprocess", "6 Hz");
		snapshot("6 Hz");
		try (InputStream inputStream = repository.openFile(s1, "Filtrage.dataprocess")) {
			assertEquals("10 Hz", new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
		}
		assertEquals(5, repository.getFileSize(s1, "Filtrage.dataprocess"));
		assertNull(repository.openFile(s1, "missing.chart"));
		assertEquals(-1, repository.getFileSize(s1, "missing.chart"));
	}
	
	@Test
	void largeBinaryFiles() throws Exception {
		byte[] data = new byte[8 * 1024 * 1024];
		new Random(1).nextBytes(data);
		Files.write(experiment.resolve("Force.samples"), data);
		Snapshot s1 = snapshot("Données");
		Files.write(experiment.resolve("Force.samples"), new byte[] {1, 2, 3});
		snapshot("Données modifiées");
		repository.restore(s1, "Restauration", AUTHOR);
		assertArrayEquals(data, Files.readAllBytes(experiment.resolve("Force.samples")));
		assertEquals(data.length, repository.getFileSize(s1, "Force.samples"));
		repository.optimize();
		assertEquals(3, repository.getSnapshots().size());
	}
	
	@Test
	void resetToLastSnapshotDiscardsChangesAndKeepsFilesWhichAreNotHistorized() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("v1");
		write("Acquisition.process", "v2");
		write("Sujet01/new.samples", "new");
		write("BinSource/Acquisition.BAS", "generated");
		delete(".gitattributes");
		assertEquals(s1, repository.resetToLastSnapshot());
		assertFalse(repository.hasChanges());
		assertEquals("v1", read("Acquisition.process"));
		assertFalse(exists("Sujet01/new.samples"));
		assertTrue(exists(".gitattributes"));
		assertEquals("generated", read("BinSource/Acquisition.BAS"));
	}
	
	@Test
	void restoreRemovesEmptyFoldersOfFilesWhichDidNotExist() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("v1");
		write("Sujet02/Session01/Essai01/Force.samples", "data");
		snapshot("Sujet02");
		repository.restore(s1, "Restauration", AUTHOR);
		assertFalse(exists("Sujet02/Session01/Essai01/Force.samples"));
		assertFalse(exists("Sujet02"), "Empty folders must be removed, otherwise the subject is still shown");
		assertFalse(repository.hasChanges());
	}
	
	@Test
	void deleteSnapshotInTheMiddle() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("v1");
		write("Acquisition.process", "v2");
		Snapshot s2 = snapshot("v2");
		write("Acquisition.process", "v3");
		Snapshot s3 = snapshot("v3");
		Snapshot r4 = repository.restore(s1, "Restauration de v1", AUTHOR);
		Snapshot r5 = repository.restore(s2, "Restauration de v2", AUTHOR);
		Snapshot r6 = repository.restore(s3, "Restauration de v3", AUTHOR);
		
		repository.deleteSnapshot(s2);
		
		List<Snapshot> snapshots = repository.getSnapshots();
		assertEquals("6 5 4 3 1", numbers(snapshots));
		// Contents, names, dates and authors are kept, numbers are not changed
		Snapshot newS3 = snapshots.get(3);
		assertTrue(newS3.hasSameContent(s3));
		assertEquals(s3.name(), newS3.name());
		assertEquals(s3.date(), newS3.date());
		assertEquals(s1, snapshots.get(4));
		// References of restorations are updated, reference to deleted snapshot is removed
		assertEquals(s1.commitId(), snapshots.get(2).restoredFromId());
		assertNull(snapshots.get(1).restoredFromId());
		assertEquals(SnapshotType.RESTORE, snapshots.get(1).type());
		assertEquals(newS3.commitId(), snapshots.get(0).restoredFromId());
		assertTrue(snapshots.get(0).hasSameContent(r6));
		assertTrue(snapshots.get(2).hasSameContent(r4));
		assertTrue(snapshots.get(1).hasSameContent(r5));
		// Experiment files are not modified
		assertFalse(repository.hasChanges());
		assertEquals("v3", read("Acquisition.process"));
		// Numbers are never reused
		write("Acquisition.process", "v7");
		assertEquals(7, snapshot("v7").number());
	}
	
	@Test
	void deleteLastSnapshotKeepsFiles() throws Exception {
		write("Acquisition.process", "v1");
		snapshot("v1");
		write("Acquisition.process", "v2");
		write("Filtrage.dataprocess", "10 Hz");
		Snapshot s2 = snapshot("v2");
		repository.deleteSnapshot(s2);
		assertEquals("1", numbers(repository.getSnapshots()));
		assertEquals("v2", read("Acquisition.process"));
		assertEquals("M Acquisition.process\nA Filtrage.dataprocess\n", toString(repository.getChanges()));
		assertEquals(3, snapshot("v2 again").number());
	}
	
	@Test
	void deleteLastSnapshotWhenFilesAreThoseOfThePreviousOne() throws Exception {
		write("Acquisition.process", "v1");
		write(".metadata.properties", "trial.state=undone\n");
		Files.createDirectories(experiment.resolve("Sujet01/Session01/Essai n°1"));
		snapshot("n°1");
		write("Sujet01/Session01/Essai n°1/Force.samples", "data");
		write(".metadata.properties", "trial.state=done\n");
		Files.createDirectories(experiment.resolve("Sujet01/Session01/Essai n°2"));
		Snapshot s2 = snapshot("n°2");
		// Last snapshot with other contents
		write("Acquisition.process", "v3");
		delete("Sujet01/Session01/Essai n°1/Force.samples");
		write(".metadata.properties", "trial.state=undone\n");
		deleteDirectory(experiment.resolve("Sujet01/Session01/Essai n°2"));
		Snapshot s3 = snapshot("n°3");
		// Files go back to the content of n°2 (without snapshot)
		write("Acquisition.process", "v1");
		write("Sujet01/Session01/Essai n°1/Force.samples", "data");
		write(".metadata.properties", "trial.state=done\n");
		Files.createDirectories(experiment.resolve("Sujet01/Session01/Essai n°2"));
		assertTrue(repository.hasChanges());
		repository.deleteSnapshot(s3);
		assertEquals(s2, repository.getLastSnapshot());
		assertEquals("", toString(repository.getChanges()));
		assertFalse(repository.hasChanges());
		assertTrue(repository.getChangedPaths().isEmpty());
	}
	
	@Test
	void deleteLastSnapshotAfterRestoration() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("n°1");
		write("Sujet01/Session01/Essai n°1/Force.samples", "data");
		Snapshot s2 = snapshot("n°2");
		Snapshot r3 = repository.restore(s1, "Restauration de n°1", AUTHOR);
		repository.deleteSnapshot(r3);
		assertEquals(s2, repository.getLastSnapshot());
		// The restoration removed the trial : files are those of n°1, not of n°2
		assertFalse(exists("Sujet01/Session01/Essai n°1/Force.samples"));
		assertEquals("M " + SnapshotRepository.FOLDERS_LIST_FILE_NAME + "\nD Sujet01/Session01/Essai n°1/Force.samples\n", toString(repository.getChanges()));
	}
	
	@Test
	void deleteLastSnapshotAndGoBackToPreviousContent() throws Exception {
		write("Acquisition.process", "v1");
		Files.createDirectories(experiment.resolve("Sujet01/Session01/Essai n°2"));
		Snapshot s1 = snapshot("n°1");
		write("Sujet01/Session01/Essai n°1/Force.samples", "data");
		write("Acquisition.process", "v2");
		Snapshot s2 = snapshot("n°2");
		Snapshot r3 = repository.restore(s1, "Restauration de n°1", AUTHOR);
		repository.deleteSnapshot(r3, true);
		assertEquals(s2, repository.getLastSnapshot());
		assertEquals("2 1", numbers(repository.getSnapshots()));
		assertEquals("data", read("Sujet01/Session01/Essai n°1/Force.samples"));
		assertEquals("v2", read("Acquisition.process"));
		assertTrue(Files.isDirectory(experiment.resolve("Sujet01/Session01/Essai n°2")));
		assertFalse(repository.hasChanges());
	}
	
	@Test
	void deleteAndGoBackRefusedWhenChangesWouldBeLostOrSnapshotIsNotTheLast() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("n°1");
		write("Acquisition.process", "v2");
		Snapshot s2 = snapshot("n°2");
		write("Acquisition.process", "v3");
		assertThrows(SnapshotException.class, () -> repository.deleteSnapshot(s2, true));
		assertEquals(2, repository.getSnapshots().size());
		assertEquals("v3", read("Acquisition.process"));
		assertThrows(SnapshotException.class, () -> repository.deleteSnapshot(s1, true));
		assertEquals(2, repository.getSnapshots().size());
	}
	
	@Test
	void deleteFirstSnapshot() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("v1");
		write("Acquisition.process", "v2");
		Snapshot s2 = snapshot("v2");
		repository.deleteSnapshot(s1);
		List<Snapshot> snapshots = repository.getSnapshots();
		assertEquals("2", numbers(snapshots));
		assertTrue(snapshots.get(0).hasSameContent(s2));
		assertEquals(repository.getChanges(null, s2), repository.getChanges(null, snapshots.get(0)));
		assertFalse(repository.hasChanges());
	}
	
	@Test
	void onlySnapshotCanNotBeDeleted() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("v1");
		assertThrows(SnapshotException.class, () -> repository.deleteSnapshot(s1));
		assertEquals(1, repository.getSnapshots().size());
	}
	
	@Test
	void deletedVersionsArePurged() throws Exception {
		write("Acquisition.process", "v1");
		write("Sujet01/Force.samples", "confidential data");
		Snapshot s1 = snapshot("v1");
		write("Sujet01/Force.samples", "other data");
		Snapshot s2 = snapshot("v2");
		write("Acquisition.process", "v3");
		snapshot("v3");
		assertEquals(List.of("Sujet01/Force.samples"), repository.getFilesOnlyIn(s1));
		assertEquals(List.of(), repository.getFilesOnlyIn(s2).stream().filter(path -> path.endsWith(".process")).toList());
		ObjectId confidentialId;
		try (ObjectInserter.Formatter formatter = new ObjectInserter.Formatter()) {
			confidentialId = formatter.idFor(Constants.OBJ_BLOB, "confidential data".getBytes(StandardCharsets.UTF_8));
		}
		try (Repository gitRepository = new FileRepositoryBuilder().setGitDir(experiment.resolve(".git").toFile()).build()) {
			assertTrue(gitRepository.getObjectDatabase().has(confidentialId));
		}
		repository.deleteSnapshot(s1);
		try (Repository gitRepository = new FileRepositoryBuilder().setGitDir(experiment.resolve(".git").toFile()).build()) {
			assertFalse(gitRepository.getObjectDatabase().has(confidentialId), "Deleted version must be purged");
		}
		assertFalse(exists(".git/logs"));
		assertFalse(exists(".git/ORIG_HEAD"));
	}
	
	private static String numbers(List<Snapshot> snapshots) {
		return String.join(" ", snapshots.stream().map(snapshot -> String.valueOf(snapshot.number())).toList());
	}
	
	@Test
	void changedPathsContainParentFolders() throws Exception {
		write("Acquisition.process", "v1");
		write("Sujet01/Session01/Essai n°1/Force.samples", "data");
		Files.createDirectories(experiment.resolve("Sujet01/Session02/Essai n°1"));
		snapshot("v1");
		assertTrue(repository.getChangedPaths().isEmpty());
		write("Sujet01/Session01/Essai n°1/Force.samples", "new data");
		Files.createDirectories(experiment.resolve("Sujet02/Session01"));
		deleteDirectory(experiment.resolve("Sujet01/Session02"));
		assertEquals(Set.of("", "Sujet01", "Sujet01/Session01", "Sujet01/Session01/Essai n°1", "Sujet01/Session01/Essai n°1/Force.samples", "Sujet02", "Sujet02/Session01"), repository.getChangedPaths());
		snapshot("v2");
		assertTrue(repository.getChangedPaths().isEmpty());
		delete("Acquisition.process");
		assertEquals(Set.of("", "Acquisition.process"), repository.getChangedPaths());
	}
	
	@Test
	void newEmptyFolderIsAChange() throws Exception {
		write("Acquisition.process", "v1");
		snapshot("v1");
		Files.createDirectories(experiment.resolve("Sujet01/Session01/Essai n°1"));
		assertTrue(repository.hasChanges());
		assertEquals("M " + SnapshotRepository.FOLDERS_LIST_FILE_NAME + "\n", toString(repository.getChanges()));
		assertNotNull(repository.createSnapshot("Essais", null, SnapshotType.MANUAL, AUTHOR));
	}
	
	@Test
	void restoreRecreatesEmptyFoldersAndRemovesOthers() throws Exception {
		Files.createDirectories(experiment.resolve("Sujet01/Session01/Essai n°1"));
		Files.createDirectories(experiment.resolve("Sujet01/Session01/Essai n°2"));
		write("Sujet01/Session01/Essai n°2/Force.samples", "data");
		write("BinSource/Acquisition.BAS", "generated");
		Snapshot s1 = snapshot("Essais");
		assertFalse(read(SnapshotRepository.FOLDERS_LIST_FILE_NAME).contains("BinSource"), "Folders which are not historized are not listed");
		deleteDirectory(experiment.resolve("Sujet01"));
		Files.createDirectories(experiment.resolve("Sujet02/Session01"));
		snapshot("Sujet02");
		repository.restore(s1, "Restauration", AUTHOR);
		assertTrue(Files.isDirectory(experiment.resolve("Sujet01/Session01/Essai n°1")));
		assertEquals("data", read("Sujet01/Session01/Essai n°2/Force.samples"));
		assertFalse(exists("Sujet02"));
		assertEquals("generated", read("BinSource/Acquisition.BAS"));
		assertFalse(repository.hasChanges());
	}
	
	@Test
	void deleteHistoryOnlyDeletesHistory() throws Exception {
		write("Acquisition.process", "v1");
		write("Sujet01/Force.samples", "data");
		snapshot("v1");
		write("Acquisition.process", "v2");
		snapshot("v2");
		repository.optimize();
		repository.close();
		repository = null;
		assertNull(SnapshotRepository.deleteHistory(experiment.toFile()));
		assertFalse(SnapshotRepository.isHistorized(experiment.toFile()));
		assertFalse(exists(".git"));
		assertFalse(exists(SnapshotRepository.DELETED_HISTORY_FOLDER_NAME));
		assertFalse(exists(".gitignore"));
		assertFalse(exists(".gitattributes"));
		assertFalse(exists(SnapshotRepository.FOLDERS_LIST_FILE_NAME));
		// Experiment files are not modified
		assertEquals("v2", read("Acquisition.process"));
		assertEquals("data", read("Sujet01/Force.samples"));
		assertThrows(SnapshotException.class, () -> SnapshotRepository.deleteHistory(experiment.toFile()));
		// History can be enabled again
		repository = SnapshotRepository.init(experiment.toFile());
		assertEquals(1, snapshot("Nouvel historique").number());
	}
	
	@Test
	void deleteHistoryKeepsModifiedGitFiles() throws Exception {
		write(".gitignore", SnapshotRepository.DEFAULT_GIT_IGNORE + "*.tmp\n");
		write("Acquisition.process", "v1");
		snapshot("v1");
		repository.close();
		repository = null;
		assertNull(SnapshotRepository.deleteHistory(experiment.toFile()));
		assertTrue(exists(".gitignore"));
		assertFalse(exists(".gitattributes"));
	}
	
	@Test
	void deleteHistoryRemovesFolderLeftByPreviousDeletion() throws Exception {
		write(SnapshotRepository.DELETED_HISTORY_FOLDER_NAME + "/objects/old", "old");
		write("Acquisition.process", "v1");
		snapshot("v1");
		assertTrue(repository.getChanges().isEmpty(), "Folder left by a previous deletion must not be historized");
		repository.close();
		repository = null;
		assertNull(SnapshotRepository.deleteHistory(experiment.toFile()));
		assertFalse(exists(SnapshotRepository.DELETED_HISTORY_FOLDER_NAME));
	}
	
	@Test
	void temporaryHistoryFoldersAreNeverHistorized() throws Exception {
		// .gitignore written by the first version of DOCoMETRe history, without temporary history folders
		repository.close();
		repository = null;
		write(".gitignore", "# DOCoMETRe - files which are not historized\nBinSource/\n.DS_Store\nThumbs.db\ndesktop.ini\n");
		repository = SnapshotRepository.open(experiment.toFile());
		write(SnapshotRepository.DELETED_HISTORY_FOLDER_NAME + "/objects/old", "old");
		write(SnapshotRepository.RENAMING_HISTORY_FOLDER_NAME + "/objects/old", "old");
		write("Acquisition.process", "v1");
		snapshot("v1");
		assertTrue(repository.getChanges().isEmpty());
		assertEquals("A " + SnapshotRepository.FOLDERS_LIST_FILE_NAME + "\nA .gitattributes\nA .gitignore\nA Acquisition.process\n", toString(repository.getChanges(null, repository.getLastSnapshot())));
		// Exclusions are written only once
		repository.close();
		repository = SnapshotRepository.open(experiment.toFile());
		String exclude = read(".git/info/exclude");
		assertEquals(1, exclude.split("/" + SnapshotRepository.DELETED_HISTORY_FOLDER_NAME + "/", -1).length - 1);
		assertEquals(1, exclude.split("/" + SnapshotRepository.RENAMING_HISTORY_FOLDER_NAME + "/", -1).length - 1);
		// Unmodified .gitignore of a previous version is deleted with history
		repository.close();
		repository = null;
		assertNull(SnapshotRepository.deleteHistory(experiment.toFile()));
		assertFalse(exists(".gitignore"));
	}
	
	@Test
	void exclusiveActionAccessesHistoryFolder() throws Exception {
		write("Acquisition.process", "v1");
		snapshot("v1");
		List<File> gitFolders = new java.util.ArrayList<>();
		repository.runExclusive(gitFolders::add);
		assertEquals(experiment.resolve(".git").toFile().getAbsoluteFile(), gitFolders.get(0).getAbsoluteFile());
		assertThrows(IOException.class, () -> repository.runExclusive(gitFolder -> { throw new IOException("export failed"); }));
	}
	
	@Test
	void filesOfSnapshotWithSameContentAsAnotherAreNotLost() throws Exception {
		write("Acquisition.process", "v1");
		Snapshot s1 = snapshot("v1");
		write("Acquisition.process", "v2");
		snapshot("v2");
		Snapshot s3 = repository.restore(s1, "Restoration of v1", AUTHOR);
		assertTrue(s3.hasSameContent(s1));
		// Version v1 is kept by No.3
		assertEquals(List.of(), repository.getFilesOnlyIn(s1));
		assertEquals(List.of(), repository.getFilesOnlyIn(s3));
		repository.deleteSnapshot(s1);
		assertEquals("v1", new String(repository.openFile(repository.getLastSnapshot(), "Acquisition.process").readAllBytes(), StandardCharsets.UTF_8));
	}
	
	@Test
	void snapshotsSinceOptimizationAreCounted() throws Exception {
		assertEquals(0, repository.getSnapshotsNumberSinceOptimization());
		write("Acquisition.process", "v1");
		snapshot("v1");
		write("Acquisition.process", "v2");
		repository.createSnapshot("auto", null, SnapshotType.AUTO, AUTHOR);
		assertEquals(2, repository.getSnapshotsNumberSinceOptimization());
		repository.optimize();
		assertEquals(0, repository.getSnapshotsNumberSinceOptimization());
		write("Acquisition.process", "v3");
		Snapshot s3 = snapshot("v3");
		repository.restore(repository.getSnapshots().get(2), "Restoration of v1", AUTHOR);
		assertEquals(2, repository.getSnapshotsNumberSinceOptimization());
		// Deletion purges history : it is an optimization
		repository.deleteSnapshot(s3);
		assertEquals(0, repository.getSnapshotsNumberSinceOptimization());
		repository.close();
		repository = SnapshotRepository.open(experiment.toFile());
		assertEquals(0, repository.getSnapshotsNumberSinceOptimization());
	}
	
	@Test
	void historyFolderCanBeMovedWhenClosed() throws Exception {
		write("Acquisition.process", "v1");
		snapshot("v1");
		repository.optimize();
		repository.getSnapshots();
		repository.close();
		repository = null;
		SnapshotRepository.checkMovable(experiment.toFile());
		assertTrue(exists(".git"));
		assertFalse(exists(SnapshotRepository.RENAMING_HISTORY_FOLDER_NAME));
		repository = SnapshotRepository.open(experiment.toFile());
		assertEquals(1, repository.getSnapshots().size());
	}
	
	@Test
	void historyFolderLeftRenamedIsPutBack() throws Exception {
		write("Acquisition.process", "v1");
		snapshot("v1");
		repository.close();
		repository = null;
		Files.move(experiment.resolve(".git"), experiment.resolve(SnapshotRepository.RENAMING_HISTORY_FOLDER_NAME));
		assertTrue(SnapshotRepository.isHistorized(experiment.toFile()));
		assertTrue(exists(".git"));
		assertFalse(exists(SnapshotRepository.RENAMING_HISTORY_FOLDER_NAME));
	}
	
	@Test
	void directoryIsTheExperimentFolder() {
		assertEquals(experiment.toFile().getAbsoluteFile(), new File(repository.getDirectory().getAbsolutePath()));
	}

}
