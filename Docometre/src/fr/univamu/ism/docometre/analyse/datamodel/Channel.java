package fr.univamu.ism.docometre.analyse.datamodel;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourceAttributes;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.jobs.ISchedulingRule;

import fr.univamu.ism.docometre.ResourceProperties;
import fr.univamu.ism.docometre.ResourceType;
import fr.univamu.ism.docometre.analyse.MathEngineFactory;

public class Channel implements IFile {
	
	private String name;
	private IFolder subject;
	
	public Channel(IFolder subject, String name) {
		this.subject = subject;
		this.name = name;
		ResourceProperties.setObjectSessionProperty(this, this);
	}

	public String getFullName() {
		return subject.getFullPath().toString().replaceAll("^/", "").replaceAll("/", ".") + "." + getName();
	}
	
	@Override
	public String toString() {
		if(isCategory()) {
			return getName() + " [" + MathEngineFactory.getMathEngine().getCriteriaForCategory(this) + "]";
		}
		return getName();
	}
	
	public boolean isSignal() {
		return MathEngineFactory.getMathEngine().isSignal(this);
	}
	
	public boolean isCategory() {
		return MathEngineFactory.getMathEngine().isCategory(this);
	}
	
	public boolean isEvent() {
		return MathEngineFactory.getMathEngine().isEvent(this);
	}

	@Override
	public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accept(IResourceProxyVisitor visitor, int depth, int memberFlags) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accept(IResourceVisitor visitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clearHistory(IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IMarker createMarker(String type) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IResourceProxy createProxy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public IMarker findMarker(long id) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getFileExtension() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getLocalTimeStamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IPath getLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getLocationURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IMarker getMarker(long id) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public long getModificationStamp() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IPathVariableManager getPathVariableManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContainer getParent() {
		return subject;
	}

	@Override
	public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getPersistentProperty(QualifiedName key) throws CoreException {
		if(ResourceProperties.TYPE_QN.equals(key)) return ResourceType.CHANNEL.toString();
		return null;
	}

	@Override
	public IProject getProject() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPath getProjectRelativePath() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IPath getRawLocation() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public URI getRawLocationURI() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ResourceAttributes getResourceAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getSessionProperty(QualifiedName key) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getType() {
		return FILE;
	}

	@Override
	public IWorkspace getWorkspace() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isAccessible() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDerived() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isDerived(int options) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isHidden() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isHidden(int options) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLinked() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isVirtual() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLinked(int options) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isLocal(int depth) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isPhantom() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSynchronized(int depth) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTeamPrivateMember() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isTeamPrivateMember(int options) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void revertModificationStamp(long value) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDerived(boolean isDerived) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setHidden(boolean isHidden) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long setLocalTimeStamp(long value) throws CoreException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void touch(IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void appendContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void create(InputStream source, boolean force, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void create(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void createLink(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getCharset() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCharset(boolean checkImplicit) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getCharsetFor(Reader reader) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IContentDescription getContentDescription() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getContents() throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream getContents(boolean force) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getEncoding() throws CoreException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IPath getFullPath() {
		Path path = new Path(getFullName().replaceAll("\\.", "/"));
		return path;
	}

	@Override
	public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isReadOnly() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCharset(String newCharset) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContents(IFileState source, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor) throws CoreException {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public boolean equals(Object object) {
		if(!(object instanceof Channel)) return false;
		Channel channel = (Channel)object;
		return getFullName().equals(channel.getFullName());
	}


}
