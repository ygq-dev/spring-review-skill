# -*- coding: utf-8 -*-
"""
H3：生成性能测试夹具。
- 创建临时 Git 仓库
- 初始 commit：20 个健康 Java 文件
- 二次 commit：修改 1 个文件，插入约 1000 行（含少量真缺陷，确保规则引擎有活干）
输出路径：target/perf-fixture/
"""
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
FIXTURE = ROOT / "target/perf-fixture"
REPO = FIXTURE / "repo"


def git(args, cwd):
    subprocess.run(["git"] + args, cwd=cwd, check=True,
                   stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)


def gen_healthy_file(i):
    return f"""package com.example.perf;

public class Healthy{i:03d} {{
    private final String name;
    private final long id;

    public Healthy{i:03d}(String name, long id) {{
        this.name = name;
        this.id = id;
    }}

    public String getName() {{
        return name;
    }}

    public long getId() {{
        return id;
    }}

    public String describe() {{
        return "Healthy{i:03d}:" + name + "#" + id;
    }}
}}
"""


def gen_modified_file(lines):
    """生成一个大文件，行数约为 lines。前 3 行含真缺陷，其余为无害代码。"""
    header = """package com.example.perf;

public class BigService {
    private String password = "admin123456";

    public void logUser(String userId) {
        System.out.println("user=" + userId);
    }

    public String findUser(String name) {
        return "SELECT * FROM users WHERE name = '" + name + "'";
    }
"""
    body = []
    for i in range(lines):
        body.append(f"    public String method{i:04d}(String v) {{ return \"m{i:04d}:\" + v; }}")
    footer = "}\n"
    return header + "\n".join(body) + "\n" + footer


def main():
    if FIXTURE.exists():
        shutil.rmtree(FIXTURE)
    REPO.mkdir(parents=True)

    # init
    git(["init", "-q"], REPO)
    git(["config", "user.email", "perf@example.com"], REPO)
    git(["config", "user.name", "perf"], REPO)

    # 初始：20 个健康文件
    pkg = REPO / "src/main/java/com/example/perf"
    pkg.mkdir(parents=True)
    for i in range(1, 21):
        (pkg / f"Healthy{i:03d}.java").write_text(gen_healthy_file(i), encoding="utf-8")
    git(["add", "."], REPO)
    git(["commit", "-q", "-m", "init"], REPO)

    # 二次 commit：新增 BigService.java（约 1000 行）
    big = gen_modified_file(1000)
    (pkg / "BigService.java").write_text(big, encoding="utf-8")
    git(["add", "."], REPO)
    git(["commit", "-q", "-m", "add BigService"], REPO)

    # 统计 diff 行数
    diff = subprocess.run(
        ["git", "diff", "--shortstat", "HEAD~1", "HEAD"],
        cwd=REPO, capture_output=True, text=True, check=True
    ).stdout.strip()
    print(f"Fixture ready: {REPO}")
    print(f"Diff shortstat: {diff}")


if __name__ == "__main__":
    main()