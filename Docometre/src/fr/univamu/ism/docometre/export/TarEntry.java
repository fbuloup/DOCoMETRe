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
 *  - Frank Buloup - frank.buloup@univ-amu.fr - initial API and implementation [25/03/2020]
 ******************************************************************************/
package fr.univamu.ism.docometre.export;

/**
 * Representation of a file in a tar archive.
 *
 * @since 3.1
 */
public class TarEntry implements Cloneable
{
	private String name;
	private long mode, time, size;
	private int type;
	int filepos;

	/**
	 * Entry type for normal files.
	 */
	public static final int FILE = '0';

	/**
	 * Entry type for directories.
	 */
	public static final int DIRECTORY = '5';

	/**
	 * Create a new TarEntry for a file of the given name at the
	 * given position in the file.
	 *
	 * @param name filename
	 * @param pos position in the file in bytes
	 */
	TarEntry(String name, int pos) {
		this.name = name;
		mode = 0644;
		type = FILE;
		filepos = pos;
		time = System.currentTimeMillis() / 1000;
	}

	/**
	 * Create a new TarEntry for a file of the given name.
	 *
	 * @param name filename
	 */
	public TarEntry(String name) {
		this(name, -1);
	}

	/**
	 * Returns the type of this file, one of FILE, LINK, SYM_LINK,
	 * CHAR_DEVICE, BLOCK_DEVICE, DIRECTORY or FIFO.
	 *
	 * @return file type
	 */
	public int getFileType() {
		return type;
	}

	/**
	 * Returns the mode of the file in UNIX permissions format.
	 *
	 * @return file mode
	 */
	public long getMode() {
		return mode;
	}

	/**
	 * Returns the name of the file.
	 *
	 * @return filename
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the size of the file in bytes.
	 *
	 * @return filesize
	 */
	public long getSize() {
		return size;
	}

	/**
	 * Returns the modification time of the file in seconds since January
	 * 1st 1970.
	 *
	 * @return time
	 */
	public long getTime() {
		return time;
	}

	/**
	 * Sets the type of the file, one of FILE, LINK, SYMLINK, CHAR_DEVICE,
	 * BLOCK_DEVICE, or DIRECTORY.
	 *
	 * @param type
	 */
	public void setFileType(int type) {
		this.type = type;
	}

	/**
	 * Sets the mode of the file in UNIX permissions format.
	 *
	 * @param mode
	 */
	public void setMode(long mode) {
		this.mode = mode;
	}

	/**
	 * Sets the size of the file in bytes.
	 *
	 * @param size
	 */
	public void setSize(long size) {
		this.size = size;
	}

	/**
	 * Sets the modification time of the file in seconds since January
	 * 1st 1970.
	 *
	 * @param time
	 */
	public void setTime(long time) {
		this.time = time;
	}
}
