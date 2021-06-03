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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.operation.ModalContext;
import org.eclipse.osgi.util.NLS;

import fr.univamu.ism.docometre.Activator;
import fr.univamu.ism.docometre.DocometreMessages;
import fr.univamu.ism.docometre.ResourceType;

/**
 *	Operation for exporting a resource and its children to a new .zip or
 *  .tar.gz file.
 *
 *  @since 3.1
 */
public class ArchiveFileExportOperation implements IRunnableWithProgress {
	private IFileExporter exporter;
	private String destinationFilename;
	private IProgressMonitor monitor;
	private List<? extends IResource> resourcesToExport;
	private IResource resource;
	private List<IStatus> errorTable = new ArrayList<>(1); // IStatus
	private boolean useCompression = true;
	private boolean resolveLinks = false;
	private boolean useTarFormat = false;
	private boolean createLeadupStructure = true;
	private String toRootDirectory;
	private String fromRootDirectory;
	private boolean includeData = true;

	/**
	 *	Create an instance of this class.  Use this constructor if you wish to
	 *	export specific resources without a common parent resource
	 *
	 *	@param resources java.util.Vector
	 *	@param filename java.lang.String
	 */
	public ArchiveFileExportOperation(List<? extends IResource> resources, String filename) {
		super();

		// Eliminate redundancies in list of resources being exported
		Iterator<? extends IResource> elementsEnum = resources.iterator();
		while (elementsEnum.hasNext()) {
			IResource currentResource = elementsEnum.next();
			if (isDescendent(resources, currentResource)) {
				elementsEnum.remove(); //Removes currentResource;
			}
		}

		resourcesToExport = resources;
		destinationFilename = filename;
	}

	/**
	 *  Create an instance of this class.  Use this constructor if you wish
	 *  to recursively export a single resource.
	 *
	 *  @param res org.eclipse.core.resources.IResource;
	 *  @param filename java.lang.String
	 */
	public ArchiveFileExportOperation(IResource res, String filename, boolean includeData) {
		super();
		resource = res;
		destinationFilename = filename;
		this.includeData = includeData;
	}

	/**
	 *  Create an instance of this class.  Use this constructor if you wish to
	 *  export specific resources with a common parent resource (affects container
	 *  directory creation)
	 *
	 *  @param res org.eclipse.core.resources.IResource
	 *  @param resources java.util.Vector
	 *  @param filename java.lang.String
	 */
	public ArchiveFileExportOperation(IResource res, List<IResource> resources, String filename, boolean includeData) {
		this(res, filename, includeData);
		resourcesToExport = resources;
	}

	/**
	 * Add a new entry to the error table with the passed information
	 */
	protected void addError(String message, Throwable e) {
		errorTable.add(new Status(IStatus.ERROR,
				Activator.PLUGIN_ID, 0, message, e));
	}

	/**
	 *  Answer the total number of file resources that exist at or below self
	 *  in the resources hierarchy.
	 *
	 *  @return int
	 *  @param checkResource org.eclipse.core.resources.IResource
	 */
	protected int countChildrenOf(IResource checkResource) throws CoreException {
		if (checkResource.getType() == IResource.FILE) {
			if(!includeData && ResourceType.isDataFile(checkResource)) return 0;
			else return 1;
		}

		int count = 0;
		if (checkResource.isAccessible()) {
			for (IResource child : ((IContainer) checkResource).members()) {
				count += countChildrenOf(child);
			}
		}

		return count;
	}

	/**
	 *	Answer a boolean indicating the number of file resources that were
	 *	specified for export
	 *
	 *	@return int
	 */
	protected int countSelectedResources() throws CoreException {
		int result = 0;
		Iterator<? extends IResource> resources = resourcesToExport.iterator();
		while (resources.hasNext()) {
			result += countChildrenOf(resources.next());
		}

		return result;
	}

	/**
	 *  Export the passed resource to the destination .zip. Export with
	 * no path leadup
	 *
	 *  @param exportResource org.eclipse.core.resources.IResource
	 */
	protected void exportResource(IResource exportResource)
			throws InterruptedException {
		exportResource(exportResource, 1);
	}

	/**
	 * Creates and returns the string that should be used as the name of the entry in the archive.
	 * @param exportResource the resource to export
	 * @param leadupDepth the number of resource levels to be included in the path including the resourse itself.
	 */
	private String createDestinationName(int leadupDepth, IResource exportResource) {
		IPath fullPath = exportResource.getFullPath();
		if (createLeadupStructure) {
			return fullPath.makeRelative().toString().replaceFirst(fromRootDirectory, toRootDirectory);
		}
		return fullPath.removeFirstSegments(fullPath.segmentCount() - leadupDepth).makeRelative().toString().replaceFirst(fromRootDirectory, toRootDirectory);
	}

	/**
	 *  Export the passed resource to the destination .zip
	 *
	 *  @param exportResource org.eclipse.core.resources.IResource
	 *  @param leadupDepth the number of resource levels to be included in
	 *                     the path including the resourse itself.
	 */
	protected void exportResource(IResource exportResource, int leadupDepth)
			throws InterruptedException {
		if (!exportResource.isAccessible() || (!resolveLinks && exportResource.isLinked())) {
			return;
		}
		
		if(!includeData) if(ResourceType.isDataFile(exportResource)) return;
		System.out.println("ArchiveFileExportOperation.exportResource : " + exportResource.getLocation().toOSString());

		if (exportResource.getType() == IResource.FILE) {
			String destinationName = createDestinationName(leadupDepth, exportResource);
			monitor.subTask(destinationName);

			try {
				exporter.write((IFile) exportResource, destinationName);
			} catch (IOException | CoreException e) {
				addError(NLS.bind(DocometreMessages.DataTransfer_errorExporting, exportResource.getFullPath().makeRelative(), e.getMessage()), e);
			}

			monitor.worked(1);
			ModalContext.checkCanceled(monitor);
		} else {
			IResource[] children = null;

			try {
				children = ((IContainer) exportResource).members();
			} catch (CoreException e) {
				// this should never happen because an #isAccessible check is done before #members is invoked
				addError(NLS.bind(DocometreMessages.DataTransfer_errorExporting, exportResource.getFullPath()), e);
			}

			if (children.length == 0) { // create an entry for empty containers, see bug 278402
				String destinationName = createDestinationName(leadupDepth, exportResource);
				try {
					exporter.write((IContainer) exportResource, destinationName + IPath.SEPARATOR);
				} catch (IOException e) {
					addError(NLS.bind(DocometreMessages.DataTransfer_errorExporting, exportResource.getFullPath().makeRelative(), e.getMessage()), e);
				}
			}

			for (IResource child : children) {
				exportResource(child, leadupDepth + 1);
			}

		}
	}

	/**
	 *	Export the resources contained in the previously-defined
	 *	resourcesToExport collection
	 */
	protected void exportSpecifiedResources() throws InterruptedException {
		Iterator<? extends IResource> resources = resourcesToExport.iterator();

		while (resources.hasNext()) {
			IResource currentResource = resources.next();
			exportResource(currentResource);
		}
	}

	/**
	 * Returns the status of the operation.
	 * If there were any errors, the result is a status object containing
	 * individual status objects for each error.
	 * If there were no errors, the result is a status object with error code <code>OK</code>.
	 *
	 * @return the status
	 */
	public IStatus getStatus() {
		IStatus[] errors = new IStatus[errorTable.size()];
		errorTable.toArray(errors);
		return new MultiStatus(
				Activator.PLUGIN_ID,
				IStatus.OK,
				errors,
				DocometreMessages.FileSystemExportOperation_problemsExporting,
				null);
	}

	/**
	 *	Initialize this operation
	 *
	 *	@exception java.io.IOException
	 */
	protected void initialize() throws IOException {
		if(useTarFormat) {
			exporter = new TarFileExporter(destinationFilename, useCompression, resolveLinks);
		} else {
			exporter = new ZipFileExporter(destinationFilename, useCompression, resolveLinks);
		}
		IPath path = new Path(destinationFilename);
		toRootDirectory =  path.removeFileExtension().lastSegment();
		fromRootDirectory = resource.getName();
	}

	/**
	 *  Answer a boolean indicating whether the passed child is a descendent
	 *  of one or more members of the passed resources collection
	 *
	 *  @return boolean
	 *  @param resources java.util.Vector
	 *  @param child org.eclipse.core.resources.IResource
	 */
	protected boolean isDescendent(List<? extends IResource> resources, IResource child) {
		if (child.getType() == IResource.PROJECT) {
			return false;
		}

		IResource parent = child.getParent();
		if (resources.contains(parent)) {
			return true;
		}

		return isDescendent(resources, parent);
	}
	
	public int getTotalWork() {
		int totalWork = IProgressMonitor.UNKNOWN;
		try {
			if (resourcesToExport == null) {
				totalWork = countChildrenOf(resource);
			} else {
				totalWork = countSelectedResources();
			}
		} catch (CoreException e) {
			// Should not happen
		}
		return totalWork;
	}

	/**
	 *	Export the resources that were previously specified for export
	 *	(or if a single resource was specified then export it recursively)
	 */
	@Override
	public void run(IProgressMonitor progressMonitor) throws InvocationTargetException, InterruptedException {
		
		this.monitor = progressMonitor;

		try {
			initialize();
		} catch (IOException e) {
			throw new InvocationTargetException(e, NLS.bind(DocometreMessages.ZipExport_cannotOpen, e.getMessage()));
		}

		try {
			if (resourcesToExport == null) {
				exportResource(resource);
			} else {
				// ie.- a list of specific resources to export was specified
				exportSpecifiedResources();
			}

			try {
				exporter.finished();
			} catch (IOException e) {
				throw new InvocationTargetException(
						e,
						NLS.bind(DocometreMessages.ZipExport_cannotClose, e.getMessage()));
			}
		} finally {
			monitor.worked(1);
		}
	}

	/**
	 *	Set this boolean indicating whether each exported resource's path should
	 *	include containment hierarchies as dictated by its parents
	 *
	 *	@param value boolean
	 */
	public void setCreateLeadupStructure(boolean value) {
		createLeadupStructure = value;
	}

	/**
	 *	Set this boolean indicating whether exported resources should
	 *	be compressed (as opposed to simply being stored)
	 *
	 *	@param value boolean
	 */
	public void setUseCompression(boolean value) {
		useCompression = value;
	}

	/**
	 * Set this boolean indicating whether the file should be output
	 * in tar.gz format rather than .zip format.
	 *
	 * @param value boolean
	 */
	public void setUseTarFormat(boolean value) {
		useTarFormat = value;
	}

	/**
	 * Set this boolean indicating whether linked resources should be resolved
	 * and exported (as opposed to simply ignored)
	 *
	 * @param value
	 *            boolean
	 */
	public void setIncludeLinkedResources(boolean value) {
		resolveLinks = value;
	}
}
