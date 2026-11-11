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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import fr.univamu.ism.docometre.dacqsystems.ImagesFile.ImageLine;

public class ImagesFileTest {
	
	private static List<String> sampleFile() {
		return Arrays.asList("# Images of the session",
							 "",
							 "Images/images/left.png    # trial 1",
							 "   # second block",
							 "  Images/images/right.png",
							 "/stimuli/center.jpg # trials 3 and following");
	}
	
	@Test
	public void fileNameIsGenericNamePlusExtension() {
		assertEquals("stimuli.images", ImagesFile.getFileName("stimuli"));
		assertEquals("stimuli.images", ImagesFile.getFileName(" stimuli "));
		assertEquals("stimuli.images", ImagesFile.getFileName("stimuli.images"));
		assertEquals("images.txt", ImagesFile.getFileName("images.txt"));
	}
	
	@Test
	public void commentsAndEmptyLinesAreIgnoredAndLineNumbersKept() {
		List<ImageLine> imageLines = ImagesFile.getImageLines(sampleFile());
		assertEquals(3, imageLines.size());
		assertEquals("Images/images/left.png", imageLines.get(0).getPath());
		assertEquals(3, imageLines.get(0).getLineNumber());
		assertEquals("Images/images/right.png", imageLines.get(1).getPath());
		assertEquals(5, imageLines.get(1).getLineNumber());
		assertEquals("/stimuli/center.jpg", imageLines.get(2).getPath());
		assertEquals(6, imageLines.get(2).getLineNumber());
	}
	
	@Test
	public void lastLineIsUsedWhenThereAreMoreTrialsThanLines() {
		List<ImageLine> imageLines = ImagesFile.getImageLines(sampleFile());
		assertEquals("Images/images/left.png", ImagesFile.getTrialImageLine(imageLines, 1).getPath());
		assertEquals("Images/images/right.png", ImagesFile.getTrialImageLine(imageLines, 2).getPath());
		assertEquals("/stimuli/center.jpg", ImagesFile.getTrialImageLine(imageLines, 3).getPath());
		assertEquals("/stimuli/center.jpg", ImagesFile.getTrialImageLine(imageLines, 10).getPath());
		assertEquals("Images/images/left.png", ImagesFile.getTrialImageLine(imageLines, 0).getPath());
	}
	
	@Test
	public void noTrialLineWhenFileHasOnlyComments() {
		List<ImageLine> imageLines = ImagesFile.getImageLines(Arrays.asList("# comment", "", "  "));
		assertTrue(imageLines.isEmpty());
		assertNull(ImagesFile.getTrialImageLine(imageLines, 1));
	}
	
	@Test
	public void commentIsExtracted() {
		assertEquals("# trial 1", ImagesFile.getComment("a.png # trial 1"));
		assertEquals("", ImagesFile.getComment("a.png"));
		assertEquals("# comment", ImagesFile.getComment("# comment"));
	}
	
	@Test
	public void setPathKeepsCommentAndAlignment() {
		assertEquals("b.png    # trial 1", ImagesFile.setPath("a.png    # trial 1", "b.png"));
		assertEquals("  b.png", ImagesFile.setPath("  a.png", "b.png"));
		assertEquals("b.png", ImagesFile.setPath("a.png", " b.png "));
		assertEquals("b.png # comment", ImagesFile.setPath("# comment", "b.png"));
		assertEquals("b.png", ImagesFile.setPath("", "b.png"));
		assertEquals("# trial 1", ImagesFile.setPath("a.png    # trial 1", ""));
	}
	
	@Test
	public void setCommentKeepsPath() {
		assertEquals("a.png # trial 1", ImagesFile.setComment("a.png", "trial 1"));
		assertEquals("a.png # first", ImagesFile.setComment("a.png    # trial 1", "# first"));
		assertEquals("a.png", ImagesFile.setComment("a.png # trial 1", ""));
		assertEquals("  a.png", ImagesFile.setComment("  a.png  # trial 1", " "));
		assertEquals("# comment", ImagesFile.setComment("", "comment"));
	}
	
	@Test
	public void relativePathsAreResolvedFromWorkspace(@TempDir Path workspace) {
		File workspaceFolder = workspace.toFile();
		assertEquals(new File(workspaceFolder, "Images/images/left.png"), ImagesFile.getImageFile("Images/images/left.png", workspaceFolder));
		File absolute = workspace.resolve("other").toAbsolutePath().toFile();
		assertEquals(absolute, ImagesFile.getImageFile(absolute.getPath(), workspaceFolder));
	}
	
	@Test
	public void pathIsRelativeToWorkspaceOnlyInsideWorkspace(@TempDir Path folder) {
		File workspaceFolder = folder.resolve("workspace").toFile();
		File inside = new File(workspaceFolder, "Images" + File.separator + "images" + File.separator + "left.png");
		assertEquals("Images/images/left.png", ImagesFile.getPath(inside, workspaceFolder));
		File outside = folder.resolve("stimuli").resolve("center.jpg").toFile();
		assertEquals(outside.getAbsolutePath(), ImagesFile.getPath(outside, workspaceFolder));
	}
	
	@Test
	public void latin1FilesAreRead(@TempDir Path folder) throws Exception {
		Path utf8File = folder.resolve("utf8.images");
		Files.write(utf8File, "Images/é.png # essai 1".getBytes(StandardCharsets.UTF_8));
		assertEquals("Images/é.png", ImagesFile.getPath(ImagesFile.readLines(utf8File).get(0)));
		Path latin1File = folder.resolve("latin1.images");
		Files.write(latin1File, "Images/é.png # essai 1".getBytes(StandardCharsets.ISO_8859_1));
		assertEquals("Images/é.png", ImagesFile.getPath(ImagesFile.readLines(latin1File).get(0)));
	}
	
}
