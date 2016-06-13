package info.kuee.boxer3.handlers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;

import com.dropbox.core.DbxException;
import com.dropbox.core.NetworkIOException;
import com.dropbox.core.RetryException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.DbxPathV2;
import com.dropbox.core.v2.files.CommitInfo;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.UploadErrorException;
import com.dropbox.core.v2.files.UploadSessionCursor;
import com.dropbox.core.v2.files.UploadSessionFinishErrorException;
import com.dropbox.core.v2.files.UploadSessionLookupErrorException;
import com.dropbox.core.v2.files.WriteMode;

import info.kuee.boxer3.auth.AuthUtil;

public class UploadHandler extends AbstractHandler {
	// Adjust the chunk size based on your network speed and reliability. Larger chunk sizes will
	// result in fewer network requests, which will be faster. But if an error occurs, the entire
	// chunk will be lost and have to be re-uploaded. Use a multiple of 4MiB for your chunk size.
	private static final long CHUNKED_UPLOAD_CHUNK_SIZE = 8L << 20; // 8MiB
	private static final int CHUNKED_UPLOAD_MAX_ATTEMPTS = 5;

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		Shell shell = window.getShell();

		FileDialog dlg = new FileDialog(shell, SWT.OPEN);
		String localPath = dlg.open();
		String dropboxPath = "/"+dlg.getFileName();

		String pathError = DbxPathV2.findError(dropboxPath);
		if (null != pathError) {
			MessageDialog.openError(shell, "PathError", "Invalid Dropbox path. " + pathError);
			return null;
		}

		File localFile = new File(localPath);
		if (!localFile.exists()) {
			MessageDialog.openError(shell, "FileError", "File does not exist");
			return null;
		}

		if (!localFile.isFile()) {
			MessageDialog.openError(shell, "FileError", "File is not valid");
			return null;
		}

		DbxClientV2 dbxClient = AuthUtil.getClientV2("examples-download-file");

		// upload the file with simple upload API if it is small enough, otherwise use chunked
		// upload API for better performance. Arbitrarily chose 2 times our chunk size as the
		// deciding factor. This should really depend on your network.
		if (localFile.length() <= (2 * CHUNKED_UPLOAD_CHUNK_SIZE)) {
			uploadFile(dbxClient, localFile, dropboxPath);
		} else {
			chunkedUploadFile(dbxClient, localFile, dropboxPath);
		}		

		return null;
	}

	/**
	 * Uploads a file in a single request. This approach is preferred for small files since it eliminates unnecessary
	 * round-trips to the servers.
	 *
	 * @param dbxClient
	 *            Dropbox user authenticated client
	 * @param localFIle
	 *            local file to upload
	 * @param dropboxPath
	 *            Where to upload the file to within Dropbox
	 */
	private static void uploadFile(DbxClientV2 dbxClient, File localFile, String dropboxPath) {
		try (InputStream in = new FileInputStream(localFile)) {
			FileMetadata metadata = dbxClient.files().uploadBuilder(dropboxPath).withMode(WriteMode.ADD)
					.withClientModified(new Date(localFile.lastModified())).uploadAndFinish(in);

			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "uploadFile", metadata.toStringMultiline());
		} catch (UploadErrorException ex) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "UploadErrorException", "Error uploading to Dropbox: " + ex.getMessage());
		} catch (DbxException ex) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "DbxException", "Error uploading to Dropbox: " + ex.getMessage());
		} catch (IOException ex) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "IOException", "Error reading from file \"" + localFile + "\": " + ex.getMessage());
		}
	}

	/**
	 * Uploads a file in chunks using multiple requests. This approach is preferred for larger files since it allows for
	 * more efficient processing of the file contents on the server side and also allows partial uploads to be retried
	 * (e.g. network connection problem will not cause you to re-upload all the bytes).
	 *
	 * @param dbxClient
	 *            Dropbox user authenticated client
	 * @param localFIle
	 *            local file to upload
	 * @param dropboxPath
	 *            Where to upload the file to within Dropbox
	 */
	private static void chunkedUploadFile(DbxClientV2 dbxClient, File localFile, String dropboxPath) {
		long size = localFile.length();

		// assert our file is at least the chunk upload size. We make this assumption in the code
		// below to simplify the logic.
		if (size < CHUNKED_UPLOAD_CHUNK_SIZE) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "chunkedUploadFile", "File too small, use upload() instead.");
			return;
		}

		long uploaded = 0L;
		DbxException thrown = null;

		// Chunked uploads have 3 phases, each of which can accept uploaded bytes:
		//
		// (1) Start: initiate the upload and get an upload session ID
		// (2) Append: upload chunks of the file to append to our session
		// (3) Finish: commit the upload and close the session
		//
		// We track how many bytes we uploaded to determine which phase we should be in.
		String sessionId = null;
		for (int i = 0; i < CHUNKED_UPLOAD_MAX_ATTEMPTS; ++i) {
			if (i > 0) {
				MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "chunkedUploadFile", String
						.format("Retrying chunked upload (%d / %d attempts)", i + 1, CHUNKED_UPLOAD_MAX_ATTEMPTS));
			}

			try (InputStream in = new FileInputStream(localFile)) {
				// if this is a retry, make sure seek to the correct offset
				in.skip(uploaded);

				// (1) Start
				if (sessionId == null) {
					sessionId = dbxClient.files().uploadSessionStart().uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE)
							.getSessionId();
					uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
					printProgress(uploaded, size);
				}

				UploadSessionCursor cursor = new UploadSessionCursor(sessionId, uploaded);

				// (2) Append
				while ((size - uploaded) > CHUNKED_UPLOAD_CHUNK_SIZE) {
					dbxClient.files().uploadSessionAppendV2(cursor).uploadAndFinish(in, CHUNKED_UPLOAD_CHUNK_SIZE);
					uploaded += CHUNKED_UPLOAD_CHUNK_SIZE;
					printProgress(uploaded, size);
					cursor = new UploadSessionCursor(sessionId, uploaded);
				}

				// (3) Finish
				long remaining = size - uploaded;
				CommitInfo commitInfo = CommitInfo.newBuilder(dropboxPath).withMode(WriteMode.ADD)
						.withClientModified(new Date(localFile.lastModified())).build();
				FileMetadata metadata = dbxClient.files().uploadSessionFinish(cursor, commitInfo).uploadAndFinish(in,
						remaining);

				MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "chunkedUploadFile", metadata.toStringMultiline());
				return;
			} catch (RetryException ex) {
				thrown = ex;
				// RetryExceptions are never automatically retried by the client for uploads. Must
				// catch this exception even if DbxRequestConfig.getMaxRetries() > 0.
				sleepQuietly(ex.getBackoffMillis());
				continue;
			} catch (NetworkIOException ex) {
				thrown = ex;
				// network issue with Dropbox (maybe a timeout?) try again
				continue;
			} catch (UploadSessionLookupErrorException ex) {
				if (ex.errorValue.isIncorrectOffset()) {
					thrown = ex;
					// server offset into the stream doesn't match our offset (uploaded). Seek to
					// the expected offset according to the server and try again.
					uploaded = ex.errorValue.getIncorrectOffsetValue().getCorrectOffset();
					continue;
				} else {
					// Some other error occurred, give up.
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "chunkedUploadFile", "Error uploading to Dropbox: " + ex.getMessage());
					return;
				}
			} catch (UploadSessionFinishErrorException ex) {
				if (ex.errorValue.isLookupFailed() && ex.errorValue.getLookupFailedValue().isIncorrectOffset()) {
					thrown = ex;
					// server offset into the stream doesn't match our offset (uploaded). Seek to
					// the expected offset according to the server and try again.
					uploaded = ex.errorValue.getLookupFailedValue().getIncorrectOffsetValue().getCorrectOffset();
					continue;
				} else {
					// some other error occurred, give up.
					MessageDialog.openError(Display.getCurrent().getActiveShell(), "chunkedUploadFile", "Error uploading to Dropbox: " + ex.getMessage());
					return;
				}
			} catch (DbxException ex) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "DbxException", "Error uploading to Dropbox: " + ex.getMessage());
				return;
			} catch (IOException ex) {
				MessageDialog.openError(Display.getCurrent().getActiveShell(), "IOException", "Error reading from file \"" + localFile + "\": " + ex.getMessage());
				return;
			}
		}

		// if we made it here, then we must have run out of attempts
		MessageDialog.openError(Display.getCurrent().getActiveShell(), "chunkedUploadFile", "Maxed out upload attempts to Dropbox. Most recent error: " + thrown.getMessage());
	}

	private static void printProgress(long uploaded, long size) {
		System.out.printf("Uploaded %12d / %12d bytes (%5.2f%%)\n", uploaded, size, 100 * (uploaded / (double) size));
	}

	private static void sleepQuietly(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException ex) {
			// just exit
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "sleepQuietly", "Error uploading to Dropbox: interrupted during backoff.");
		}
	}

}
