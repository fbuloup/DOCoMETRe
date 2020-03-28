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
package matlabcontrol;

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

import matlabcontrol.MatlabProxy.Identifier;

/**
 * Creates instances of {@link MatlabProxy}. Any number of proxies may be created with a factory.
 * <br><br>
 * How the proxies will connect to a session of MATLAB depends on whether the factory is running inside or outside
 * MATLAB:
 * <br><br>
 * <i>Running inside MATLAB</i><br>
 * The proxy will connect to the session of MATLAB this factory is running in.
 * <br><br>
 * <i>Running outside MATLAB</i><br>
 * By default a new session of MATLAB will be started and connected to, but the factory may be configured via the
 * options provided to this factory to connect to a previously controlled session.
 * <br><br>
 * This class is unconditionally thread-safe. Any number of proxies may be created simultaneously.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabProxyFactory implements ProxyFactory
{
    private final ProxyFactory _delegateFactory;
    
    /**
     * Constructs the factory using default options.
     * 
     * @throws MatlabConnectionException 
     */
    public MatlabProxyFactory()
    {
        this(new MatlabProxyFactoryOptions.Builder().build());
    }
    
    /**
     * Constructs the factory with the specified {@code options}. Depending on the whether the factory is running inside
     * MATLAB or outside MATLAB will determine if a given option is used.
     * 
     * @param options
     */
    public MatlabProxyFactory(MatlabProxyFactoryOptions options)
    {           
        if(Configuration.isRunningInsideMatlab())
        {
            _delegateFactory = new LocalMatlabProxyFactory(options);
        }
        else
        {
            _delegateFactory = new RemoteMatlabProxyFactory(options);
        }
    }

    @Override
    public MatlabProxy getProxy() throws MatlabConnectionException
    {
        return _delegateFactory.getProxy();
    }

    @Override
    public Request requestProxy(RequestCallback callback) throws MatlabConnectionException
    {
        if(callback == null)
        {
            throw new NullPointerException("The request callback may not be null");
        }
        
        return _delegateFactory.requestProxy(callback);
    }
    
    /**
     * Provides the requested proxy.
     * 
     * @since 4.0.0
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static interface RequestCallback
    {
        /**
         * Called when the proxy has been created. Because requests have no timeout, there is no guarantee that this
         * method will ever be called.
         * 
         * @param proxy 
         */
        public void proxyCreated(MatlabProxy proxy);
    }
    
    /**
     * A request for a proxy. Because requests have no timeout, a {@code Request} has no concept of
     * failure.
     * <br><br>
     * Implementations of this class are unconditionally thread-safe.
     * <br><br>
     * <b>WARNING:</b> This interface is not intended to be implemented by users of matlabcontrol. Methods may be added
     * to this interface, and these additions will not be considered breaking binary compatibility.
     * 
     * @since 4.0.0
     * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
     */
    public static interface Request
    {
        /**
         * The identifier of the proxy associated with this request. If the proxy is created, then its identifier
         * accessible via {@link MatlabProxy#getIdentifier()} will return {@code true} when tested for equivalence with
         * the identifier returned by this method using {@link Identifier#equals(java.lang.Object)}.
         * 
         * @return proxy's identifier
         */
        public Identifier getProxyIdentifer();
        
        /**
         * Attempts to cancel the request. If the request has already been completed or cannot successfully be canceled
         * then {@code false} will be returned, otherwise {@code true} will be returned. If the request has already been
         * successfully canceled then this method will have no effect and {@code true} will be returned.
         * 
         * @return if successfully cancelled
         */
        public boolean cancel();
        
        /**
         * If the request has been successfully cancelled.
         * 
         * @return if successfully cancelled
         */
        public boolean isCancelled();
        
        /**
         * Returns {@code true} if the proxy has been created.
         * 
         * @return if the proxy has been created
         */
        public boolean isCompleted();
    }
}
