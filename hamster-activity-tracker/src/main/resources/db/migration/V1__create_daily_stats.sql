CREATE TABLE IF NOT EXISTS daily_stats (
  date         date        NOT NULL,
  hamster_id   text        NOT NULL,
  total_rounds integer     NOT NULL,
  is_active    boolean     NOT NULL,
  updated_at   timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT pk_daily_stats PRIMARY KEY (date, hamster_id)
);