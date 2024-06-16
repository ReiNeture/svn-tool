package fubuki.ref.entry;

import java.util.Date;
import java.util.Objects;

import org.tmatesoft.svn.core.SVNLogEntryPath;

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

        return Objects.equals(entryPath.getPath(), that.entryPath.getPath());
    }

    @Override
    public int hashCode() {
        return Objects.hash(entryPath.getPath());
    }
}
