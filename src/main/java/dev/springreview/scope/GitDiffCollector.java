package dev.springreview.scope;

import dev.springreview.exit.SpringReviewException;
import dev.springreview.observability.Logs;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.patch.FileHeader;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * M05：只读 JGit 收集 DIFF，精算新侧变更行范围。
 * 不 checkout、不 reset、不写 .git、不改工作区。
 */
public final class GitDiffCollector {

    private static final Logger LOG = Logs.logger("git-diff");
    private static final int MAX_CHANGED_FILES = 2000;

    public DiffResult collect(ReviewScope scope, PathFilters filters) {
        Logs.module("M05");
        try (Repository repo = openRepo(scope.repoRoot())) {
            ObjectId baseId = repo.resolve(scope.baseRef());
            ObjectId headId = repo.resolve(scope.headRef() == null ? "HEAD" : scope.headRef());
            if (baseId == null) {
                throw SpringReviewException.scope("baseRef 无法解析: " + scope.baseRef(), null);
            }
            if (headId == null) {
                throw SpringReviewException.scope("headRef 无法解析: " + scope.headRef(), null);
            }
            try (RevWalk rw = new RevWalk(repo)) {
                RevCommit base = rw.parseCommit(baseId);
                RevCommit head = rw.parseCommit(headId);
                AbstractTreeIterator baseTree = prepareTree(repo, base);
                AbstractTreeIterator headTree = prepareTree(repo, head);
                List<ChangedFile> changed = new ArrayList<>();
                Map<String, List<LineRange>> lineRanges = new LinkedHashMap<>();
                int added = 0, modified = 0, deleted = 0, renamed = 0,
                    binarySkipped = 0, nonJavaSkipped = 0, collected = 0;

                try (DiffFormatter df = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
                    df.setRepository(repo);
                    df.setDetectRenames(true);
                    List<DiffEntry> entries = df.scan(baseTree, headTree);
                    if (entries.size() > MAX_CHANGED_FILES) {
                        throw SpringReviewException.scope(
                            "变更文件数 " + entries.size() + " 超过上限 " + MAX_CHANGED_FILES, null);
                    }
                    for (DiffEntry e : entries) {
                        ChangedFile.ChangeType ct = map(e.getChangeType());
                        switch (ct) {
                            case ADD -> added++;
                            case MODIFY -> modified++;
                            case DELETE -> deleted++;
                            case RENAME -> renamed++;
                            case COPY -> renamed++;
                        }
                        String newPath = e.getNewPath();
                        String oldPath = e.getOldPath();
                        String path = ct == ChangedFile.ChangeType.DELETE ? oldPath : newPath;
                        boolean bin = isBinary(e) || hasBinaryMarker(df, e);
                        boolean isJava = path != null && path.endsWith(".java");
                        Path rel = Path.of(path);
                        boolean included = !bin && isJava && filters.includes(rel);
                        if (bin) {
                            binarySkipped++;
                        } else if (!isJava) {
                            nonJavaSkipped++;
                        } else if (included) {
                            collected++;
                        }

                        List<LineRange> ranges = List.of();
                        if (included && ct != ChangedFile.ChangeType.DELETE) {
                            ranges = editRanges(df, e);
                        }
                        changed.add(new ChangedFile(path, oldPath, ct,
                            isJava ? "java" : "other", bin,
                            ct == ChangedFile.ChangeType.DELETE, ranges, included));
                        lineRanges.put(path, ranges);
                    }
                }
                LOG.info("diff collected base={} head={} total={} collected={} binary={} nonJava={}",
                    scope.baseRef(), scope.headRef(), changed.size(), collected,
                    binarySkipped, nonJavaSkipped);
                return new DiffResult(
                    scope.repoRoot().toString(),
                    scope.baseRef(),
                    scope.headRef() == null ? "HEAD" : scope.headRef(),
                    base.getName(),
                    head.getName(),
                    changed,
                    lineRanges,
                    new DiffResult.Stats(changed.size(), added, modified, deleted, renamed,
                        binarySkipped, 0, nonJavaSkipped, collected),
                    List.of()
                );
            }
        } catch (IOException ex) {
            throw SpringReviewException.scope("JGit diff 失败: " + ex.getMessage(), ex);
        }
    }

    // ---------- EditList 精算 ----------

    private static List<LineRange> editRanges(DiffFormatter df, DiffEntry e) {
        try {
            FileHeader fh = df.toFileHeader(e);
            if (fh == null) {
                return List.of();
            }
            if (fh.getPatchType() == FileHeader.PatchType.BINARY) {
                return List.of();
            }
            EditList edits = fh.toEditList();
            if (edits.isEmpty()) {
                return List.of();
            }
            List<LineRange> raw = new ArrayList<>(edits.size());
            for (Edit edit : edits) {
                int beginB = edit.getBeginB();
                int endB = edit.getEndB();
                if (endB <= beginB) {
                    continue;
                }
                int start = beginB + 1;
                int end = endB;
                LineRange.ChangeType type = edit.getEndA() == edit.getBeginA()
                    ? LineRange.ChangeType.ADD
                    : LineRange.ChangeType.MODIFY;
                raw.add(new LineRange(start, end, LineRange.Side.NEW, type));
            }
            return mergeAdjacent(raw);
        } catch (IOException ex) {
            LOG.warn("edit list parse failed for {}: {}", e.getNewPath(), ex.getMessage());
            return List.of();
        }
    }

    private static List<LineRange> mergeAdjacent(List<LineRange> ranges) {
        if (ranges.size() <= 1) {
            return ranges;
        }
        List<LineRange> sorted = new ArrayList<>(ranges);
        sorted.sort(Comparator.comparingInt(LineRange::start).thenComparingInt(LineRange::end));
        List<LineRange> merged = new ArrayList<>();
        LineRange current = sorted.get(0);
        for (int i = 1; i < sorted.size(); i++) {
            LineRange next = sorted.get(i);
            if (current.end() + 1 >= next.start()) {
                int start = Math.min(current.start(), next.start());
                int end = Math.max(current.end(), next.end());
                LineRange.ChangeType type = (current.changeType() == LineRange.ChangeType.MODIFY
                    || next.changeType() == LineRange.ChangeType.MODIFY)
                    ? LineRange.ChangeType.MODIFY
                    : current.changeType();
                current = new LineRange(start, end, LineRange.Side.NEW, type);
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return List.copyOf(merged);
    }

    // ---------- repo ----------

    /**
     * 打开 JGit Repository。
     * 显式查找 .git 目录：JGit 的 findGitDir 在找不到时不报错，
     * 会在 build() 时才抛 "One of setGitDir or setWorkTree must be called"。
     * 这里直接遍历向上查找，找不到抛清晰错误（退出码 3）。
     */
    private static Repository openRepo(Path repoRoot) throws IOException {
        File gitDir = findGitDir(repoRoot);
        if (gitDir == null) {
            throw SpringReviewException.scope(
                "不是 Git 仓库或找不到 .git 目录: " + repoRoot
                    + "（DIFF 模式需要 Git 仓库；MODULE/FILES 模式不需要）", null);
        }
        return new org.eclipse.jgit.storage.file.FileRepositoryBuilder()
            .setGitDir(gitDir)
            .setMustExist(true)
            .build();
    }

    /** 向上遍历目录树查找 .git（文件或目录均可）。 */
    private static File findGitDir(Path start) {
        if (start == null) {
            return null;
        }
        Path cur = start.toAbsolutePath().normalize();
        while (cur != null) {
            Path dotGit = cur.resolve(".git");
            if (Files.exists(dotGit)) {
                return dotGit.toFile();
            }
            cur = cur.getParent();
        }
        return null;
    }

    private static AbstractTreeIterator prepareTree(Repository repo, RevCommit commit) throws IOException {
        try (ObjectReader reader = repo.newObjectReader()) {
            CanonicalTreeParser p = new CanonicalTreeParser();
            p.reset(reader, commit.getTree().getId());
            return p;
        }
    }

    private static boolean isBinary(DiffEntry e) {
        String p = e.getNewPath();
        if (p == null) {
            p = e.getOldPath();
        }
        if (p == null) {
            return false;
        }
        String lower = p.toLowerCase();
        return lower.endsWith(".class") || lower.endsWith(".jar")
            || lower.endsWith(".war") || lower.endsWith(".ear")
            || lower.endsWith(".so") || lower.endsWith(".dll")
            || lower.endsWith(".exe") || lower.endsWith(".bin")
            || lower.endsWith(".png") || lower.endsWith(".jpg")
            || lower.endsWith(".jpeg") || lower.endsWith(".gif")
            || lower.endsWith(".webp") || lower.endsWith(".ico")
            || lower.endsWith(".zip") || lower.endsWith(".tar")
            || lower.endsWith(".gz") || lower.endsWith(".7z")
            || lower.endsWith(".rar");
    }

    private static boolean hasBinaryMarker(DiffFormatter df, DiffEntry e) {
        try {
            FileHeader fh = df.toFileHeader(e);
            return fh != null && fh.getPatchType() == FileHeader.PatchType.BINARY;
        } catch (IOException ex) {
            return false;
        }
    }

    private static ChangedFile.ChangeType map(DiffEntry.ChangeType ct) {
        return switch (ct) {
            case ADD -> ChangedFile.ChangeType.ADD;
            case MODIFY -> ChangedFile.ChangeType.MODIFY;
            case DELETE -> ChangedFile.ChangeType.DELETE;
            case RENAME -> ChangedFile.ChangeType.RENAME;
            case COPY -> ChangedFile.ChangeType.COPY;
        };
    }
}
