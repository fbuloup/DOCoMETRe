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

import java.lang.reflect.Array;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import matlabcontrol.MatlabProxy.MatlabThreadCallable;
import matlabcontrol.MatlabInvocationException;
import matlabcontrol.MatlabProxy;
import matlabcontrol.MatlabProxy.DisconnectionListener;
import matlabcontrol.MatlabProxy.Identifier;

/**
 * @deprecated Replaced by {@link matlabcontrol.LoggingMatlabProxy}.
 * 
 * Wraps around a {@link MatlabProxy} to provide a log of interactions. The data is not altered. This logger is useful
 * for determining the Java types and structure of data returned from MATLAB.
 * <br><br>
 * Entering a method, exiting a method, and throwing an exception are logged. Method parameters and return values are
 * logged. The contents of a returned array will be recursively explored and its contents logged. As is convention, all
 * of these interactions are logged at {@code Level.FINER}. If the logging system has not been otherwise configured,
 * then the {@code ConsoleHandler} which prints log messages to the console will not show these log messages as their
 * level is too low. To configure the {@code ConsoleHandler} to show these log messages, call
 * {@link #showInConsoleHandler()}.
 * <br><br>
 * This class is unconditionally thread-safe.
 * 
 * @since 4.0.0
 * 
 * @author <a href="mailto:nonother@gmail.com">Joshua Kaplan</a>
 */
public class MatlabProxyLogger
{
    private static final Logger LOGGER = Logger.getLogger(MatlabProxyLogger.class.getName());
    static
    {
        LOGGER.setLevel(Level.FINER);
    }
    private static final String CLASS_NAME = MatlabProxyLogger.class.getName();
    
    private final MatlabProxy _proxy;
    
    /**
     * Constructs the logger. If the provided {@code proxy} throws an exception it will be caught, logged, and
     * then rethrown.
     * 
     * @param proxy
     */
    public MatlabProxyLogger(MatlabProxy proxy)
    {
        _proxy = proxy;
    }
    
    /**
     * Configures the {@code ConsoleHandler} responsible for showing logging records to show the records that are
     * logged by this interactor. This is behavior is useful if you have not otherwise configured logging in your
     * application.
     */
    public static void showInConsoleHandler()
    {
        for(Handler handler : Logger.getLogger("").getHandlers())
        {
            if(handler instanceof ConsoleHandler)
            {
                handler.setLevel(Level.FINER);
            }
        }
    }
    
    private static abstract class Invocation
    {
        final String name;
        final Object[] args;
        
        public Invocation(String name, Object... args)
        {
            this.name = name;
            this.args = args;
        }
    }
    
    private static abstract class VoidThrowingInvocation extends Invocation
    {
        public VoidThrowingInvocation(String name, Object... args)
        {
            super(name, args);
        }
        
        public abstract void invoke() throws MatlabInvocationException;
    }
    
    private static abstract class VoidInvocation extends Invocation
    {
        public VoidInvocation(String name, Object... args)
        {
            super(name, args);
        }
        
        public abstract void invoke();
    }
    
    private static abstract class ReturnThrowingInvocation<T> extends Invocation
    {
        public ReturnThrowingInvocation(String name, Object... args)
        {
            super(name, args);
        }
        
        public abstract T invoke() throws MatlabInvocationException;
    }
    
    private static abstract class ReturnInvocation<T> extends Invocation
    {
        public ReturnInvocation(String name, Object... args)
        {
            super(name, args);
        }
        
        public abstract T invoke();
    }
    
    private static abstract class ReturnBooleanInvocation extends Invocation
    {
        public ReturnBooleanInvocation(String name, Object... args)
        {
            super(name, args);
        }
        
        public abstract boolean invoke();
    }

    private void invoke(VoidThrowingInvocation invocation) throws MatlabInvocationException
    {   
        LOGGER.entering(CLASS_NAME, invocation.name, invocation.args);
        
        try
        {
            invocation.invoke();
            LOGGER.exiting(CLASS_NAME, invocation.name);
        }
        catch(MatlabInvocationException e)
        {            
            LOGGER.throwing(CLASS_NAME, invocation.name, e);
            LOGGER.exiting(CLASS_NAME, invocation.name);
            
            throw e;
        }
    }
    
    private void invoke(VoidInvocation invocation)
    {   
        LOGGER.entering(CLASS_NAME, invocation.name, invocation.args);
        invocation.invoke();
        LOGGER.exiting(CLASS_NAME, invocation.name);
    }

    private <T> T invoke(ReturnThrowingInvocation<T> invocation) throws MatlabInvocationException
    {
        T data;
        
        LOGGER.entering(CLASS_NAME, invocation.name, invocation.args);
        try
        {
            data = invocation.invoke();
            LOGGER.exiting(CLASS_NAME, invocation.name, formatResult(data));
        }
        catch(MatlabInvocationException e)
        {
            LOGGER.throwing(CLASS_NAME, invocation.name, e);
            LOGGER.exiting(CLASS_NAME, invocation.name);
            
            throw e;
        }
        return data;
    }
    
    private <T> T invoke(ReturnInvocation<T> invocation)
    {
        T data;
        
        LOGGER.entering(CLASS_NAME, invocation.name, invocation.args);
        data = invocation.invoke();
        LOGGER.exiting(CLASS_NAME, invocation.name, formatResult(data));
        
        return data;
    }
    
    private boolean invoke(ReturnBooleanInvocation invocation)
    {
        boolean data;
        
        LOGGER.entering(CLASS_NAME, invocation.name, invocation.args);
        data = invocation.invoke();
        LOGGER.exiting(CLASS_NAME, invocation.name, "boolean: " + data);
        
        return data;
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @param command
     * @throws MatlabInvocationException
     */
    public void eval(final String command) throws MatlabInvocationException
    {           
        this.invoke(new VoidThrowingInvocation("eval(String)", command)
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _proxy.eval(command);
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @param command
     * @param nargout
     * @return
     * @throws MatlabInvocationExceptio
     */
    public Object[] returningEval(final String command, final int nargout) throws MatlabInvocationException
    {
        return this.invoke(new ReturnThrowingInvocation<Object[]>("returningEval(String, int)", command, nargout)
        {
            @Override
            public Object[] invoke() throws MatlabInvocationException
            {
                return _proxy.returningEval(command, nargout);
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @param functionName
     * @param args
     * @throws MatlabInvocationException
     */
    public void feval(final String functionName, final Object... args) throws MatlabInvocationException
    {
        this.invoke(new VoidThrowingInvocation("feval(String, Object...)", functionName, args)
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _proxy.feval(functionName, args);
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @param functionName
     * @param nargout
     * @param args
     * @return
     * @throws MatlabInvocationException
     */
    public Object[] returningFeval(final String functionName, final int nargout, final Object... args)
            throws MatlabInvocationException
    {
        return this.invoke(new ReturnThrowingInvocation<Object[]>("returningFeval(String, int, Object...)",
                functionName, nargout, args)
        {
            @Override
            public Object[] invoke() throws MatlabInvocationException
            {
                return _proxy.returningFeval(functionName, nargout, args);
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @param variableName
     * @param value
     * @throws MatlabInvocationException
     */
    public void setVariable(final String variableName, final Object value) throws MatlabInvocationException
    {   
        this.invoke(new VoidThrowingInvocation("setVariable(String, int)", variableName, value)
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _proxy.setVariable(variableName, value);
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @param variableName
     * @return
     * @throws MatlabInvocationException
     */
    public Object getVariable(final String variableName) throws MatlabInvocationException
    {
        return this.invoke(new ReturnThrowingInvocation<Object>("getVariable(String)", variableName)
        {
            @Override
            public Object invoke() throws MatlabInvocationException
            {
                return _proxy.getVariable(variableName);
            }
        });
    }
    
    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @param <U>
     * @param callable
     * @return
     * @throws MatlabInvocationException
     */
    public <U> U invokeAndWait(final MatlabThreadCallable<U> callable) throws MatlabInvocationException
    {
        return this.invoke(new ReturnThrowingInvocation<U>("invokeAndWait(MatlabThreadCallable)", callable)
        {
            @Override
            public U invoke() throws MatlabInvocationException
            {
                return _proxy.invokeAndWait(callable);
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @param listener 
     */
    public void addDisconnectionListener(final DisconnectionListener listener)
    {        
        this.invoke(new VoidInvocation("addDisconnectionListener(DisconnectionListener)", listener)
        {
            @Override
            public void invoke()
            {
                _proxy.addDisconnectionListener(listener);
            }
        });
        
    }
    
    /**
     *  Delegates to the proxy; logs the interaction.
     * 
     * @param listener 
     */
    public void removeDisconnectionListener(final DisconnectionListener listener)
    {
        this.invoke(new VoidInvocation("removeDisconnectionListener(DisconnectionListener)", listener)
        {
            @Override
            public void invoke()
            {
                _proxy.removeDisconnectionListener(listener);
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @return 
     */
    public boolean disconnect()
    {        
        return this.invoke(new ReturnBooleanInvocation("disconnect()")
        {
            @Override
            public boolean invoke()
            {
                return _proxy.disconnect();
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @return 
     */
    public boolean isExistingSession()
    {
        return this.invoke(new ReturnBooleanInvocation("isExistingSession()")
        {
            @Override
            public boolean invoke()
            {
                return _proxy.isExistingSession();
            }
        });
    }
        
    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @return 
     */
    public boolean isRunningInsideMatlab()
    {
        return this.invoke(new ReturnBooleanInvocation("isRunningInsideMatlab")
        {
            @Override
            public boolean invoke()
            {
                return _proxy.isRunningInsideMatlab();
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @return 
     */
    public boolean isConnected()
    {
        return this.invoke(new ReturnBooleanInvocation("isConnected()")
        {
            @Override
            public boolean invoke()
            {
                return _proxy.isConnected();
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @return 
     */
    public Identifier getIdentifier()
    {
        return this.invoke(new ReturnInvocation<Identifier>("getIdentifier()")
        {
            @Override
            public Identifier invoke()
            {
                return _proxy.getIdentifier();
            }
        });
    }

    /**
     * Delegates to the proxy; logs the interaction.
     * 
     * @throws MatlabInvocationException 
     */
    public void exit() throws MatlabInvocationException
    {
        this.invoke(new VoidThrowingInvocation("exit")
        {
            @Override
            public void invoke() throws MatlabInvocationException
            {
                _proxy.exit();
            }
        });
    }
        
    /**
     * Returns a brief description of this proxy. The exact details of this representation are unspecified and are
     * subject to change.
     * 
     * @return 
     */
    @Override
    public String toString()
    {
        return "[" + this.getClass().getName() + " proxy=" + _proxy + "]";
    }
    
    private String formatResult(Object result)
    {
        String formattedResult;
        
        if(result == null)
        {
            formattedResult = "null";
        }
        else if(result.getClass().isArray())
        {
            formattedResult = result.getClass().getName() + "\n" + formatResult(result, 0).trim();
        }
        else
        {
            formattedResult = result.getClass().getName() + ": " + result.toString();
        }
        
        return formattedResult;
    }
    
    /**
     * Takes in the result from MATLAB and turns it into an easily readable format.
     * 
     * @param result
     * @param level, pass in 0 to initialize, used recursively
     * @return description
     */
    private static String formatResult(Object result, int level)
    {
        //Message builder
        StringBuilder builder = new StringBuilder();
        
        //Tab offset for levels
        String tab = "";
        for(int i = 0; i < level + 1; i++)
        {
            tab += "  ";
        }
        
        //If the result is null
        if(result == null)
        {
            builder.append("null\n");
        }
        //If the result is an array
        else if(result.getClass().isArray())
        {
            Class<?> componentClass = result.getClass().getComponentType();
            
            //Primitive array
            if(componentClass.isPrimitive())
            {
                String componentName = componentClass.toString();
                int length = Array.getLength(result);
                
                builder.append(componentName);
                builder.append(" array, length = ");
                builder.append(length);
                builder.append("\n");
                
                for(int i = 0; i < length; i++)
                {   
                    builder.append(tab);
                    builder.append("index ");
                    builder.append(i);
                    builder.append(", ");
                    builder.append(componentName);
                    builder.append(": ");
                    builder.append(Array.get(result, i));
                    builder.append("\n");
                }
            }
            //Object array
            else
            {
                Object[] array = (Object[]) result;
                
                builder.append(array.getClass().getComponentType().getName());
                builder.append(" array, length = ");
                builder.append(array.length);
                builder.append("\n");
                
                for(int i = 0; i < array.length; i++)
                {   
                    builder.append(tab);
                    builder.append("index ");
                    builder.append(i);
                    builder.append(", ");
                    builder.append(formatResult(array[i], level + 1));
                }
            }
        }
        //If an Object and not an array
        else
        {   
            builder.append(result.getClass().getCanonicalName());
            builder.append(": ");
            builder.append(result);
            builder.append("\n");
        }
        
        return builder.toString();
    }
}
