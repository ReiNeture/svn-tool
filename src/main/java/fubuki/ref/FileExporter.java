package fubuki.ref;

import java.io.File;
import java.util.List;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;

import fubuki.ref.entry.ModifiedFileEntry;

public class FileExporter {

	private final SVNClientManager clientManager;

	public FileExporter(SVNClientManager clientManager) {
		this.clientManager = clientManager;
	}

	public void exportFiles(SVNURL url, List<ModifiedFileEntry> modifiedFiles, long revision, String exportDir) {
		
		SVNUpdateClient updateClient = clientManager.getUpdateClient();

		SVNUtilities.cleanDirectory(exportDir);
		
        for (ModifiedFileEntry fileEntry : modifiedFiles) {
            String file = fileEntry.getEntryPath().getPath();

			try {
				SVNURL fileUrl = url.appendPath(file, false);
				File exportFile = new File(exportDir, file);
//				createParentDirs(exportFile);

				updateClient.doExport(fileUrl, exportFile, SVNRevision.create(revision),
						SVNRevision.create(revision), null, true, SVNDepth.FILES);

			} catch (SVNException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	@SuppressWarnings("unused")
	private void createParentDirs(File file) {
		File parentDir = file.getParentFile();
		if (!parentDir.exists()) {
			parentDir.mkdirs();
		}
	}
}
