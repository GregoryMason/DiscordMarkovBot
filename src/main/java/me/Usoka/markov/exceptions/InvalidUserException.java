package me.Usoka.markov.exceptions;

public class InvalidUserException extends Exception {

	/**
	 * Constructs a new <code>InvalidUserException</code> with no
	 * detail message.
	 */
	public InvalidUserException() { super(); }

	/**
	 * Constructs a new <code>InvalidUserException</code> with the
	 * specified detail message
	 * @param message the detail message
	 */
	public InvalidUserException(String message) { super(message); }

	/**
	 * Constructs a new <code>InvalidUserException</code> with the
	 * specified detail message and cause
	 *
	 * <p>Note that the detail message associated with <code>cause</code> is
	 * <i>not</i> automatically incorporated in this exception's detail
	 * message.
	 *
	 * @param message the detail message
	 * @param cause the cause. (A <tt>null</tt> value is permitted, and
	 *                 indicates that the cause is nonexistent or unknown.)
	 */
	public InvalidUserException(String message, Throwable cause) { super(message, cause); }

	/**
	 * Constructs a new <code>InvalidUserException</code> with the specified
	 * cause and a detail message of <tt>(cause==null ? null : cause.toString())
	 * </tt> (which typically contains the class and detail message of <tt>cause
	 * </tt>). This constructor is useful for exceptions that are little more than
	 * wrappers for other throwables.
	 *
	 * @param  cause the cause. (A <tt>null</tt> value is permitted, and indicates
	 *               that the cause is nonexistent or unknown.)
	 */
	public InvalidUserException(Throwable cause) { super(cause); }
}
