package info.kuee.boxer3.handlers;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.DbxPathV2;
import com.dropbox.core.v2.files.FileMetadata;

import info.kuee.boxer3.auth.AuthUtil;
import info.kuee.boxer3.ui.views.FileView;

public class DownloadHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IWorkbenchPart part = HandlerUtil.getActivePart(event);
		if(null != part && part instanceof FileView){
			FileView view = (FileView)part;
			String dropboxPath = "/" + view.getSelection();

			Shell shell = HandlerUtil.getActiveShell(event);
			FileDialog dlg = new FileDialog(shell, SWT.SAVE);
			dlg.setFileName(dropboxPath);
			String localPath = dlg.open();
	
			String pathError = DbxPathV2.findError(dropboxPath);
			if (null != pathError) {
				MessageDialog.openError(shell, "PathError", "Invalid Dropbox path. " + pathError);
				return null;
			}
	
			File localFile = new File(localPath);
			if (localFile.exists()) {
				MessageDialog.openError(shell, "FileError", "File already exists");
				return null;
			}

			DbxClientV2 dbxClient = AuthUtil.getClientV2("examples-download-file");
	
			downloadFile(dbxClient, dropboxPath, localFile);
		}

		return null;
	}

	/**
	 * Download a file
	 *
	 * @param dbxClient
	 *            Dropbox user authenticated client
	 * @param dropboxPath
	 *            Where to upload the file to within Dropbox
	 * @param localFIle
	 *            local file to upload
	 */
	private static void downloadFile(DbxClientV2 dbxClient, String dropboxPath, File localFile) {
		try (OutputStream out = new FileOutputStream(localFile)) {
			FileMetadata meta = dbxClient.files().downloadBuilder(dropboxPath).download(out);
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "downloadFile", meta.toStringMultiline());
		} catch (FileNotFoundException e) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "FileNotFoundException", e.getMessage());
		} catch (IOException e) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "IOException", e.getMessage());
		} catch (DbxException e) {
			MessageDialog.openError(Display.getCurrent().getActiveShell(), "DbxException", e.getMessage());
		}
	}

}
