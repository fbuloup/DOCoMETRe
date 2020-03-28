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

/**
 * Allows for calling MATLAB from <b>inside</b> of MATLAB.
 * 
 * @since 3.1.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
class LocalMatlabProxy extends MatlabProxy
{   
    /**
     * If connected to MATLAB.
     * 
     * This notion of connection exists to make it consistent with {@link RemoteMatlabProxy}, but is not actually
     * necessary. Unless a user calls {@link #disconnect()} this proxy cannot become disconnected.
     */
    private volatile boolean _isConnected = true;

    LocalMatlabProxy(Identifier id)
    {
        super(id, true);
    }
    
    @Override
    public boolean isRunningInsideMatlab()
    {
        return true;
    }
    
    @Override
    public boolean isConnected()
    {
        return _isConnected;
    }
    
    @Override
    public boolean disconnect()
    {
        _isConnected = false;
        
        //Notify listeners
        notifyDisconnectionListeners();
        
        return true;
    }
    
    // Methods which interact with MATLAB
        
    @Override
    public void exit() throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            JMIWrapper.exit();
        }
        else
        {
            throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
        }
    }

    @Override
    public void eval(String command) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            JMIWrapper.eval(command);
        }
        else
        {
            throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
        }
    }
    
    @Override
    public Object[] returningEval(String command, int nargout) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            return JMIWrapper.returningEval(command, nargout);
        }
        else
        {
            throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
        }
    }
    
    @Override
    public void feval(String functionName, Object... args) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            JMIWrapper.feval(functionName, args);
        }
        else
        {
            throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
        }
    }

    @Override
    public Object[] returningFeval(String functionName, int nargout, Object... args) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            return JMIWrapper.returningFeval(functionName, nargout, args);
        }
        else
        {
            throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
        }
    }

    @Override
    public void setVariable(String variableName, Object value) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            JMIWrapper.setVariable(variableName, value);
        }
        else
        {
            throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
        }
    }
    
    @Override
    public Object getVariable(String variableName) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            return JMIWrapper.getVariable(variableName);
        }
        else
        {
            throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
        }
    }
    
    @Override
    public <T> T invokeAndWait(MatlabThreadCallable<T> callable) throws MatlabInvocationException
    {
        if(this.isConnected())
        {
            return JMIWrapper.invokeAndWait(callable);
        }
        else
        {
            throw MatlabInvocationException.Reason.PROXY_NOT_CONNECTED.asException();
        }
    }
}
