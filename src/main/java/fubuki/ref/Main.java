package fubuki.ref;

import java.io.IOException;
import java.util.Set;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import fubuki.ref.entry.ModifiedFileEntry;

public class Main {

    public static void main(String[] args) {
        final String repoUrl = "http://192.168.18.207:8085/svn/TBB_edge";  // your svn repo URL
        final long startRevision = 346;
        final long endRevision = 352;
        final String outputDir = "./svn_diffs";
        final String exportDir = "./svn_source";
        final String reportPath = "./svn_report.xlsx";
        final boolean preserveFileStructure = true; // diff need to create directory structure for files?

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
            
            Set<ModifiedFileEntry> modifiedFiles = SVNUtilities.getModifiedFiles(url, startRevision, endRevision, clientManager);
            ExcelReportGenerator reportGenerator = new ExcelReportGenerator();
            reportGenerator.generateReport(modifiedFiles, reportPath, startRevision, endRevision, exportDir, url, clientManager);
            System.out.println("Excel report generated.");
            
        } catch (SVNException | IOException e) {
            e.printStackTrace();
        }
    }
}
