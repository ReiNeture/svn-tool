package fubuki.ref.entry;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import org.tmatesoft.svn.core.SVNLogEntryPath;

public class ModifiedFileEntry {
    private final SVNLogEntryPath entryPath;
    private Date commitDate;
    private final List<Character> operations;
    private long lastCommitRevision;

    public ModifiedFileEntry(SVNLogEntryPath entryPath, Date commitDate, long commitRevision) {
        this.entryPath = entryPath;
        this.commitDate = commitDate;
        this.operations = new ArrayList<>();
        this.operations.add(entryPath.getType());
        this.lastCommitRevision = commitRevision;
    }

    public SVNLogEntryPath getEntryPath() {
        return entryPath;
    }

    public Date getCommitDate() {
        return commitDate;
    }

    public List<Character> getOperations() {
        return operations;
    }

    public void addOperation(char type, long commitRevision, Date commitDate) {
        this.operations.add(type);
        this.lastCommitRevision = commitRevision;
        this.commitDate = commitDate;
    }

    public long getLastCommitRevision() {
        return lastCommitRevision;
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