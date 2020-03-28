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
package matlabcontrol.extensions;

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

import java.io.Serializable;

import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxy.MatlabThreadCallable;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy.MatlabThreadProxy;

/** 
 * Converts between MATLAB and Java types. Currently only supports numeric arrays.
 * <br><br>
 * This class is unconditionally thread-safe.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabTypeConverter
{
    private final MatlabProxy _proxy;
    
    /**
     * Constructs the converter.
     * 
     * @param proxy
     */
    public MatlabTypeConverter(MatlabProxy proxy)
    {
        _proxy = proxy;
    }
    
    /**
     * Retrieves the MATLAB numeric array with the variable name {@code arrayName}.
     * 
     * @param arrayName
     * @return the retrieved numeric array
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    public MatlabNumericArray getNumericArray(String arrayName) throws MatlabInvocationException
    {
        ArrayInfo info = _proxy.invokeAndWait(new GetArrayCallable(arrayName));
        
        return new MatlabNumericArray(info.real, info.imaginary, info.lengths);
    }
    
    private static class GetArrayCallable implements MatlabThreadCallable<ArrayInfo>, Serializable
    {
        private final String _arrayName;
        
        public GetArrayCallable(String arrayName)
        {
            _arrayName = arrayName;
        }

        @Override
        public ArrayInfo call(MatlabThreadProxy proxy) throws MatlabInvocationException
        {
            //Retrieve real values
            Object realObject = proxy.returningEval("real(" + _arrayName + ");", 1)[0];
            double[] realValues = (double[]) realObject;
            
            //Retrieve imaginary values if present
            boolean isReal = ((boolean[]) proxy.returningEval("isreal(" + _arrayName + ");", 1)[0])[0];
            double[] imaginaryValues = null;
            if(!isReal)
            {
                Object imaginaryObject = proxy.returningEval("imag(" + _arrayName + ");", 1)[0];
                imaginaryValues = (double[]) imaginaryObject;
            }

            //Retrieve lengths of array
            double[] size = (double[]) proxy.returningEval("size(" + _arrayName + ");", 1)[0];
            int[] lengths = new int[size.length];
            for(int i = 0; i < size.length; i++)
            {
                lengths[i] = (int) size[i];
            }

            return new ArrayInfo(realValues, imaginaryValues, lengths);
        }
    }
    
    private static class ArrayInfo implements Serializable
    {
        private final double[] real, imaginary;
        private final int[] lengths;
        
        public ArrayInfo(double[] real, double[] imaginary, int[] lengths)
        {
            this.real = real;
            this.imaginary = imaginary;
            this.lengths = lengths;
        }
    }
    
    /**
     * Stores the {@code array} in MATLAB with the variable name {@code arrayName}.
     * 
     * @param arrayName the variable name
     * @param array
     * @throws matlabcontrol.MatlabInvocationException if thrown by the proxy
     */
    public void setNumericArray(String arrayName, MatlabNumericArray array) throws MatlabInvocationException
    {
        _proxy.invokeAndWait(new SetArrayCallable(arrayName, array));
    }
    
    private static class SetArrayCallable implements MatlabThreadCallable<Object>, Serializable
    {
        private final String _arrayName;
        private final double[] _realArray, _imaginaryArray;
        private final int[] _lengths;
        
        private SetArrayCallable(String arrayName, MatlabNumericArray array)
        {
            _arrayName = arrayName;
            _realArray = array.getRealLinearArray();
            _imaginaryArray = array.getImaginaryLinearArray();
            _lengths = array.getLengths();
        }
        
        @Override
        public Object call(MatlabThreadProxy proxy) throws MatlabInvocationException
        {
            //Store real array in the MATLAB environment
            String realArray = (String) proxy.returningEval("genvarname('" + _arrayName + "_real', who);", 1)[0];
            proxy.setVariable(realArray, _realArray);
            
            //If present, store the imaginary array in the MATLAB environment
            String imagArray = null;
            if(_imaginaryArray != null)
            {
                imagArray = (String) proxy.returningEval("genvarname('" + _arrayName + "_imag', who);", 1)[0];
                proxy.setVariable(imagArray, _imaginaryArray);
            }

            //Build a statement to eval
            // - If imaginary array exists, combine the real and imaginary arrays
            // - Set the proper dimension length metadata
            // - Store as arrayName
            String evalStatement = _arrayName + " = reshape(" + realArray;
            if(_imaginaryArray != null)
            {
                evalStatement += " + " + imagArray + " * i";
            }
            for(int length : _lengths)
            {
                evalStatement += ", " + length;
            }
            evalStatement += ");";
            proxy.eval(evalStatement);
            
            //Clear variables holding separate real and imaginary arrays
            proxy.eval("clear " + realArray + ";");
            proxy.eval("clear " + imagArray + ";");
            
            return null;
        }
    }
    
    /**
     * Returns a brief description of this converter. The exact details of this representation are unspecified and are
     * subject to change.
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() + " proxy=" + _proxy + "]";
    }
}
