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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [11/11/2026]
 ******************************************************************************/
package fr.univamu.ism.docometre.dacqsystems;

import java.io.File;
import java.io.IOException;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reading of the lines of an images file (.images), used by image charts, without
 * Eclipse dependencies : the workspace folder is given by the caller.
 * <p>
 * Format of the file :
 * <ul>
 * <li># starts a comment, until the end of the line : a line can be a comment or a
 * comment can follow the path of an image. Comment lines and empty lines are ignored ;</li>
 * <li>uncommented line N holds the path of the image of trial N (N starting at 1). When
 * there are more trials than lines, last line is used for the remaining trials ;</li>
 * <li>a path is absolute or relative to the workspace folder, it can't contain #.</li>
 * </ul>
 */
public final class ImagesFile {
	
	/**
	 * Images files extension
	 */
	public static final String EXTENSION = ".images";
	
	/**
	 * Images files with this extension (created by first versions of image charts) are still accepted
	 */
	public static final String LEGACY_EXTENSION = ".txt";
	
	/**
	 * Extensions of the images files proposed by dialogs, as SWT reads them
	 */
	public static final String[] IMAGES_EXTENSIONS = {"*.png;*.PNG;*.jpg;*.JPG;*.jpeg;*.JPEG;*.bmp;*.BMP;*.gif;*.GIF;*.ico;*.ICO;*.tif;*.TIF;*.tiff;*.TIFF", "*.*"};
	
	/**
	 * End of line comment
	 */
	private static final String COMMENT_REGEXP = "#.*$";
	
	/**
	 * An uncommented line of an images file
	 */
	public static final class ImageLine {
		
		private final int lineNumber;
		private final String path;
		
		private ImageLine(int lineNumber, String path) {
			this.lineNumber = lineNumber;
			this.path = path;
		}
		
		/**
		 * @return the number of this line in the file, comment and empty lines included (starting at 1)
		 */
		public int getLineNumber() {
			return lineNumber;
		}
		
		/**
		 * @return the path of the image, as written in the file (comment and white spaces removed)
		 */
		public String getPath() {
			return path;
		}
		
	}
	
	private ImagesFile() {
	}
	
	/**
	 * @param genericName the generic name of the images files, as given in an image chart configuration
	 * @return the name of the images file : genericName + {@link #EXTENSION}, or genericName if it already ends
	 * with {@link #EXTENSION} or {@link #LEGACY_EXTENSION}
	 */
	public static String getFileName(String genericName) {
		genericName = genericName.trim();
		if(genericName.endsWith(EXTENSION) || genericName.endsWith(LEGACY_EXTENSION)) return genericName;
		return genericName + EXTENSION;
	}
	
	/**
	 * Reads the lines of a file in UTF-8, or in ISO-8859-1 if it is not an UTF-8 file
	 * (edited with a Latin-1 editor for example).
	 * @param file the file to read
	 * @return the lines of the file
	 * @throws IOException if the file can't be read
	 */
	public static List<String> readLines(Path file) throws IOException {
		try {
			return Files.readAllLines(file, StandardCharsets.UTF_8);
		} catch (MalformedInputException e) {
			return Files.readAllLines(file, StandardCharsets.ISO_8859_1);
		}
	}
	
	/**
	 * @param line a line of an images file
	 * @return the path of the image of this line, without comment and surrounding white spaces : empty for a
	 * comment line or an empty line
	 */
	public static String getPath(String line) {
		return line.replaceAll(COMMENT_REGEXP, "").trim();
	}
	
	/**
	 * @param line a line of an images file
	 * @return the comment of this line, # included, empty if there is no comment
	 */
	public static String getComment(String line) {
		int index = line.indexOf("#");
		return index > -1 ? line.substring(index) : "";
	}
	
	/**
	 * @param line a line of an images file
	 * @param path the new path of the image of this line
	 * @return the line with this new path : the comment of the line, and the white spaces before it, are kept
	 */
	public static String setPath(String line, String path) {
		path = path.trim();
		String oldPath = getPath(line);
		if(oldPath.isEmpty()) {
			String comment = getComment(line);
			if(comment.isEmpty() || path.isEmpty()) return path + comment;
			return path + " " + comment;
		}
		int index = line.indexOf(oldPath);
		String end = line.substring(index + oldPath.length());
		if(path.isEmpty()) end = end.stripLeading();
		return line.substring(0, index) + path + end;
	}
	
	/**
	 * @param line a line of an images file
	 * @param comment the new comment of this line, without #, empty to remove the comment
	 * @return the line with this new comment : the path of the line is kept
	 */
	public static String setComment(String line, String comment) {
		comment = comment.trim();
		if(comment.startsWith("#")) comment = comment.substring(1).trim();
		int index = line.indexOf("#");
		String start = (index > -1 ? line.substring(0, index) : line).stripTrailing();
		if(comment.isEmpty()) return start;
		if(start.isEmpty()) return "# " + comment;
		return start + " # " + comment;
	}
	
	/**
	 * @param lines the lines of an images file
	 * @return the uncommented lines (images paths), with their number in the file
	 */
	public static List<ImageLine> getImageLines(List<String> lines) {
		List<ImageLine> imageLines = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			String path = getPath(lines.get(i));
			if(!path.isEmpty()) imageLines.add(new ImageLine(i + 1, path));
		}
		return imageLines;
	}
	
	/**
	 * @param imageLines the uncommented lines of an images file (see {@link #getImageLines(List)})
	 * @param trialNumber a trial number (starting at 1, values lower than 1 give trial 1)
	 * @return the line of this trial, the last line if there are less lines than trials, null if there is no line
	 */
	public static ImageLine getTrialImageLine(List<ImageLine> imageLines, int trialNumber) {
		if(imageLines.isEmpty()) return null;
		int index = Math.min(Math.max(trialNumber, 1), imageLines.size()) - 1;
		return imageLines.get(index);
	}
	
	/**
	 * @param path the path of an image, as written in an images file
	 * @param workspaceFolder the workspace folder
	 * @return the image file : path if it is absolute, path relative to the workspace folder otherwise
	 */
	public static File getImageFile(String path, File workspaceFolder) {
		File imageFile = new File(path);
		if(imageFile.isAbsolute()) return imageFile;
		return new File(workspaceFolder, path);
	}
	
	/**
	 * @param imageFile an image file
	 * @param workspaceFolder the workspace folder
	 * @return the path to write in an images file : relative to the workspace folder (with / separators)
	 * if the image is in the workspace, absolute otherwise
	 */
	public static String getPath(File imageFile, File workspaceFolder) {
		Path imagePath = imageFile.toPath().toAbsolutePath().normalize();
		Path workspacePath = workspaceFolder.toPath().toAbsolutePath().normalize();
		if(!imagePath.startsWith(workspacePath)) return imagePath.toString();
		return workspacePath.relativize(imagePath).toString().replace(File.separatorChar, '/');
	}
	
}
