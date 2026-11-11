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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand.ResetType;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.EmptyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.DirCacheNameConflictException;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ConfigConstants;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.RefUpdate;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryCache;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.storage.file.WindowCacheConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import fr.univamu.ism.docometre.history.FileChange.Kind;

/**
 * Local history of an experiment folder, stored with Git (JGit) in a .git folder.
 * 
 * History is linear, on a single branch. Each snapshot is a commit. Restoring a snapshot
 * never removes a snapshot : it creates a new commit whose content is exactly the
 * content of the restored snapshot (git commit-tree &lt;X&gt;^{tree} -p HEAD, then
 * git reset --hard), so that it is always possible to go back or forward.
 * 
 * This class only depends on JGit : it can be used and tested outside Eclipse.
 * Public methods are synchronized : a repository may be used by several jobs.
 */
public final class SnapshotRepository implements AutoCloseable {
	
	public static final String BRANCH = "main";
	public static final String TYPE_FOOTER = "Docometre-Type";
	public static final String RESTORED_FROM_FOOTER = "Docometre-Restored-From";
	public static final String NUMBER_FOOTER = "Docometre-Number";
	/* Repository configuration : number of the last created snapshot, numbers are never reused */
	private static final String CONFIG_SECTION = "docometre";
	private static final String CONFIG_KEY_LAST_NUMBER = "lastSnapshotNumber";
	/* Repository configuration : number of the last snapshot when history storage was last optimized */
	private static final String CONFIG_KEY_LAST_OPTIMIZATION_NUMBER = "lastOptimizationNumber";
	public static final String GIT_IGNORE_FILE_NAME = ".gitignore";
	public static final String GIT_ATTRIBUTES_FILE_NAME = ".gitattributes";
	/* List of experiment folders, recorded in snapshots because Git does not record empty folders */
	public static final String FOLDERS_LIST_FILE_NAME = ".docometre-folders";
	/* Name of .git folder while checking that it can be moved (see checkMovable) */
	public static final String RENAMING_HISTORY_FOLDER_NAME = ".git.renaming";
	/* Name of .git folder while it is being deleted (see deleteHistory) */
	public static final String DELETED_HISTORY_FOLDER_NAME = ".git.deleted";
	
	/*
	 * Files which are not historized by default : generated files (processes compilation
	 * sources) and operating system files.
	 */
	public static final String DEFAULT_GIT_IGNORE = """
			# DOCoMETRe - files which are not historized
			BinSource/
			.git.deleted/
			.git.renaming/
			.DS_Store
			Thumbs.db
			desktop.ini
			""";
	
	/*
	 * Previous versions of DEFAULT_GIT_IGNORE : such .gitignore files, written by previous
	 * versions of DOCoMETRe, are also deleted with history if they have not been modified.
	 */
	private static final List<String> PREVIOUS_DEFAULT_GIT_IGNORES = List.of("""
			# DOCoMETRe - files which are not historized
			BinSource/
			.DS_Store
			Thumbs.db
			desktop.ini
			""", """
			# DOCoMETRe - files which are not historized
			BinSource/
			.git.deleted/
			.DS_Store
			Thumbs.db
			desktop.ini
			""");
	
	/*
	 * Temporary history folders, excluded in .git/info/exclude so that they are never
	 * historized, even if .gitignore has been modified or written by a previous version.
	 */
	private static final String EXCLUDE_HEADER = "# DOCoMETRe - temporary history folders";
	private static final List<String> EXCLUDED_HISTORY_FOLDERS = List.of("/" + DELETED_HISTORY_FOLDER_NAME + "/", "/" + RENAMING_HISTORY_FOLDER_NAME + "/");
	
	/*
	 * No end of line conversion (same files on all operating systems), data files are binary.
	 */
	public static final String DEFAULT_GIT_ATTRIBUTES = """
			# DOCoMETRe - no end of line conversion, data files are binary
			* -text
			*.samples binary
			*.adw binary
			*.npy binary
			""";
	
	private final Git git;
	private final Repository repository;
	/* True once closed : history must not be used anymore (its files would be opened again) */
	private boolean closed;
	
	private SnapshotRepository(Git git) {
		this.git = git;
		this.repository = git.getRepository();
	}
	
	/*
	 * Return true if directory contains a history (a .git folder)
	 */
	public static boolean isHistorized(File directory) {
		if(directory == null) return false;
		// .git folder left renamed by an interrupted checkMovable : put it back
		File gitFolder = new File(directory, Constants.DOT_GIT);
		File renamingFolder = new File(directory, RENAMING_HISTORY_FOLDER_NAME);
		if(!gitFolder.exists() && renamingFolder.isDirectory()) renamingFolder.renameTo(gitFolder);
		return RepositoryCache.FileKey.isGitRepository(gitFolder, FS.DETECTED);
	}
	
	/*
	 * Check that the .git folder of directory can be moved, i.e. that none of its files is opened
	 * (on Windows, a folder containing an opened file can't be renamed) : the folder is renamed
	 * then renamed back, with retries for 5 seconds (files locked for a short time, e.g. by an
	 * antivirus). History must be closed.
	 */
	public static void checkMovable(File directory) throws SnapshotException {
		Path gitFolder = directory.toPath().resolve(Constants.DOT_GIT);
		Path renamingFolder = directory.toPath().resolve(RENAMING_HISTORY_FOLDER_NAME);
		try {
			move(gitFolder, renamingFolder);
		} catch (IOException | InterruptedException e) {
			throw new SnapshotException("History files of " + directory + " are used by another program", e);
		}
		try {
			move(renamingFolder, gitFolder);
		} catch (IOException | InterruptedException e) {
			throw new SnapshotException("Unable to rename " + renamingFolder + " to " + gitFolder, e);
		}
	}
	
	/*
	 * Create history of directory : Git repository on branch main, without end of line
	 * conversion, with default .gitignore and .gitattributes files if they don't exist.
	 * No snapshot is created.
	 */
	public static SnapshotRepository init(File directory) throws SnapshotException {
		if(isHistorized(directory)) throw new SnapshotException("History already exists in " + directory);
		Git git = null;
		try {
			git = Git.init().setDirectory(directory).setInitialBranch(BRANCH).call();
			configure(git.getRepository());
			writeIfAbsent(new File(directory, GIT_IGNORE_FILE_NAME), DEFAULT_GIT_IGNORE);
			writeIfAbsent(new File(directory, GIT_ATTRIBUTES_FILE_NAME), DEFAULT_GIT_ATTRIBUTES);
			return new SnapshotRepository(git);
		} catch (GitAPIException | IOException e) {
			if(git != null) git.close();
			throw new SnapshotException("Unable to create history in " + directory, e);
		}
	}
	
	/*
	 * Open existing history of directory
	 */
	public static SnapshotRepository open(File directory) throws SnapshotException {
		if(!isHistorized(directory)) throw new SnapshotException("No history in " + directory);
		Git git = null;
		try {
			git = Git.open(directory);
			configure(git.getRepository());
			return new SnapshotRepository(git);
		} catch (IOException e) {
			if(git != null) git.close();
			throw new SnapshotException("Unable to open history of " + directory, e);
		}
	}
	
	/*
	 * Repository settings which must not depend on user or system Git configuration :
	 *  - no end of line conversion (core.autocrlf),
	 *  - full timestamps comparison to detect modified files (core.checkstat=minimal would
	 *    only compare seconds and miss files modified twice in the same second),
	 *  - snapshots are not signed (commit.gpgsign),
	 *  - no reflog (core.logAllRefUpdates), so that deleted snapshots can really be deleted,
	 *  - user global ignore file is not used (core.excludesFile is the repository one).
	 */
	private static void configure(Repository repository) throws IOException {
		StoredConfig config = repository.getConfig();
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_AUTOCRLF, false);
		config.setString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_CHECKSTAT, "default");
		config.setBoolean(ConfigConstants.CONFIG_COMMIT_SECTION, null, ConfigConstants.CONFIG_KEY_GPGSIGN, false);
		// Global ignore file of user (core.excludesFile) must not exclude experiment files
		config.setString(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_EXCLUDESFILE, new File(repository.getDirectory(), Constants.INFO_EXCLUDE).getAbsolutePath());
		// No reflog : a deleted snapshot must not remain reachable
		config.setBoolean(ConfigConstants.CONFIG_CORE_SECTION, null, ConfigConstants.CONFIG_KEY_LOGALLREFUPDATES, false);
		config.save();
		excludeHistoryFolders(repository);
	}
	
	/*
	 * Add temporary history folders to .git/info/exclude if they are not already there
	 */
	private static void excludeHistoryFolders(Repository repository) throws IOException {
		Path excludeFile = repository.getDirectory().toPath().resolve(Constants.INFO_EXCLUDE);
		List<String> lines = Files.isRegularFile(excludeFile) ? Files.readAllLines(excludeFile, StandardCharsets.UTF_8) : List.of();
		StringBuilder missingLines = new StringBuilder();
		for (String folder : EXCLUDED_HISTORY_FOLDERS) 
			if(lines.stream().noneMatch(line -> line.trim().equals(folder))) missingLines.append(folder).append("\n");
		if(missingLines.isEmpty()) return;
		String content = Files.isRegularFile(excludeFile) ? Files.readString(excludeFile, StandardCharsets.UTF_8) : "";
		if(!content.isEmpty() && !content.endsWith("\n")) content += "\n";
		if(lines.stream().noneMatch(line -> line.equals(EXCLUDE_HEADER))) content += EXCLUDE_HEADER + "\n";
		Files.createDirectories(excludeFile.getParent());
		Files.writeString(excludeFile, content + missingLines, StandardCharsets.UTF_8);
	}

	
	/*
	 * Delete history of directory, which must not be opened : experiment files are not modified.
	 * .git folder is first renamed, so that history is disabled at once or not at all (e.g. if a
	 * file is locked on Windows), then deleted. Default .gitignore and .gitattributes files are
	 * deleted if they have not been modified.
	 * Return null if history has been completely deleted, otherwise the renamed folder which
	 * could not be completely deleted (history is disabled anyway).
	 */
	public static File deleteHistory(File directory) throws SnapshotException {
		if(!isHistorized(directory)) throw new SnapshotException("No history in " + directory);
		Path gitFolder = directory.toPath().resolve(Constants.DOT_GIT);
		Path deletedFolder = directory.toPath().resolve(DELETED_HISTORY_FOLDER_NAME);
		try {
			// Folder left by a previous incomplete deletion
			if(Files.exists(deletedFolder)) deleteRecursively(deletedFolder);
			move(gitFolder, deletedFolder);
		} catch (IOException | InterruptedException e) {
			throw new SnapshotException("Unable to delete history of " + directory, e);
		}
		try {
			List<String> defaultGitIgnores = new ArrayList<>(PREVIOUS_DEFAULT_GIT_IGNORES);
			defaultGitIgnores.add(DEFAULT_GIT_IGNORE);
			deleteIfUnchanged(directory.toPath().resolve(GIT_IGNORE_FILE_NAME), defaultGitIgnores);
			deleteIfUnchanged(directory.toPath().resolve(GIT_ATTRIBUTES_FILE_NAME), List.of(DEFAULT_GIT_ATTRIBUTES));
			Files.deleteIfExists(directory.toPath().resolve(FOLDERS_LIST_FILE_NAME));
			deleteRecursively(deletedFolder);
			return null;
		} catch (IOException | InterruptedException e) {
			return deletedFolder.toFile();
		}
	}
	
	/*
	 * Number of attempts and delay between them for file operations which may fail because
	 * a file has just been written and is locked for a short time by another process
	 * (antivirus, indexing service on Windows)
	 */
	private static final int FILE_OPERATION_ATTEMPTS = 50;
	private static final long FILE_OPERATION_DELAY = 100;
	
	private static void move(Path source, Path target) throws IOException, InterruptedException {
		for (int attempt = 1; ; attempt++) {
			try {
				Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
				return;
			} catch (IOException e) {
				if(attempt >= FILE_OPERATION_ATTEMPTS) throw e;
				Thread.sleep(FILE_OPERATION_DELAY);
			}
		}
	}
	
	/*
	 * Delete folder and its content. Git makes some files read only : they are made writable
	 * first (required on Windows).
	 */
	private static void deleteRecursively(Path folder) throws IOException, InterruptedException {
		for (int attempt = 1; ; attempt++) {
			try {
				if(!Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) return;
				List<Path> paths;
				try (Stream<Path> stream = Files.walk(folder)) {
					paths = stream.sorted(Comparator.reverseOrder()).toList();
				}
				for (Path path : paths) {
					path.toFile().setWritable(true);
					Files.deleteIfExists(path);
				}
				return;
			} catch (IOException e) {
				if(attempt >= FILE_OPERATION_ATTEMPTS) throw e;
				Thread.sleep(FILE_OPERATION_DELAY);
			}
		}
	}
	
	/*
	 * Delete file if its content is one of default contents (i.e. it has not been modified)
	 */
	private static void deleteIfUnchanged(Path file, List<String> defaultContents) throws IOException {
		if(Files.isRegularFile(file) && defaultContents.contains(Files.readString(file, StandardCharsets.UTF_8))) Files.delete(file);
	}
	
	private static void writeIfAbsent(File file, String content) throws IOException {
		if(!file.exists()) Files.writeString(file.toPath(), content, StandardCharsets.UTF_8);
	}
	
	/*
	 * Return historized directory
	 */
	public File getDirectory() {
		return repository.getWorkTree();
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Reading history
	
	/*
	 * Return all snapshots, the most recent first
	 */
	public synchronized List<Snapshot> getSnapshots() throws SnapshotException {
		checkOpen();
		try {
			ObjectId head = repository.resolve(Constants.HEAD);
			if(head == null) return new ArrayList<>(0);
			ArrayList<RevCommit> commits = new ArrayList<>();
			try (RevWalk revWalk = new RevWalk(repository)) {
				revWalk.setFirstParent(true);
				revWalk.markStart(revWalk.parseCommit(head));
				for (RevCommit commit : revWalk) commits.add(commit);
			}
			ArrayList<Snapshot> snapshots = new ArrayList<>(commits.size());
			for (int i = 0; i < commits.size(); i++) snapshots.add(toSnapshot(commits.get(i), commits.size() - i));
			return snapshots;
		} catch (IOException e) {
			throw new SnapshotException("Unable to read history of " + getDirectory(), e);
		}
	}
	
	/*
	 * Return the most recent snapshot, null if there is no snapshot
	 */
	public synchronized Snapshot getLastSnapshot() throws SnapshotException {
		checkOpen();
		List<Snapshot> snapshots = getSnapshots();
		return snapshots.isEmpty() ? null : snapshots.get(0);
	}
	
	/*
	 * Return snapshot of commit id, null if it is not in history
	 */
	public synchronized Snapshot getSnapshot(ObjectId commitId) throws SnapshotException {
		checkOpen();
		for (Snapshot snapshot : getSnapshots()) {
			if(snapshot.commitId().equals(commitId)) return snapshot;
		}
		return null;
	}
	
	/*
	 * Snapshot number is read from message footer, position in history is used for commits
	 * which have no number
	 */
	private static Snapshot toSnapshot(RevCommit commit, int number) {
		String[] paragraphs = commit.getFullMessage().strip().split("\\n\\s*\\n");
		String name = paragraphs[0].strip().replaceAll("\\s*\\n\\s*", " ");
		int descriptionEnd = paragraphs.length;
		if(descriptionEnd > 1 && !commit.getFooterLines().isEmpty()) descriptionEnd--;
		String description = descriptionEnd > 1 ? String.join("\n\n", Arrays.copyOfRange(paragraphs, 1, descriptionEnd)).strip() : "";
		List<String> types = commit.getFooterLines(TYPE_FOOTER);
		SnapshotType type = types.isEmpty() ? SnapshotType.MANUAL : SnapshotType.fromKey(types.get(0).strip());
		ObjectId restoredFromId = null;
		List<String> restoredFrom = commit.getFooterLines(RESTORED_FROM_FOOTER);
		if(!restoredFrom.isEmpty() && ObjectId.isId(restoredFrom.get(0).strip())) restoredFromId = ObjectId.fromString(restoredFrom.get(0).strip());
		List<String> numbers = commit.getFooterLines(NUMBER_FOOTER);
		if(!numbers.isEmpty() && numbers.get(0).strip().matches("[0-9]{1,9}")) number = Integer.parseInt(numbers.get(0).strip());
		PersonIdent author = commit.getAuthorIdent();
		return new Snapshot(commit.copy(), number, name, description, author.getWhenAsInstant(), author.getName(), type, restoredFromId, commit.getTree().copy());
	}
	
	/*
	 * Return true if experiment content differs from the last snapshot
	 * (files which are not historized are not taken into account)
	 */
	public synchronized boolean hasChanges() throws SnapshotException {
		checkOpen();
		updateFoldersList();
		return !isClean();
	}
	
	/*
	 * Return true if files are the files of the last snapshot (folders list is not updated)
	 */
	private boolean isClean() throws SnapshotException {
		try {
			return git.status().call().isClean();
		} catch (GitAPIException e) {
			throw new SnapshotException("Unable to get status of " + getDirectory(), e);
		}
	}
	
	/*
	 * Return changes of experiment content since the last snapshot, sorted by path
	 */
	public synchronized List<FileChange> getChanges() throws SnapshotException {
		checkOpen();
		updateFoldersList();
		try {
			return toChanges(git.status().call(), Set.of());
		} catch (GitAPIException e) {
			throw new SnapshotException("Unable to get status of " + getDirectory(), e);
		}
	}
	
	/*
	 * Changes of status sorted by path, new files which are kept paths (or inside them) excepted
	 */
	private static List<FileChange> toChanges(Status status, Set<String> keptPaths) {
		TreeMap<String, Kind> changes = new TreeMap<>();
		for (String path : status.getModified()) changes.put(path, Kind.MODIFIED);
		for (String path : status.getChanged()) changes.put(path, Kind.MODIFIED);
		for (String path : status.getAdded()) changes.put(path, Kind.ADDED);
		for (String path : status.getUntracked()) if(!isKept(path, keptPaths)) changes.put(path, Kind.ADDED);
		for (String path : status.getRemoved()) changes.put(path, Kind.DELETED);
		for (String path : status.getMissing()) changes.put(path, Kind.DELETED);
		return toFileChanges(changes);
	}
	
	/*
	 * Return paths of files and folders changed since the last snapshot, with all their parent
	 * folders ("" is the experiment folder itself, present if there is any change). Folders
	 * added since the last snapshot are changed, nearest existing parents of removed folders
	 * are changed.
	 */
	public synchronized Set<String> getChangedPaths() throws SnapshotException {
		checkOpen();
		HashSet<String> changedPaths = new HashSet<>();
		List<FileChange> changes = getChanges();
		for (FileChange change : changes) {
			if(FOLDERS_LIST_FILE_NAME.equals(change.path())) addFoldersChanges(changedPaths);
			else addWithParents(change.path(), changedPaths);
		}
		if(!changes.isEmpty()) changedPaths.add("");
		return changedPaths;
	}
	
	private void addFoldersChanges(Set<String> changedPaths) throws SnapshotException {
		HashSet<String> currentFolders = new HashSet<>();
		HashSet<String> previousFolders = new HashSet<>();
		try {
			Path currentList = getDirectory().toPath().resolve(FOLDERS_LIST_FILE_NAME);
			if(Files.isRegularFile(currentList)) currentFolders.addAll(Files.readAllLines(currentList, StandardCharsets.UTF_8));
			Snapshot lastSnapshot = getLastSnapshot();
			if(lastSnapshot != null) {
				try (InputStream inputStream = openFile(lastSnapshot, FOLDERS_LIST_FILE_NAME)) {
					if(inputStream != null) previousFolders.addAll(Arrays.asList(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).split("\n")));
				}
			}
		} catch (IOException e) {
			throw new SnapshotException("Unable to read folders list of " + getDirectory(), e);
		}
		for (String folder : currentFolders) {
			if(!folder.isBlank() && !previousFolders.contains(folder)) addWithParents(folder, changedPaths);
		}
		// Removed folder : its nearest existing parent folder is changed
		for (String folder : previousFolders) {
			if(folder.isBlank() || currentFolders.contains(folder)) continue;
			String parent = getParent(folder);
			while(!parent.isEmpty() && !currentFolders.contains(parent)) parent = getParent(parent);
			addWithParents(parent, changedPaths);
		}
	}
	
	private static void addWithParents(String path, Set<String> paths) {
		for (String currentPath = path; !currentPath.isEmpty(); currentPath = getParent(currentPath)) paths.add(currentPath);
		paths.add("");
	}
	
	private static String getParent(String path) {
		int index = path.lastIndexOf('/');
		return index < 0 ? "" : path.substring(0, index);
	}
	
	/*
	 * Return changes from snapshot "from" to snapshot "to", sorted by path.
	 * "from" may be null : all files of "to" are then added.
	 */
	public synchronized List<FileChange> getChanges(Snapshot from, Snapshot to) throws SnapshotException {
		checkOpen();
		TreeMap<String, Kind> changes = new TreeMap<>();
		try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
			diffFormatter.setRepository(repository);
			diffFormatter.setDetectRenames(false);
			for (DiffEntry diffEntry : diffFormatter.scan(from == null ? null : from.treeId(), to == null ? null : to.treeId())) {
				switch (diffEntry.getChangeType()) {
				case ADD:
					changes.put(diffEntry.getNewPath(), Kind.ADDED);
					break;
				case DELETE:
					changes.put(diffEntry.getOldPath(), Kind.DELETED);
					break;
				default:
					changes.put(diffEntry.getNewPath(), Kind.MODIFIED);
					break;
				}
			}
		} catch (IOException e) {
			throw new SnapshotException("Unable to compare snapshots of " + getDirectory(), e);
		}
		return toFileChanges(changes);
	}
	
	/*
	 * Return changes that restoring snapshot would apply to the current experiment content,
	 * sorted by path : ADDED files will be recreated, MODIFIED files will be replaced,
	 * DELETED files will be removed.
	 * When experiment content differs from the last snapshot, a file modified since the last
	 * snapshot is considered as different from the restored one.
	 */
	public synchronized List<FileChange> getRestoreChanges(Snapshot snapshot) throws SnapshotException {
		checkOpen();
		Snapshot lastSnapshot = getLastSnapshot();
		Map<String, Kind> committedChanges = toMap(getChanges(lastSnapshot, snapshot));
		Map<String, Kind> workingChanges = toMap(getChanges());
		TreeSet<String> paths = new TreeSet<>(committedChanges.keySet());
		paths.addAll(workingChanges.keySet());
		TreeMap<String, Kind> changes = new TreeMap<>();
		for (String path : paths) {
			Kind committedChange = committedChanges.get(path);
			Kind workingChange = workingChanges.get(path);
			boolean inSnapshot;
			boolean inLastSnapshot;
			if(committedChange != null) {
				inSnapshot = committedChange != Kind.DELETED;
				inLastSnapshot = committedChange != Kind.ADDED;
			} else {
				// Same in both snapshots : working change tells whether it exists
				inLastSnapshot = workingChange != Kind.ADDED;
				inSnapshot = inLastSnapshot;
			}
			boolean exists = workingChange == null ? inLastSnapshot : workingChange != Kind.DELETED;
			if(inSnapshot && !exists) changes.put(path, Kind.ADDED);
			else if(!inSnapshot && exists) changes.put(path, Kind.DELETED);
			else if(inSnapshot && exists && (workingChange != null || committedChange == Kind.MODIFIED)) changes.put(path, Kind.MODIFIED);
		}
		return toFileChanges(changes);
	}
	
	/*
	 * Open content of file path (relative, '/' separator) in snapshot, null if file is not in snapshot
	 */
	public synchronized InputStream openFile(Snapshot snapshot, String path) throws SnapshotException {
		checkOpen();
		ObjectLoader objectLoader = getObjectLoader(snapshot, path);
		try {
			return objectLoader == null ? null : objectLoader.openStream();
		} catch (IOException e) {
			throw new SnapshotException("Unable to read " + path + " in snapshot " + snapshot.name(), e);
		}
	}
	
	/*
	 * Return size in bytes of file path (relative, '/' separator) in snapshot, -1 if file is not in snapshot
	 */
	public synchronized long getFileSize(Snapshot snapshot, String path) throws SnapshotException {
		checkOpen();
		ObjectLoader objectLoader = getObjectLoader(snapshot, path);
		return objectLoader == null ? -1 : objectLoader.getSize();
	}
	
	private ObjectLoader getObjectLoader(Snapshot snapshot, String path) throws SnapshotException {
		try (TreeWalk treeWalk = TreeWalk.forPath(repository, path, snapshot.treeId())) {
			if(treeWalk == null || (treeWalk.getRawMode(0) & FileMode.TYPE_MASK) != FileMode.TYPE_FILE) return null;
			return repository.open(treeWalk.getObjectId(0), Constants.OBJ_BLOB);
		} catch (IOException e) {
			throw new SnapshotException("Unable to read " + path + " in snapshot " + snapshot.name(), e);
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Writing history
	
	/*
	 * Create a snapshot of current experiment content (git add -A, git commit).
	 * Return created snapshot or null if content has not changed since the last snapshot.
	 */
	public synchronized Snapshot createSnapshot(String name, String description, SnapshotType type, PersonIdent author) throws SnapshotException {
		checkOpen();
		checkName(name);
		try {
			if(!hasChanges()) return null;
			int number = getNextNumber();
			String message = buildMessage(name, description, type, null, number);
			git.add().addFilepattern(".").call();
			git.add().addFilepattern(".").setUpdate(true).call();
			git.commit().setMessage(message).setAuthor(author).setCommitter(author).setAllowEmpty(false).setSign(false).setNoVerify(true).call();
			saveLastNumber(number);
			return getLastSnapshot();
		} catch (EmptyCommitException e) {
			return null;
		} catch (GitAPIException e) {
			throw new SnapshotException("Unable to create snapshot of " + getDirectory(), e);
		}
	}
	
	/*
	 * Restore whole experiment content to the content of snapshot :
	 * a new snapshot named name, whose content is the content of restored snapshot, is created
	 * after the last one (git commit-tree &lt;snapshot&gt;^{tree} -p HEAD, git reset --hard).
	 * Experiment content must not have changed since the last snapshot (create a snapshot first).
	 * Files which are not historized are not modified.
	 * Return created snapshot or null if experiment content is already the content of snapshot.
	 */
	public synchronized Snapshot restore(Snapshot snapshot, String name, PersonIdent author) throws SnapshotException {
		checkOpen();
		checkName(name);
		checkNoChanges();
		Snapshot lastSnapshot = getLastSnapshot();
		if(lastSnapshot == null) throw new SnapshotException("No snapshot in " + getDirectory());
		if(snapshot.hasSameContent(lastSnapshot)) return null;
		int number = getNextNumber();
		String message = buildMessage(name, null, SnapshotType.RESTORE, snapshot.commitId(), number);
		try (ObjectInserter objectInserter = repository.newObjectInserter()) {
			CommitBuilder commitBuilder = new CommitBuilder();
			commitBuilder.setTreeId(snapshot.treeId());
			commitBuilder.setParentId(lastSnapshot.commitId());
			commitBuilder.setAuthor(author);
			commitBuilder.setCommitter(author);
			commitBuilder.setMessage(message);
			ObjectId commitId = objectInserter.insert(commitBuilder);
			objectInserter.flush();
			saveLastNumber(number);
			checkout(commitId);
			return getLastSnapshot();
		} catch (IOException e) {
			throw new SnapshotException("Unable to restore snapshot " + snapshot.name() + " in " + getDirectory(), e);
		}
	}
	
	/*
	 * Make experiment content the content of the last snapshot again (git reset --hard HEAD,
	 * git clean) : changes since the last snapshot are lost, files which are not historized
	 * are kept. To be used to complete an incomplete restoration
	 * (see IncompleteRestoreException).
	 * Return the last snapshot.
	 */
	public synchronized Snapshot resetToLastSnapshot() throws SnapshotException {
		checkOpen();
		return resetToLastSnapshot(Set.of());
	}
	
	/*
	 * Same as resetToLastSnapshot(), files of keptFiles (relative paths, '/' separator) are kept
	 * too : to complete an incomplete restoration, use IncompleteRestoreException.getKeptFiles()
	 * so that files which were not historized before the restoration are not deleted.
	 */
	public synchronized Snapshot resetToLastSnapshot(Set<String> keptFiles) throws SnapshotException {
		checkOpen();
		Snapshot lastSnapshot = getLastSnapshot();
		if(lastSnapshot == null) throw new SnapshotException("No snapshot in " + getDirectory());
		checkout(lastSnapshot.commitId(), keptFiles);
		return lastSnapshot;
	}
	
	private void checkout(ObjectId commitId) throws SnapshotException {
		checkout(commitId, Set.of());
	}
	
	/*
	 * git reset --hard commitId then git clean : the branch is moved first, then files are
	 * written, then files which are not in commit are deleted.
	 * Files which are not historized before the operation (ignored files) and files of keptFiles
	 * are never deleted, even if they are no longer ignored afterwards (e.g. .gitignore of
	 * commit ignores less files) : they then become new files of experiment (changes since the
	 * last snapshot).
	 * A file locked by another program (antivirus, explorer, Python... on Windows) may make
	 * writing fail, or may silently not be deleted : the operation is retried for 5 seconds,
	 * until experiment content is exactly the content of commit.
	 */
	private void checkout(ObjectId commitId, Set<String> keptFiles) throws SnapshotException {
		// Must be computed before files (and .gitignore) are replaced
		HashSet<String> keptPaths = new HashSet<>();
		try {
			ArrayList<String> paths = new ArrayList<>(keptFiles);
			paths.addAll(git.status().call().getIgnoredNotInIndex());
			for (String path : paths) {
				String normalizedPath = path.replaceAll("/+$", "");
				if(!normalizedPath.isEmpty()) keptPaths.add(normalizedPath);
			}
		} catch (GitAPIException | RuntimeException e) {
			throw new SnapshotException("Unable to get status of " + getDirectory(), e);
		}
		Exception lastException = null;
		long deadline = System.currentTimeMillis() + FILE_OPERATION_ATTEMPTS * FILE_OPERATION_DELAY;
		for (int attempt = 1; attempt == 1 || System.currentTimeMillis() < deadline; attempt++) {
			try {
				git.reset().setMode(ResetType.HARD).setRef(commitId.name()).call();
				// Files which are not in commit and could not be deleted by reset (git clean),
				// files which are not historized are kept
				deleteUntrackedPaths(getCleanablePaths(git.status().call(), keptPaths));
				lastException = null;
			} catch (GitAPIException | IOException | RuntimeException e) {
				lastException = e;
			}
			try {
				if(commitId.equals(repository.resolve(Constants.HEAD)) && toChanges(git.status().call(), keptPaths).isEmpty()) {
					createFolders();
					return;
				}
			} catch (IOException | GitAPIException | RuntimeException e) {
				lastException = e;
			}
			try {
				Thread.sleep(FILE_OPERATION_DELAY);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		Snapshot lastSnapshot = getLastSnapshot();
		if(lastSnapshot == null || !lastSnapshot.commitId().equals(commitId)) {
			throw new SnapshotException("Unable to restore content of " + commitId.name() + " in " + getDirectory(), lastException);
		}
		updateFoldersList();
		Status status;
		try {
			status = git.status().call();
		} catch (GitAPIException e) {
			throw new SnapshotException("Unable to get status of " + getDirectory(), e);
		}
		// Kept files which are no longer ignored : they must still be kept when restoration is completed
		TreeSet<String> remainingKeptFiles = new TreeSet<>();
		for (String path : status.getUntracked()) if(isKept(path, keptPaths)) remainingKeptFiles.add(path);
		throw new IncompleteRestoreException("Content of snapshot " + lastSnapshot.name() + " could not be completely restored in " + getDirectory(), lastException, lastSnapshot, toChanges(status, keptPaths), remainingKeptFiles);
	}
	
	/*
	 * True if path is one of kept paths (files or folders) or is inside one of them
	 */
	private static boolean isKept(String path, Set<String> keptPaths) {
		for (String keptPath : keptPaths) {
			if(isInPath(path, keptPath)) return true;
		}
		return false;
	}
	
	/*
	 * Delete untracked files and folders (relative paths), then their parent folders which become
	 * empty (git clean). Like git clean, folders containing a Git repository are not deleted and
	 * symbolic links are deleted, not followed. Git makes some files read only : they are made
	 * writable first (required on Windows).
	 */
	private void deleteUntrackedPaths(Set<String> paths) throws IOException {
		Path directory = getDirectory().toPath().toAbsolutePath().normalize();
		for (String path : paths) {
			Path file = directory.resolve(path).normalize();
			// Already deleted with its folder
			if(!file.startsWith(directory) || file.equals(directory) || !Files.exists(file, LinkOption.NOFOLLOW_LINKS)) continue;
			if(Files.isDirectory(file, LinkOption.NOFOLLOW_LINKS)) {
				List<Path> folderPaths;
				try (Stream<Path> stream = Files.walk(file)) {
					folderPaths = stream.sorted(Comparator.reverseOrder()).toList();
				}
				if(folderPaths.stream().anyMatch(folderPath -> Constants.DOT_GIT.equals(folderPath.getFileName().toString()))) continue;
				for (Path folderPath : folderPaths) {
					folderPath.toFile().setWritable(true);
					Files.deleteIfExists(folderPath);
				}
			} else {
				file.toFile().setWritable(true);
				Files.deleteIfExists(file);
			}
			for (Path parent = file.getParent(); parent != null && !parent.equals(directory) && isEmptyFolder(parent); parent = parent.getParent()) Files.delete(parent);
		}
	}
	
	private static boolean isEmptyFolder(Path folder) throws IOException {
		if(!Files.isDirectory(folder, LinkOption.NOFOLLOW_LINKS)) return false;
		try (Stream<Path> stream = Files.list(folder)) {
			return stream.findAny().isEmpty();
		}
	}
	
	/*
	 * Untracked files and folders which can be deleted : not kept and, for folders, not
	 * containing a kept path
	 */
	private static Set<String> getCleanablePaths(Status status, Set<String> keptPaths) {
		HashSet<String> cleanablePaths = new HashSet<>();
		for (String file : status.getUntracked()) {
			if(!isKept(file, keptPaths)) cleanablePaths.add(file);
		}
		for (String folder : status.getUntrackedFolders()) {
			if(isKept(folder, keptPaths)) continue;
			boolean containsKeptPath = false;
			for (String keptPath : keptPaths) containsKeptPath |= keptPath.startsWith(folder + "/");
			if(!containsKeptPath) cleanablePaths.add(folder);
		}
		return cleanablePaths;
	}
	
	/*
	 * Restore only file or folder path (relative, '/' separator) to its content in snapshot :
	 * files of path are replaced or recreated, files of path which did not exist in snapshot
	 * are removed, folders of path are those of snapshot. A new snapshot named name, whose
	 * content is the content of the last snapshot except for path, is created after the last one.
	 * Like a whole restoration, files are written with retries and the result is checked
	 * (IncompleteRestoreException).
	 * Experiment content must not have changed since the last snapshot (create a snapshot first).
	 * Return created snapshot or null if path content is already its content in snapshot.
	 */
	public synchronized Snapshot restore(Snapshot snapshot, String path, String name, PersonIdent author) throws SnapshotException {
		checkOpen();
		checkName(name);
		String normalizedPath = path.replace('\\', '/').replaceAll("^/+|/+$", "");
		if(normalizedPath.isEmpty()) return restore(snapshot, name, author);
		checkNoChanges();
		Snapshot lastSnapshot = getLastSnapshot();
		if(lastSnapshot == null) throw new SnapshotException("No snapshot in " + getDirectory());
		try (ObjectReader objectReader = repository.newObjectReader(); ObjectInserter objectInserter = repository.newObjectInserter()) {
			// Content of the last snapshot outside path, content of snapshot inside path
			DirCache dirCache = DirCache.newInCore();
			DirCacheBuilder dirCacheBuilder = dirCache.builder();
			Set<String> outsidePaths = addEntries(dirCacheBuilder, objectReader, lastSnapshot.treeId(), normalizedPath, false);
			Set<String> insidePaths = addEntries(dirCacheBuilder, objectReader, snapshot.treeId(), normalizedPath, true);
			// A file is now where a parent folder of path must be (e.g. restore "A/B/f" while "A" is a file)
			if(!insidePaths.isEmpty()) {
				for (String parent = getParent(normalizedPath); !parent.isEmpty(); parent = getParent(parent)) {
					if(outsidePaths.contains(parent)) throw new SnapshotException("Unable to restore " + path + " from snapshot " + snapshot.name() + " : " + parent + " is now a file, it must be removed or renamed first");
				}
			}
			// Folders list : folders of path are those of snapshot
			if(!isInPath(FOLDERS_LIST_FILE_NAME, normalizedPath)) {
				List<String> lastFolders = readFoldersList(lastSnapshot);
				List<String> snapshotFolders = readFoldersList(snapshot);
				TreeSet<String> folders = new TreeSet<>();
				for (String folder : lastFolders) if(!isInPath(folder, normalizedPath)) folders.add(folder);
				for (String folder : snapshotFolders) if(isInPath(folder, normalizedPath) || normalizedPath.startsWith(folder + "/")) folders.add(folder);
				ArrayList<String> sortedFolders = new ArrayList<>(folders);
				Collections.sort(sortedFolders);
				StringBuilder content = new StringBuilder();
				for (String folder : sortedFolders) content.append(folder).append('\n');
				DirCacheEntry foldersListEntry = new DirCacheEntry(FOLDERS_LIST_FILE_NAME);
				foldersListEntry.setFileMode(FileMode.REGULAR_FILE);
				foldersListEntry.setObjectId(objectInserter.insert(Constants.OBJ_BLOB, content.toString().getBytes(StandardCharsets.UTF_8)));
				dirCacheBuilder.add(foldersListEntry);
			}
			dirCacheBuilder.finish();
			ObjectId treeId = dirCache.writeTree(objectInserter);
			if(treeId.equals(lastSnapshot.treeId())) return null;
			int number = getNextNumber();
			CommitBuilder commitBuilder = new CommitBuilder();
			commitBuilder.setTreeId(treeId);
			commitBuilder.setParentId(lastSnapshot.commitId());
			commitBuilder.setAuthor(author);
			commitBuilder.setCommitter(author);
			commitBuilder.setMessage(buildMessage(name, null, SnapshotType.RESTORE, snapshot.commitId(), number));
			ObjectId commitId = objectInserter.insert(commitBuilder);
			objectInserter.flush();
			saveLastNumber(number);
			checkout(commitId);
			return getLastSnapshot();
		} catch (IOException | DirCacheNameConflictException e) {
			throw new SnapshotException("Unable to restore " + path + " from snapshot " + snapshot.name() + " in " + getDirectory(), e);
		}
	}
	
	private static boolean isInPath(String entryPath, String path) {
		return entryPath.equals(path) || entryPath.startsWith(path + "/");
	}
	
	/*
	 * Add files of tree which are inside (or outside) path, except folders list. Return their paths.
	 */
	private static Set<String> addEntries(DirCacheBuilder dirCacheBuilder, ObjectReader objectReader, ObjectId treeId, String path, boolean insidePath) throws IOException {
		HashSet<String> paths = new HashSet<>();
		try (TreeWalk treeWalk = new TreeWalk(objectReader)) {
			treeWalk.addTree(treeId);
			treeWalk.setRecursive(true);
			while (treeWalk.next()) {
				String entryPath = treeWalk.getPathString();
				if(FOLDERS_LIST_FILE_NAME.equals(entryPath) && !FOLDERS_LIST_FILE_NAME.equals(path)) continue;
				if(isInPath(entryPath, path) != insidePath) continue;
				DirCacheEntry dirCacheEntry = new DirCacheEntry(entryPath);
				dirCacheEntry.setFileMode(treeWalk.getFileMode(0));
				dirCacheEntry.setObjectId(treeWalk.getObjectId(0));
				dirCacheBuilder.add(dirCacheEntry);
				paths.add(entryPath);
			}
		}
		return paths;
	}
	
	private List<String> readFoldersList(Snapshot snapshot) throws SnapshotException, IOException {
		ArrayList<String> folders = new ArrayList<>();
		try (InputStream inputStream = openFile(snapshot, FOLDERS_LIST_FILE_NAME)) {
			if(inputStream == null) return folders;
			for (String folder : new String(inputStream.readAllBytes(), StandardCharsets.UTF_8).split("\n")) if(!folder.isBlank()) folders.add(folder);
		}
		return folders;
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Differences of a file
	
	/* Text files larger than this are not compared */
	public static final int MAXIMUM_DIFF_SIZE = 5 * 1024 * 1024;
	
	/**
	 * Differences of a file between two states.
	 * @param result kind of result
	 * @param text unified diff (lines starting with ' ', '-' or '+', hunks headers starting with @@), empty if result is not DIFFERENCES
	 */
	public record FileDiff(Result result, String text) {
		public enum Result {DIFFERENCES, IDENTICAL, BINARY, TOO_LARGE}
	}
	
	/*
	 * Return differences of file path (relative, '/' separator) from snapshot "from" to snapshot
	 * "to". "from" may be null when there is no previous snapshot (e.g. first snapshot) : the file
	 * is then compared to an empty file. "to" may be null : current file of experiment is then used.
	 * A missing file is compared as an empty file.
	 */
	public synchronized FileDiff getDiff(Snapshot from, Snapshot to, String path) throws SnapshotException {
		checkOpen();
		try {
			byte[] oldContent = from == null ? new byte[0] : readContent(from, path);
			byte[] newContent = readContent(to, path);
			if(oldContent == null || newContent == null) return new FileDiff(FileDiff.Result.TOO_LARGE, "");
			if(Arrays.equals(oldContent, newContent)) return new FileDiff(FileDiff.Result.IDENTICAL, "");
			if(RawText.isBinary(oldContent) || RawText.isBinary(newContent)) return new FileDiff(FileDiff.Result.BINARY, "");
			RawText oldText = new RawText(oldContent);
			RawText newText = new RawText(newContent);
			EditList edits = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM).diff(RawTextComparator.DEFAULT, oldText, newText);
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			try (DiffFormatter diffFormatter = new DiffFormatter(outputStream)) {
				diffFormatter.setContext(3);
				diffFormatter.format(edits, oldText, newText);
			}
			return new FileDiff(FileDiff.Result.DIFFERENCES, outputStream.toString(StandardCharsets.UTF_8));
		} catch (IOException e) {
			throw new SnapshotException("Unable to compare " + path + " in " + getDirectory(), e);
		}
	}
	
	/*
	 * Content of file path in snapshot (current file if snapshot is null), empty if file does not
	 * exist, null if file is too large to be compared
	 */
	private byte[] readContent(Snapshot snapshot, String path) throws SnapshotException, IOException {
		if(snapshot == null) {
			Path file = getDirectory().toPath().resolve(path);
			if(!Files.isRegularFile(file)) return new byte[0];
			if(Files.size(file) > MAXIMUM_DIFF_SIZE) return null;
			return Files.readAllBytes(file);
		}
		long size = getFileSize(snapshot, path);
		if(size < 0) return new byte[0];
		if(size > MAXIMUM_DIFF_SIZE) return null;
		try (InputStream inputStream = openFile(snapshot, path)) {
			return inputStream.readAllBytes();
		}
	}
	
	/////////////////////////////////////////////////////////////////////////////
	// Snapshot deletion
	
	/*
	 * Permanently delete snapshot from history. Experiment files are not modified.
	 * As each snapshot contains the whole experiment content, more recent snapshots are only
	 * re-attached to the snapshot preceding the deleted one : their contents, names, numbers,
	 * dates and authors are kept, references of restorations to rewritten snapshots are updated
	 * (references to the deleted snapshot are removed).
	 * If the last snapshot is deleted, its changes become changes of the current state.
	 * File versions which only existed in deleted snapshot are purged from storage.
	 * The only snapshot of history can't be deleted (delete history instead).
	 */
	public synchronized void deleteSnapshot(Snapshot snapshot) throws SnapshotException {
		checkOpen();
		deleteSnapshot(snapshot, false);
	}
	
	/*
	 * Same as deleteSnapshot(snapshot), but if restorePreviousContent is true, snapshot must
	 * be the last one and experiment files are then restored to the content of the snapshot
	 * which becomes the last one (e.g. to cancel a restoration). Experiment content must not
	 * have changed since snapshot (these changes would be lost).
	 * IncompleteRestoreException is thrown if files could not all be restored (snapshot is
	 * deleted anyway), see resetToLastSnapshot.
	 */
	public synchronized void deleteSnapshot(Snapshot snapshot, boolean restorePreviousContent) throws SnapshotException {
		checkOpen();
		if(restorePreviousContent) {
			Snapshot lastSnapshot = getLastSnapshot();
			if(lastSnapshot == null || !lastSnapshot.commitId().equals(snapshot.commitId())) throw new SnapshotException("Snapshot " + snapshot.name() + " is not the last snapshot of " + getDirectory());
			checkNoChanges();
		}
		deleteSnapshotFromHistory(snapshot);
		if(restorePreviousContent) checkout(getLastSnapshot().commitId());
	}
	
	private void deleteSnapshotFromHistory(Snapshot snapshot) throws SnapshotException {
		List<Snapshot> snapshots = getSnapshots();
		int index = -1;
		for (int i = 0; i < snapshots.size(); i++) {
			if(snapshots.get(i).commitId().equals(snapshot.commitId())) index = i;
		}
		if(index < 0) throw new SnapshotException("Snapshot " + snapshot.name() + " is not in history of " + getDirectory());
		if(snapshots.size() == 1) throw new SnapshotException("The only snapshot of " + getDirectory() + " can't be deleted");
		try (RevWalk revWalk = new RevWalk(repository); ObjectInserter objectInserter = repository.newObjectInserter()) {
			ObjectId lastCommitId = snapshots.get(0).commitId();
			ObjectId newParentId = index + 1 < snapshots.size() ? snapshots.get(index + 1).commitId() : null;
			HashMap<ObjectId, ObjectId> rewrittenIds = new HashMap<>();
			// More recent snapshots, the oldest first
			for (int i = index - 1; i >= 0; i--) {
				RevCommit commit = revWalk.parseCommit(snapshots.get(i).commitId());
				CommitBuilder commitBuilder = new CommitBuilder();
				commitBuilder.setTreeId(commit.getTree());
				if(newParentId != null) commitBuilder.setParentId(newParentId);
				commitBuilder.setAuthor(commit.getAuthorIdent());
				commitBuilder.setCommitter(commit.getCommitterIdent());
				commitBuilder.setEncoding(commit.getEncoding());
				commitBuilder.setMessage(updateRestoredFrom(commit.getFullMessage(), snapshot.commitId(), rewrittenIds));
				newParentId = objectInserter.insert(commitBuilder);
				rewrittenIds.put(commit.copy(), newParentId);
			}
			objectInserter.flush();
			if(index == 0) {
				// Last snapshot : branch and index go back to the previous snapshot, files are kept
				git.reset().setMode(ResetType.MIXED).setRef(newParentId.name()).call();
			} else {
				RefUpdate refUpdate = repository.updateRef(Constants.HEAD);
				refUpdate.setNewObjectId(newParentId);
				refUpdate.setExpectedOldObjectId(lastCommitId);
				refUpdate.disableRefLog();
				RefUpdate.Result result = refUpdate.forceUpdate();
				if(result != RefUpdate.Result.FORCED && result != RefUpdate.Result.FAST_FORWARD && result != RefUpdate.Result.NEW) {
					throw new SnapshotException("Unable to delete snapshot " + snapshot.name() + " in " + getDirectory() + " : " + result);
				}
			}
		} catch (IOException | GitAPIException e) {
			throw new SnapshotException("Unable to delete snapshot " + snapshot.name() + " in " + getDirectory(), e);
		}
		purge();
	}
	
	/*
	 * Update restoration footer : references to rewritten snapshots are updated, reference to
	 * deleted snapshot is removed
	 */
	private static String updateRestoredFrom(String message, ObjectId deletedId, Map<ObjectId, ObjectId> rewrittenIds) {
		StringBuilder newMessage = new StringBuilder();
		for (String line : message.split("\n", -1)) {
			if(line.startsWith(RESTORED_FROM_FOOTER + ":")) {
				String value = line.substring(RESTORED_FROM_FOOTER.length() + 1).strip();
				if(ObjectId.isId(value)) {
					ObjectId restoredId = ObjectId.fromString(value);
					if(restoredId.equals(deletedId)) continue;
					if(rewrittenIds.containsKey(restoredId)) line = RESTORED_FROM_FOOTER + ": " + rewrittenIds.get(restoredId).name();
				}
			}
			newMessage.append(line).append('\n');
		}
		return newMessage.substring(0, newMessage.length() - 1);
	}
	
	/*
	 * Remove objects which are no longer in history (deleted snapshots) : ORIG_HEAD and
	 * reflogs which could still reference them are removed, then git gc prunes them.
	 */
	private void purge() throws SnapshotException {
		try {
			repository.writeOrigHead(null);
			deleteRecursively(repository.getDirectory().toPath().resolve(Constants.LOGS));
			git.gc().setExpire(Instant.now()).call();
			saveLastOptimizationNumber();
		} catch (IOException | GitAPIException e) {
			throw new SnapshotException("Unable to purge history of " + getDirectory(), e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
	
	/*
	 * Return paths of files of snapshot whose version (content) exists in no other snapshot :
	 * these versions are lost if snapshot is deleted
	 */
	public synchronized List<String> getFilesOnlyIn(Snapshot snapshot) throws SnapshotException {
		checkOpen();
		try (ObjectReader objectReader = repository.newObjectReader()) {
			HashSet<ObjectId> otherVersions = new HashSet<>();
			for (Snapshot otherSnapshot : getSnapshots()) {
				// Snapshots with the same content keep all versions of snapshot : they are counted too
				if(otherSnapshot.commitId().equals(snapshot.commitId())) continue;
				try (TreeWalk treeWalk = new TreeWalk(objectReader)) {
					treeWalk.addTree(otherSnapshot.treeId());
					treeWalk.setRecursive(true);
					while (treeWalk.next()) otherVersions.add(treeWalk.getObjectId(0));
				}
			}
			ArrayList<String> paths = new ArrayList<>();
			try (TreeWalk treeWalk = new TreeWalk(objectReader)) {
				treeWalk.addTree(snapshot.treeId());
				treeWalk.setRecursive(true);
				while (treeWalk.next()) {
					if(!otherVersions.contains(treeWalk.getObjectId(0))) paths.add(treeWalk.getPathString());
				}
			}
			return paths;
		} catch (IOException e) {
			throw new SnapshotException("Unable to read snapshot " + snapshot.name() + " in " + getDirectory(), e);
		}
	}
	
	/*
	 * Write the list of experiment folders, except folders which are not historized, if it has
	 * changed : Git does not record empty folders (e.g. trials which are not acquired yet), this
	 * list is recorded instead.
	 */
	private void updateFoldersList() throws SnapshotException {
		try {
			ArrayList<String> folders = new ArrayList<>();
			try (TreeWalk treeWalk = new TreeWalk(repository)) {
				treeWalk.addTree(new FileTreeIterator(repository));
				treeWalk.setRecursive(false);
				while (treeWalk.next()) {
					if(!treeWalk.isSubtree()) continue;
					WorkingTreeIterator iterator = treeWalk.getTree(0, WorkingTreeIterator.class);
					if(iterator == null || iterator.isEntryIgnored()) continue;
					folders.add(treeWalk.getPathString());
					treeWalk.enterSubtree();
				}
			}
			Collections.sort(folders);
			StringBuilder content = new StringBuilder();
			for (String folder : folders) content.append(folder).append('\n');
			Path file = getDirectory().toPath().resolve(FOLDERS_LIST_FILE_NAME);
			if(Files.isRegularFile(file) && Files.readString(file, StandardCharsets.UTF_8).equals(content.toString())) return;
			Files.writeString(file, content.toString(), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new SnapshotException("Unable to list folders of " + getDirectory(), e);
		}
	}
	
	/*
	 * Create folders of folders list (empty folders of restored snapshot)
	 */
	private void createFolders() throws SnapshotException {
		Path directory = getDirectory().toPath().toAbsolutePath().normalize();
		Path file = directory.resolve(FOLDERS_LIST_FILE_NAME);
		try {
			if(!Files.isRegularFile(file)) return;
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				if(line.isBlank()) continue;
				Path folder = directory.resolve(line).normalize();
				if(folder.startsWith(directory) && !folder.equals(directory)) Files.createDirectories(folder);
			}
		} catch (IOException e) {
			throw new SnapshotException("Unable to create folders of " + getDirectory(), e);
		}
	}
	
	/*
	 * Optimize history storage (git gc)
	 */
	public synchronized void optimize() throws SnapshotException {
		checkOpen();
		try {
			git.gc().call();
		} catch (GitAPIException e) {
			throw new SnapshotException("Unable to optimize history of " + getDirectory(), e);
		}
		saveLastOptimizationNumber();
	}
	
	/*
	 * Return number of snapshots created since history storage was last optimized (snapshots of
	 * any type, numbers of deleted snapshots are counted), all snapshots if it never was
	 */
	public synchronized int getSnapshotsNumberSinceOptimization() throws SnapshotException {
		checkOpen();
		int lastOptimizationNumber = repository.getConfig().getInt(CONFIG_SECTION, CONFIG_KEY_LAST_OPTIMIZATION_NUMBER, 0);
		return Math.max(0, getNextNumber() - 1 - lastOptimizationNumber);
	}
	
	private void saveLastOptimizationNumber() throws SnapshotException {
		try {
			StoredConfig config = repository.getConfig();
			config.setInt(CONFIG_SECTION, null, CONFIG_KEY_LAST_OPTIMIZATION_NUMBER, getNextNumber() - 1);
			config.save();
		} catch (IOException e) {
			throw new SnapshotException("Unable to save optimization number in " + getDirectory(), e);
		}
	}
	
	/*
	 * Action run on the .git folder while no other operation can modify the history
	 */
	@FunctionalInterface
	public interface ExclusiveAction<E extends Exception> {
		void run(File gitFolder) throws E;
	}
	
	/*
	 * Run action on the .git folder (e.g. to copy it) while no snapshot can be created,
	 * restored or deleted and history can't be optimized
	 */
	public synchronized <E extends Exception> void runExclusive(ExclusiveAction<E> action) throws E {
		action.run(repository.getDirectory());
	}
	
	/*
	 * Close history and release its files : JGit keeps pack files opened in a global cache even
	 * after the repository is closed, and on Windows an opened file can't be deleted or moved
	 * (deletion or renaming of the experiment, deletion of the history).
	 * A closed history can't be used anymore : its methods throw a SnapshotException.
	 */
	@Override
	public synchronized void close() {
		if(closed) return;
		closed = true;
		git.close();
		releaseCachedFiles();
	}
	
	/*
	 * Return true if history has been closed
	 */
	public synchronized boolean isClosed() {
		return closed;
	}
	
	/*
	 * A closed history must not be used : JGit would open its files again (e.g. pack files), which
	 * would prevent deleting or renaming the experiment on Windows. Get it again from SnapshotManager.
	 */
	private void checkOpen() throws SnapshotException {
		if(closed) throw new SnapshotException("History of " + getDirectory() + " is closed");
	}
	
	/*
	 * Close pack files kept opened by JGit global cache (of all repositories : they are opened
	 * again when needed, reading another history at the same time is not disturbed)
	 */
	public static void releaseCachedFiles() {
		new WindowCacheConfig().install();
	}
	
	private void checkNoChanges() throws SnapshotException {
		if(hasChanges()) throw new SnapshotException("Content of " + getDirectory() + " has changed since the last snapshot");
	}
	
	private static void checkName(String name) {
		if(name == null || name.isBlank()) throw new IllegalArgumentException("Snapshot name is empty");
	}
	
	/*
	 * Message : name, empty line, optional description and empty line, footers
	 */
	static String buildMessage(String name, String description, SnapshotType type, ObjectId restoredFromId, int number) {
		checkName(name);
		StringBuilder message = new StringBuilder(name.strip().replaceAll("\\s*\\R\\s*", " "));
		if(description != null && !description.isBlank()) message.append("\n\n").append(description.strip());
		message.append("\n\n").append(TYPE_FOOTER).append(": ").append(type.getKey()).append('\n');
		message.append(NUMBER_FOOTER).append(": ").append(number).append('\n');
		if(restoredFromId != null) message.append(RESTORED_FROM_FOOTER).append(": ").append(restoredFromId.name()).append('\n');
		return message.toString();
	}
	
	/*
	 * Number of the next snapshot : numbers of deleted snapshots are not reused
	 */
	public synchronized int getNextNumber() throws SnapshotException {
		checkOpen();
		int lastNumber = repository.getConfig().getInt(CONFIG_SECTION, CONFIG_KEY_LAST_NUMBER, 0);
		for (Snapshot snapshot : getSnapshots()) lastNumber = Math.max(lastNumber, snapshot.number());
		return lastNumber + 1;
	}
	
	private void saveLastNumber(int number) throws SnapshotException {
		try {
			StoredConfig config = repository.getConfig();
			config.setInt(CONFIG_SECTION, null, CONFIG_KEY_LAST_NUMBER, number);
			config.save();
		} catch (IOException e) {
			throw new SnapshotException("Unable to save snapshot number in " + getDirectory(), e);
		}
	}
	
	private static Map<String, Kind> toMap(List<FileChange> fileChanges) {
		TreeMap<String, Kind> map = new TreeMap<>();
		for (FileChange fileChange : fileChanges) map.put(fileChange.path(), fileChange.kind());
		return map;
	}
	
	private static List<FileChange> toFileChanges(TreeMap<String, Kind> changes) {
		ArrayList<FileChange> fileChanges = new ArrayList<>(changes.size());
		for (Map.Entry<String, Kind> change : changes.entrySet()) fileChanges.add(new FileChange(change.getKey(), change.getValue()));
		return Collections.unmodifiableList(fileChanges);
	}

}
