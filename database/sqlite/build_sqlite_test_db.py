#!/usr/bin/env python3
"""Build a local SQLite database that DBeaver can open immediately."""
from __future__ import annotations

import sqlite3
from pathlib import Path


HERE = Path(__file__).resolve().parent
DB_PATH = HERE / "growgate_be02_test.db"
SCHEMA_PATH = HERE / "schema.sql"
SEED_PATH = HERE / "seed.sql"
VERIFY_PATH = HERE / "verify.sql"


def execute_script(conn: sqlite3.Connection, path: Path) -> None:
    conn.executescript(path.read_text(encoding="utf-8"))


def main() -> None:
    if DB_PATH.exists():
        DB_PATH.unlink()

    with sqlite3.connect(DB_PATH) as conn:
        conn.execute("PRAGMA foreign_keys = ON")
        execute_script(conn, SCHEMA_PATH)
        execute_script(conn, SEED_PATH)

        cursor = conn.execute("PRAGMA foreign_key_check")
        fk_errors = cursor.fetchall()
        if fk_errors:
            raise SystemExit(f"Foreign key check failed: {fk_errors}")

        print(f"created={DB_PATH}")
        print("row_counts:")
        for table_name, row_count in conn.execute(VERIFY_PATH.read_text(encoding="utf-8").split(";")[0]):
            print(f"  {table_name}: {row_count}")


if __name__ == "__main__":
    main()
