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
package matlabcontrol.internal;

/*
 * Copyright (c) 2013, Joshua Kaplan
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *  - Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 *    disclaimer.
 *  - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *    following disclaimer in the documentation and/or other materials provided with the distribution.
 *  - Neither the name of matlabcontrol nor the names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.File;
import java.net.MalformedURLException;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;

/**
 * <strong>Internal Use Only</strong>
 * <br><br>
 * This class must be public so that it can be created via reflection by {@link RemoteClassLoader}. If it were package
 * private it would result in an {@link IllegalAccessError} because only classes in the same package as a package
 * private class may construct it (even via reflection). It has been placed in the {@code matlabcontrol.internal}
 * package to make it clear it is not intended for use by users of matlabcontrol.
 * <br><br>
 * A custom service provider for the RMI class loader. Allows for loading classes sent from the external JVM and
 * providing annotations so that the external JVM may load classes defined only in the MATLAB JVM. Loading classes from
 * the external JVM could be accomplished by setting {@code java.rmi.server.codebase} property in the external JVM but
 * that could interfere with other uses of RMI in the application. There is no way to always sending the correct
 * annotations without this custom rmi class loader spi. While the {@code java.rmi.server.codebase} property could be
 * set in the MATLAB JVM, the property is checked only at load time. This would mean that class definitions added
 * dynamically with {@code javaaddpath} could not be sent.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 * 
 */
public class MatlabRMIClassLoaderSpi extends RMIClassLoaderSpi
{
    /**
     * Loading of classes is delegated to the default {@link RMIClassLoaderSpi}.
     */
    private final RMIClassLoaderSpi _delegateLoaderSpi = RMIClassLoader.getDefaultProviderInstance();
    
    /**
     * The codebase of the external virtual machine which has a proxy that can interact with this session of MATLAB.
     * This is done instead of setting the {@code java.rmi.server.codebase} property so that matlabcontrol does not
     * interfere with any other uses of RMI in the application.
     */
    private static volatile String _remoteCodebase = null;
    
    /**
     * Sets the codebase of the currently connected external JVM. This should be called only once per connection to an
     * external JVM and should occur before users of the API can send objects over RMI.
     * 
     * @param remoteCodebase 
     */
    public static void setCodebase(String remoteCodebase)
    {
        _remoteCodebase = remoteCodebase;
    }
    
    @Override
    public Class<?> loadClass(String codebase, String name, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException
    {   
        return _delegateLoaderSpi.loadClass(_remoteCodebase, name, defaultLoader);
    }

    @Override
    public Class<?> loadProxyClass(String codebase, String[] interfaces, ClassLoader defaultLoader) throws MalformedURLException, ClassNotFoundException
    {
        return _delegateLoaderSpi.loadProxyClass(_remoteCodebase, interfaces, defaultLoader);
    }

    @Override
    public ClassLoader getClassLoader(String codebase) throws MalformedURLException
    {
        return _delegateLoaderSpi.getClassLoader(_remoteCodebase);
    }

    /**
     * {@inheritDoc}
     * <br><br>
     * The returned annotation becomes the {@code codebase} argument in
     * {@link #loadClass(java.lang.String, java.lang.String, java.lang.ClassLoader)} when the {@code RMIClassLoaderSpi}
     * in the receiving JVM attempts to load {@code clazz}. This allows for classes defined in MATLAB but not in the
     * receiving JVM to find and load the class definition.
     * 
     * @param clazz
     * @return 
     */
    @Override
    public String getClassAnnotation(Class<?> clazz)
    {
        if(clazz == null)
        {
            throw new NullPointerException("class may not be null");
        }
        
        String annotation = null;
        
        //If the class has a code source, meaning it is not part of the Java Runtime Environment
        if(clazz.getProtectionDomain().getCodeSource() != null)
        {
            //This convoluted way of determining the code source location is necessary due to a bug in early versions of
            //Java 6 on Windows (such as what is used by MATLAB R2007b) which puts a space in the code source's URL.
            //A space in the URL will cause the receiver of this annotation to treat it as a path separator, which would
            //be very problematic and likely cause invalid protocol exceptions.
            try
            {
                File file = new File(clazz.getProtectionDomain().getCodeSource().getLocation().getPath());
                annotation = file.toURI().toURL().toString();
            }
            catch(MalformedURLException e) { }
        }
        
        return annotation;
    }
}
