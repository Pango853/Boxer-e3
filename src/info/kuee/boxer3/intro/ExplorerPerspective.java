package info.kuee.boxer3.intro;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class ExplorerPerspective implements IPerspectiveFactory {

	public void createInitialLayout(IPageLayout layout) {
		//String editorArea = layout.getEditorArea();
		layout.setEditorAreaVisible(false);
	}
}
