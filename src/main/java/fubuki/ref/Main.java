package fubuki.ref;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import fubuki.ref.entry.ModifiedFileEntry;

public class Main {

    public static void main(String[] args) {
        final String repoUrl = "https://127.0.0.1/svn/yuuki";  // your svn repo URL
        final long startRevision = 14;
        final long endRevision = 17;
        final String outputDir = "./svn_diffs";
        final String exportDir = "./svn_source";
        final String reportPath = "./svn_report.xlsx";
        final boolean preserveFileStructure = false; // diff need to create directory structure for files?

        try {
            SVNURL url = SVNURL.parseURIEncoded(repoUrl);
            SVNClientManager clientManager = SVNClientManager.newInstance();

            Set<String> modifiedPaths = SVNUtilities.getModifiedPaths(url, startRevision, endRevision, clientManager);
            
            DiffGenerator diffGenerator = new DiffGenerator(clientManager);
            diffGenerator.generateDiffs(url, modifiedPaths, startRevision, endRevision, outputDir, preserveFileStructure);
            System.out.println("Diff generate completed.");
            
            FileExporter fileExporter = new FileExporter(clientManager);
            fileExporter.exportFiles(url, modifiedPaths, startRevision, endRevision, exportDir);
            System.out.println("Source Export completed.");
            
            List<ModifiedFileEntry> modifiedFiles = SVNUtilities.getModifiedFiles(url, startRevision, endRevision, clientManager);
            ExcelReportGenerator reportGenerator = new ExcelReportGenerator();
            reportGenerator.generateReport(modifiedFiles, reportPath, startRevision, endRevision, exportDir, url, clientManager);
            System.out.println("Excel report generated.");
            
        } catch (SVNException | IOException e) {
            e.printStackTrace();
        }
    }
}
