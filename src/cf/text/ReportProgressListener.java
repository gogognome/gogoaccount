/**
 * 
 */
package cf.text;

/**
 * This interface specifies a listener to monitor the progress of report creation. 
 * @author Sander Kooijmans
 */
public interface ReportProgressListener {

	/**
	 * This method may be called any time to notify that a part of report creation has been completed.
	 * @param percentageCompleted the percentage of the report creation process that has been completed
	 */
	public void onProgressUpdate(int percentageCompleted);
	
	/**
	 * This method is called when report creation has finished.
	 * @param t <code>null</code> if report creaion has finished successfully; otherwise
	 *         a {@link Throwable} is returned to indicate why report creation failed
	 */
	public void onFinished(Throwable t);
}
