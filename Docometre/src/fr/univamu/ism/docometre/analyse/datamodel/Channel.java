package fr.univamu.ism.docometre.analyse.datamodel;

import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
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
import fr.univamu.ism.docometre.analyse.views.SubjectsView;
import fr.univamu.ism.docometre.views.ExperimentsView;

public class Channel implements IFile {
	
	public static Channel fromBeginningChannel = new Channel(null, "From_Beginning");
	public static Channel toEndChannel = new Channel(null, "To_End");
	
	private String name;
	private IFolder subject;
	private Channel parentChannel;
	private boolean isMarker;
	private boolean isFeature;
	
	public Channel(IFolder subject, String name) {
		this.subject = subject;
		this.name = name;
		ResourceProperties.setObjectSessionProperty(this, this);
	}
	
	public Channel(IFolder subject, Channel parentChannel, String name, boolean isMarker, boolean isFeature) {
		this(subject, name);
		if(isMarker && isFeature) throw new IllegalArgumentException("Cannot be a marker and a feature at the same time !");
		this.parentChannel = parentChannel;
		this.isMarker = isMarker;
		this.isFeature = isFeature;
	}
	
	public IResource getSubject() {
		return subject;
	}
	
	public Channel getParentChannel() {
		return parentChannel;
	}
	
	public String getLabel() {
		if(!(isMarker || isFeature)) return null;
		return getName().split("_")[1];
	}

	public void setModified(boolean modified) {
		ResourceProperties.setSubjectModified(subject, modified);
		ExperimentsView.refresh(subject, null);
		SubjectsView.refresh(subject, null);
	}
	
	public boolean isModified() {
		return ResourceProperties.isSubjectModified(subject);
	}
	
	public String getFullName() {
		if(isMarker() || isFeature()) {
			return subject.getFullPath().toString().replaceAll("^/", "").replaceAll("/", ".") + "." + parentChannel.getName() + "." + getName();
		}
		if(isFrontEndCut() || isFromBeginningToEnd()) return getName();
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
		if(isMarker() || isFeature() || isFrontEndCut() || isFromBeginningToEnd()) return false;
		return MathEngineFactory.getMathEngine().isSignal(this);
	}
	
	public boolean isCategory() {
		if(isMarker() || isFeature() || isFrontEndCut() || isFromBeginningToEnd()) return false;
		return MathEngineFactory.getMathEngine().isCategory(this);
	}
	
	public boolean isEvent() {
		if(isMarker() || isFeature() || isFrontEndCut() || isFromBeginningToEnd()) return false;
		return MathEngineFactory.getMathEngine().isEvent(this);
	}
	
	public static boolean matchMarker(String name) {
		return Pattern.matches("^\\w+\\.\\w+\\.\\w+\\.MarkersGroup_\\w+", name);
	}
	
	public boolean isMarker() {
		return isMarker;
	}
	
	public static boolean matchFeature(String name) {
		return Pattern.matches("^\\w+\\.\\w+\\.\\w+\\.Feature_\\w+", name);
	}
	
	public boolean isFeature() {
		return isFeature;
	}
	
	public static boolean matchFrontEndCut(String name) {
		boolean match = Pattern.matches("^\\w+\\.\\w+\\.\\w+\\.FrontCut", name);
		match = match ||  Pattern.matches("^\\w+\\.\\w+\\.\\w+\\.EndCut", name);
		return match;
	}
	
	public boolean isFrontEndCut() {
		return matchFrontEndCut(getName());
	}
	
	public boolean isFromBeginningToEnd() {
		return this == fromBeginningChannel || this == toEndChannel;
	}
	
	@Override
	public void accept(IResourceProxyVisitor visitor, int memberFlags) throws CoreException {
		
		
	}

	@Override
	public void accept(IResourceProxyVisitor visitor, int depth, int memberFlags) throws CoreException {
		
		
	}

	@Override
	public void accept(IResourceVisitor visitor) throws CoreException {
		
		
	}

	@Override
	public void accept(IResourceVisitor visitor, int depth, boolean includePhantoms) throws CoreException {
		
		
	}

	@Override
	public void accept(IResourceVisitor visitor, int depth, int memberFlags) throws CoreException {
		
		
	}

	@Override
	public void clearHistory(IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void copy(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void copy(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void copy(IProjectDescription description, boolean force, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void copy(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public IMarker createMarker(String type) throws CoreException {
		
		return null;
	}

	@Override
	public IResourceProxy createProxy() {
		
		return null;
	}

	@Override
	public void delete(boolean force, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void delete(int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void deleteMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
		
		
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public IMarker findMarker(long id) throws CoreException {
		
		return null;
	}

	@Override
	public IMarker[] findMarkers(String type, boolean includeSubtypes, int depth) throws CoreException {
		
		return null;
	}

	@Override
	public int findMaxProblemSeverity(String type, boolean includeSubtypes, int depth) throws CoreException {
		
		return 0;
	}

	@Override
	public String getFileExtension() {
		
		return null;
	}

	@Override
	public long getLocalTimeStamp() {
		
		return 0;
	}

	@Override
	public IPath getLocation() {
		
		return null;
	}

	@Override
	public URI getLocationURI() {
		
		return null;
	}

	@Override
	public IMarker getMarker(long id) {
		
		return null;
	}

	@Override
	public long getModificationStamp() {
		
		return 0;
	}

	@Override
	public IPathVariableManager getPathVariableManager() {
		
		return null;
	}

	@Override
	public IContainer getParent() {
		return subject;
	}

	@Override
	public Map<QualifiedName, String> getPersistentProperties() throws CoreException {
		
		return null;
	}

	@Override
	public String getPersistentProperty(QualifiedName key) throws CoreException {
		if(ResourceProperties.TYPE_QN.equals(key)) return ResourceType.CHANNEL.toString();
		return null;
	}

	@Override
	public IProject getProject() {
		
		return null;
	}

	@Override
	public IPath getProjectRelativePath() {
		
		return null;
	}

	@Override
	public IPath getRawLocation() {
		
		return null;
	}

	@Override
	public URI getRawLocationURI() {
		
		return null;
	}

	@Override
	public ResourceAttributes getResourceAttributes() {
		
		return null;
	}

	@Override
	public Map<QualifiedName, Object> getSessionProperties() throws CoreException {
		
		return null;
	}

	@Override
	public Object getSessionProperty(QualifiedName key) throws CoreException {
		
		return null;
	}

	@Override
	public int getType() {
		return FILE;
	}

	@Override
	public IWorkspace getWorkspace() {
		
		return null;
	}

	@Override
	public boolean isAccessible() {
		
		return false;
	}

	@Override
	public boolean isDerived() {
		
		return false;
	}

	@Override
	public boolean isDerived(int options) {
		
		return false;
	}

	@Override
	public boolean isHidden() {
		
		return false;
	}

	@Override
	public boolean isHidden(int options) {
		
		return false;
	}

	@Override
	public boolean isLinked() {
		
		return false;
	}

	@Override
	public boolean isVirtual() {
		
		return false;
	}

	@Override
	public boolean isLinked(int options) {
		
		return false;
	}

	@Override
	public boolean isLocal(int depth) {
		
		return false;
	}

	@Override
	public boolean isPhantom() {
		
		return false;
	}

	@Override
	public boolean isSynchronized(int depth) {
		
		return false;
	}

	@Override
	public boolean isTeamPrivateMember() {
		
		return false;
	}

	@Override
	public boolean isTeamPrivateMember(int options) {
		
		return false;
	}

	@Override
	public void move(IPath destination, boolean force, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void move(IPath destination, int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void move(IProjectDescription description, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		
		
	}

	@Override
	public void move(IProjectDescription description, int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void refreshLocal(int depth, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void revertModificationStamp(long value) throws CoreException {
		
		
	}

	@Override
	public void setDerived(boolean isDerived) throws CoreException {
		
		
	}

	@Override
	public void setDerived(boolean isDerived, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void setHidden(boolean isHidden) throws CoreException {
		
		
	}

	@Override
	public void setLocal(boolean flag, int depth, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public long setLocalTimeStamp(long value) throws CoreException {
		
		return 0;
	}

	@Override
	public void setPersistentProperty(QualifiedName key, String value) throws CoreException {
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		
		
	}

	@Override
	public void setResourceAttributes(ResourceAttributes attributes) throws CoreException {
		
		
	}

	@Override
	public void setSessionProperty(QualifiedName key, Object value) throws CoreException {
		
		
	}

	@Override
	public void setTeamPrivateMember(boolean isTeamPrivate) throws CoreException {
		
		
	}

	@Override
	public void touch(IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		
		return null;
	}

	@Override
	public boolean contains(ISchedulingRule rule) {
		
		return false;
	}

	@Override
	public boolean isConflicting(ISchedulingRule rule) {
		
		return false;
	}

	@Override
	public void appendContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		
		
	}

	@Override
	public void appendContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void create(InputStream source, boolean force, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void create(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void createLink(IPath localLocation, int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void createLink(URI location, int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void delete(boolean force, boolean keepHistory, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public String getCharset() throws CoreException {
		
		return null;
	}

	@Override
	public String getCharset(boolean checkImplicit) throws CoreException {
		
		return null;
	}

	@Override
	public String getCharsetFor(Reader reader) throws CoreException {
		
		return null;
	}

	@Override
	public IContentDescription getContentDescription() throws CoreException {
		
		return null;
	}

	@Override
	public InputStream getContents() throws CoreException {
		
		return null;
	}

	@Override
	public InputStream getContents(boolean force) throws CoreException {
		
		return null;
	}

	@Override
	public int getEncoding() throws CoreException {
		
		return 0;
	}

	@Override
	public IPath getFullPath() {
		Path path = new Path(getFullName().replaceAll("\\.", "/"));
		return path;
	}

	@Override
	public IFileState[] getHistory(IProgressMonitor monitor) throws CoreException {
		
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isReadOnly() {
		
		return false;
	}

	@Override
	public void move(IPath destination, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		
		
	}

	@Override
	public void setCharset(String newCharset) throws CoreException {
		
		
	}

	@Override
	public void setCharset(String newCharset, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void setContents(InputStream source, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		
		
	}

	@Override
	public void setContents(IFileState source, boolean force, boolean keepHistory, IProgressMonitor monitor)
			throws CoreException {
		
		
	}

	@Override
	public void setContents(InputStream source, int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}

	@Override
	public void setContents(IFileState source, int updateFlags, IProgressMonitor monitor) throws CoreException {
		
		
	}
	
	@Override
	public boolean equals(Object object) {
		if(!(object instanceof Channel)) return false;
		Channel channel = (Channel)object;
		return getFullName().equals(channel.getFullName());
	}


}
