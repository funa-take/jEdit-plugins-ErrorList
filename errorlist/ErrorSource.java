/*
 * ErrorSource.java - An error source
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999 Slava Pestov
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package errorlist;

//{{{ Imports
import org.gjt.sp.jedit.*;

import errorlist.DefaultErrorSource.DefaultError;

import java.util.Vector;
//}}}

/**
 * Abstract interface for a named error source. 
 *
 * Most plugins can more easily extend DefaultErrorSource instead of
 * implementing all the abstract methods in this class. 
 * 
 * @author Slava Pestov
 * @version $Id: ErrorSource.java 21839 2012-06-19 17:20:43Z ezust $
 */
public abstract class ErrorSource
{
	//{{{ Static part

	//{{{ registerErrorSource() method
	/**
	 * Registers an error source, making the errors visible.
	 * @param errorSource The error source
	 */
	public static void registerErrorSource(ErrorSource errorSource)
	{
		// We must block potential addError calls during
		// source registering. The number of errors must not
		// change now. Synchronization on the source will do.
		synchronized(errorSource)
		{
			if(errorSource.registered)
				return;
			synchronized(errorSources)
			{
				errorSources.addElement(errorSource);
				errorSource.registered = true;
				cachedErrorSources = null;
			}
			// It is important that at the moment of registering
			// the source to gui, it is registered with the
			// number of errors from the moment of switching
			// the `registered` flag, that is from now.
			// Hence we pack the errors into the message.
			EditBus.sendAsync(new ErrorSourceUpdate(
				errorSource,
				ErrorSourceUpdate.ERROR_SOURCE_ADDED,
				errorSource.getAllErrors()));
		}
	} //}}}

	//{{{ unregisterErrorSource() method
	/**
	 * Unregisters an error source.
	 * @param errorSource The error source
	 */
	public static void unregisterErrorSource(ErrorSource errorSource)
	{
		if(!errorSource.registered)
			return;

		EditBus.removeFromBus(errorSource);

		synchronized(errorSources)
		{
			errorSources.removeElement(errorSource);
			errorSource.registered = false;
			cachedErrorSources = null;
		}
		// No additional care is needed when removing the
		// error source. It doesn't matter if new errors are
		// added to the source before the gui unregisters it.
		EditBus.sendAsync(new ErrorSourceUpdate(errorSource,
			ErrorSourceUpdate.ERROR_SOURCE_REMOVED));
	} //}}}

	//{{{ getErrorSources() method
	/**
	 * Returns an array of registered error sources.
	 */
	public static ErrorSource[] getErrorSources()
	{
		synchronized(errorSources)
		{
			if(cachedErrorSources == null)
			{
				cachedErrorSources = new ErrorSource[
					errorSources.size()];
				errorSources.copyInto(cachedErrorSources);
			}
			return cachedErrorSources;
		}
	} //}}}
	//}}}
	
	// {{{ addError()
	/**
	 * This should be abstract but I do not want to
	 * break existing plugins.
	 * 
	 * @since jedit 4.3pre3
	 */
	public void addError(final DefaultError error) {}
	// }}}
	
	// {{{ getView()
	/** @return the View this ErrorSource belongs to, 
	  * or null if the errors should be sent to all Views. 
	  */
	public View getView() {return null;} // }}}
	
	//{{{ Constants
	/**
	 * An error.
	 */
	public static final int ERROR = 0;

	/**
	 * A warning.
	 */
	public static final int WARNING = 1;
	//}}}

	//{{{ getName() method
	/**
	 * Returns a string description of this error source.
	 */
	public abstract String getName();
	//}}}

	//{{{ getErrorCount() method
	/**
	 * Returns the number of errors in this source.
	 */
	public abstract int getErrorCount();
	//}}}

	//{{{ getAllErrors() method
	/**
	 * Returns an array of all errors in this error source.
	 */
	public abstract Error[] getAllErrors();
	//}}}

	//{{{ getFileErrorCount() method
	/**
	 * Returns the number of errors in the specified file.
	 * @param path Full path name
	 */
	public abstract int getFileErrorCount(String path);
	//}}}

	//{{{ getFileErrors() method
	/**
	 * Returns all errors in the specified file.
	 * @param path Full path name
	 */
	public abstract Error[] getFileErrors(String path);
	//}}}

	//{{{ getLineErrors() method
	/**
	 * Returns all errors in the specified line range.
	 * @param path The file path
	 * @param startLineIndex The line number
	 * @param endLineIndex The line number
	 * @since ErrorList 1.3
	 */
	public abstract ErrorSource.Error[] getLineErrors(String path,
		int startLineIndex, int endLineIndex);
	//}}}

	//{{{ Private members

	// unregistered error sources do not fire events.
	// the console uses this fact to 'batch' multiple errors together
	// for improved performance
	protected boolean registered;

	private static Vector errorSources = new Vector();
	private static ErrorSource[] cachedErrorSources;
	//}}}

	//{{{ Error interface
	/**
	 * An error.
	 */
	public interface Error
	{
		//{{{ getErrorType() method
		/**
		 * Returns the error type (error or warning)
		 */
		int getErrorType();
		//}}}

		//{{{ getErrorSource() method
		/**
		 * Returns the source of this error.
		 */
		ErrorSource getErrorSource();
		//}}}

		//{{{ getBuffer() method
		/**
		 * Returns the buffer involved, or null if it is not open.
		 */
		Buffer getBuffer();
		//}}}

		//{{{ getFilePath() method
		/**
		 * Returns the file path name involved.
		 */
		String getFilePath();
		//}}}

		//{{{ getFileName() method
		/**
		 * Returns just the name portion of the file involved.
		 */
		String getFileName();
		//}}}

		//{{{ getLineNumber() method
		/**
		 * Returns the line number.
		 */
		int getLineNumber();
		//}}}

		//{{{ getStartOffset() method
		/**
		 * Returns the start offset.
		 */
		int getStartOffset();
		//}}}

		//{{{ getEndOffset() method
		/**
		 * Returns the end offset.
		 */
		int getEndOffset();
		//}}}

		//{{{ getErrorMessage() method
		/**
		 * Returns the error message.
		 */
		String getErrorMessage();
		//}}}

		//{{{ getExtraMessages() method
		/**
		 * Returns the extra error messages.
		 */
		String[] getExtraMessages();
		//}}}
	} //}}}
}
