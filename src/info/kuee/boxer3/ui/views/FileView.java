package info.kuee.boxer3.ui.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.part.ViewPart;

import com.dropbox.core.DbxException;
import com.dropbox.core.v1.DbxEntry;

import info.kuee.boxer3.auth.AuthUtil;

public class FileView extends ViewPart {
	private Table table;

	public FileView() {}

	@Override
	public void createPartControl(Composite parent) {
		table = new Table(parent, SWT.BORDER | SWT.FULL_SELECTION);

		// table.setLinesVisible(true);
		table.setHeaderVisible(true);

		TableColumn tcFileName = new TableColumn(table, SWT.LEFT);
		tcFileName.setText("File name");
		// tcFileName.setImage(new Image(shell.getDisplay(), "java2s.gif"));

		TableColumn tcFileSize = new TableColumn(table, SWT.RIGHT);
		tcFileSize.setText("Size");

		TableColumn tcDateModified = new TableColumn(table, SWT.RIGHT);
		tcDateModified.setText("Date Modified");

		tcFileName.setWidth(200);
		tcFileSize.setWidth(80);
		tcDateModified.setWidth(180);

		TableItem item = new TableItem(table, SWT.NULL);
		item.setText(new String[] { "Name", "Size" });

		table.setLayoutData(new GridData(GridData.FILL_BOTH));

		setInput();

		table.pack();
	}

	private void setInput() {
		// Remove all existing items.
		table.removeAll();

		int i=0;
		try {
			for(DbxEntry entry : AuthUtil.getClient().getMetadataWithChildren("/").children){
				TableItem item = new TableItem(table, SWT.NULL);
				item.setBackground(i++ % 2 == 0 ? Display.getCurrent().getSystemColor(SWT.COLOR_WHITE)
						: Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
				item.setText(0, entry.name);
				item.setText(1, entry.isFile() ? "File" : entry.isFolder() ? "Folder" : "?");
			}
		} catch (DbxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			// throw new IllegalArgumentException("Invalid directory. ");
		}
	}

	@Override
	public void setFocus() {
	}

	public String getSelection(){
		TableItem[] items = table.getSelection();
		if(0 < items.length){
			// TODO Multi file support
			TableItem item = items[0];
			if("File".equals(item.getText(1)))
				return item.getText(0);
		}
		return null;
	}
}
