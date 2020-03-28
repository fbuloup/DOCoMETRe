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
 * Represents a failure to invoke a method on the MATLAB session.
 * 
 * @since 3.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabInvocationException extends Exception
{    
    private static final long serialVersionUID = 0xB400L;
    
    static enum Reason
    {
        INTERRRUPTED("Method could not be completed because the MATLAB thread was interrupted before MATLAB returned"),
        PROXY_NOT_CONNECTED("The proxy is not connected to MATLAB"),
        UNMARSHAL("Object attempting to be received cannot be transferred between Java Virtual Machines"),
        MARSHAL("Object attempting to be sent cannot be transferred between Java Virtual Machines"),
        INTERNAL_EXCEPTION("Method did not return properly because of an internal MATLAB exception"),
        NARGOUT_MISMATCH("Number of arguments returned did not match excepted"),
        EVENT_DISPATCH_THREAD("Issue pumping Event Dispatch Thread"),
        RUNTIME_EXCEPTION("RuntimeException occurred in MatlabThreadCallable, see cause for more information"),
        UNKNOWN("Method could not be invoked for an unknown reason, see cause for more information");
        
        private final String _message;
        
        private Reason(String msg)
        {
            _message = msg;
        }
        
        MatlabInvocationException asException()
        {
            return new MatlabInvocationException(_message);
        }
        
        MatlabInvocationException asException(Throwable cause)
        {
            return new MatlabInvocationException(_message, cause);
        }
        
        MatlabInvocationException asException(String additionalInfo)
        {
            return new MatlabInvocationException(_message + ": " + additionalInfo);
        }
        
        MatlabInvocationException asException(String additionalInfo, Throwable cause)
        {
            return new MatlabInvocationException(_message + ": " + additionalInfo, cause);
        }
    }
    
    private MatlabInvocationException(String msg)
    {
        super(msg);
    }
    
    private MatlabInvocationException(String msg, Throwable cause)
    {
        super(msg, cause);
    }
}
