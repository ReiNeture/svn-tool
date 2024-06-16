package fubuki.ref;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNUpdateClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import java.io.File;
import java.util.Set;

public class FileExporter {

	private final SVNClientManager clientManager;

	public FileExporter(SVNClientManager clientManager) {
		this.clientManager = clientManager;
	}

	public void exportFiles(SVNURL url, Set<String> modifiedFiles, long startRevision, long endRevision, String exportDir) {
		
		SVNUpdateClient updateClient = clientManager.getUpdateClient();

		SVNUtilities.cleanDirectory(exportDir);
		for (String file : modifiedFiles) {

			try {
//				if (!SVNUtilities.isBinaryFile(file) && DiffGenerator.isDiffEmpty(url, file, startRevision, endRevision, clientManager)) {
//					System.out.println("Skipping unmodified file: " + file);
//					continue;
//				}

				SVNURL fileUrl = url.appendPath(file, false);
				File exportFile = new File(exportDir, file);
				createParentDirs(exportFile);

				updateClient.doExport(fileUrl, exportFile, SVNRevision.create(endRevision),
						SVNRevision.create(endRevision), null, true, SVNDepth.FILES);

			} catch (SVNException e) {
				System.err.println(e.getMessage());
			}
		}
	}

	private void createParentDirs(File file) {
		File parentDir = file.getParentFile();
		if (!parentDir.exists()) {
			parentDir.mkdirs();
		}
	}
}
