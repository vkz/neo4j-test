-- -*- fill-column: 120; sql-product: sqlite; -*-

PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;
-- NOTE https://litestream.io/tips/#busy-timeout
PRAGMA busy_timeout = 5000;
-- NOTE https://litestream.io/tips/#synchronous-pragma
PRAGMA synchronous = NORMAL;
-- NOTE https://litestream.io/tips/#disable-autocheckpoints-for-high-write-load-servers


-- /usr/local/opt/sqlite3/bin/sqlite3 -column -header -bail -echo -init resources/db/migrations/0000.sql ~/.local/state/drillbot/drills.sqlite
