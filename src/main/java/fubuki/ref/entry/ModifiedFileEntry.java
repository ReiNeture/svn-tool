package fubuki.ref.entry;

import org.tmatesoft.svn.core.SVNLogEntryPath;

import java.util.Date;

public class ModifiedFileEntry {
    private final SVNLogEntryPath entryPath;
    private final Date commitDate;

    public ModifiedFileEntry(SVNLogEntryPath entryPath, Date commitDate) {
        this.entryPath = entryPath;
        this.commitDate = commitDate;
    }

    public SVNLogEntryPath getEntryPath() {
        return entryPath;
    }

    public Date getCommitDate() {
        return commitDate;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModifiedFileEntry that = (ModifiedFileEntry) o;

        if (!entryPath.getPath().equals(that.entryPath.getPath())) return false;
        return entryPath.getType() == that.entryPath.getType();
    }

    @Override
    public int hashCode() {
        int result = entryPath.getPath().hashCode();
        result = 31 * result + (int) entryPath.getType();
        return result;
    }
}
