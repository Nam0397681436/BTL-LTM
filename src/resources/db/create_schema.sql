CREATE DATABASE IF NOT EXISTS game_service;
USE game_service;

DROP TABLE IF EXISTS player_matches;
DROP TABLE IF EXISTS matches;
DROP TABLE IF EXISTS players;

-- 1) Bảng players
CREATE TABLE players (
  player_id   VARCHAR(20)  PRIMARY KEY,
  username    VARCHAR(50)  NOT NULL,
  password    VARCHAR(255) NOT NULL,
  nick_name   VARCHAR(100) NOT NULL,
  total_score INT          DEFAULT 0,
  total_wins  INT          DEFAULT 0,
  UNIQUE KEY uq_players_nickname (nick_name),
  UNIQUE KEY uq_players_username (username) 
);

-- 2) Bảng matches
CREATE TABLE matches (
  match_id   INT AUTO_INCREMENT PRIMARY KEY,
  type       ENUM('ONE_VS_ONE','MULTIPLAYER') NOT NULL,
  creator_id VARCHAR(20),
  start_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  end_time   DATETIME,
  FOREIGN KEY (creator_id) REFERENCES players(player_id)
);

-- 3) Bảng player_matches
CREATE TABLE player_matches (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  match_id  INT NOT NULL,
  player_id VARCHAR(20) NOT NULL,
  score     INT DEFAULT 0,
  is_winner TINYINT(1) DEFAULT 0,
  is_host   TINYINT(1) DEFAULT 0,
  FOREIGN KEY (match_id) REFERENCES matches(match_id),
  FOREIGN KEY (player_id) REFERENCES players(player_id)
);
