CREATE TABLE artista (
  id			 SERIAL,
  name			 VARCHAR(512) NOT NULL,
  description		 VARCHAR(512) NOT NULL,
  songwriter_issongwriter BOOL,
  band_isband		 BOOL,
  musician_ismusician	 BOOL,
  composer_iscomposer	 BOOL,
  PRIMARY KEY(id)
);

CREATE TABLE music (
  id	 SERIAL,
  title	 VARCHAR(512) NOT NULL,
  length INTEGER NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE publisher (
  id	 SERIAL,
  name VARCHAR(512) UNIQUE NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE album (
  id		 SERIAL,
  name	 VARCHAR(512) UNIQUE NOT NULL,
  description	 VARCHAR(512) NOT NULL,
  genre	 VARCHAR(512) NOT NULL,
  length	 INTEGER UNIQUE,
  publisher_id INTEGER NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE critic (
  id		 SERIAL,
  score	 FLOAT(8) NOT NULL,
  text		 VARCHAR(512) NOT NULL,
  album_id	 INTEGER NOT NULL,
  utilizador_id INTEGER NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE concert (
  id		 SERIAL,
  name	 VARCHAR(512) NOT NULL,
  description VARCHAR(512) NOT NULL,
  location	 VARCHAR(512) NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE utilizador (
  id	 SERIAL UNIQUE,
  username VARCHAR(512) UNIQUE NOT NULL,
  password VARCHAR(512) NOT NULL,
  iseditor BOOL NOT NULL,
  PRIMARY KEY(id)
);

CREATE TABLE filearchive (
  path		 VARCHAR(512) NOT NULL,
  utilizador_id INTEGER,
  music_id	 INTEGER,
  PRIMARY KEY(utilizador_id,music_id)
);

CREATE TABLE belongs (
  inicio	 DATE,
  fim	 DATE,
  artista_id INTEGER,
  PRIMARY KEY(artista_id)
);

CREATE TABLE playlist (
  id		 SERIAL UNIQUE,
  name		 VARCHAR(512) NOT NULL,
  utilizador_id INTEGER,
  PRIMARY KEY(id,utilizador_id)
);

CREATE TABLE playlist_music (
  playlist_id		 INTEGER,
  playlist_utilizador_id INTEGER,
  music_id		 INTEGER,
  PRIMARY KEY(playlist_id,playlist_utilizador_id,music_id)
);

CREATE TABLE album_music (
  album_id INTEGER,
  music_id INTEGER,
  PRIMARY KEY(album_id,music_id)
);

CREATE TABLE music_songwriter (
  music_id	 INTEGER,
  artista_id INTEGER,
  PRIMARY KEY(music_id,artista_id)
);

CREATE TABLE artista_album (
  artista_id INTEGER NOT NULL,
  album_id	 INTEGER,
  PRIMARY KEY(album_id)
);

CREATE TABLE artista_music (
  artista_id INTEGER NOT NULL,
  music_id	 INTEGER,
  PRIMARY KEY(music_id)
);

CREATE TABLE concert_artista (
  concert_id INTEGER,
  artista_id INTEGER NOT NULL,
  PRIMARY KEY(concert_id)
);

CREATE TABLE composer_music (
  artista_id INTEGER,
  music_id	 INTEGER,
  PRIMARY KEY(artista_id,music_id)
);

CREATE TABLE utilizador_filearchive (
  utilizador_id		 INTEGER,
  filearchive_utilizador_id INTEGER,
  filearchive_music_id	 INTEGER,
  PRIMARY KEY(utilizador_id,filearchive_utilizador_id,filearchive_music_id)
);

ALTER TABLE album ADD CONSTRAINT album_fk1 FOREIGN KEY (publisher_id) REFERENCES publisher(id);
ALTER TABLE critic ADD CONSTRAINT critic_fk1 FOREIGN KEY (album_id) REFERENCES album(id);
ALTER TABLE critic ADD CONSTRAINT critic_fk2 FOREIGN KEY (utilizador_id) REFERENCES utilizador(id);
ALTER TABLE filearchive ADD CONSTRAINT filearchive_fk1 FOREIGN KEY (utilizador_id) REFERENCES utilizador(id);
ALTER TABLE filearchive ADD CONSTRAINT filearchive_fk2 FOREIGN KEY (music_id) REFERENCES music(id);
ALTER TABLE belongs ADD CONSTRAINT belongs_fk1 FOREIGN KEY (artista_id) REFERENCES artista(id);
ALTER TABLE playlist ADD CONSTRAINT playlist_fk1 FOREIGN KEY (utilizador_id) REFERENCES utilizador(id);
ALTER TABLE playlist_music ADD CONSTRAINT playlist_music_fk1 FOREIGN KEY (playlist_id,playlist_utilizador_id) REFERENCES playlist(id,utilizador_id);
ALTER TABLE playlist_music ADD CONSTRAINT playlist_music_fk3 FOREIGN KEY (music_id) REFERENCES music(id);
ALTER TABLE album_music ADD CONSTRAINT album_music_fk1 FOREIGN KEY (album_id) REFERENCES album(id);
ALTER TABLE album_music ADD CONSTRAINT album_music_fk2 FOREIGN KEY (music_id) REFERENCES music(id);
ALTER TABLE music_songwriter ADD CONSTRAINT music_songwriter_fk1 FOREIGN KEY (music_id) REFERENCES music(id);
ALTER TABLE music_songwriter ADD CONSTRAINT music_songwriter_fk2 FOREIGN KEY (artista_id) REFERENCES artista(id);
ALTER TABLE artista_album ADD CONSTRAINT artista_album_fk1 FOREIGN KEY (artista_id) REFERENCES artista(id);
ALTER TABLE artista_album ADD CONSTRAINT artista_album_fk2 FOREIGN KEY (album_id) REFERENCES album(id);
ALTER TABLE artista_music ADD CONSTRAINT artista_music_fk1 FOREIGN KEY (artista_id) REFERENCES artista(id);
ALTER TABLE artista_music ADD CONSTRAINT artista_music_fk2 FOREIGN KEY (music_id) REFERENCES music(id);
ALTER TABLE concert_artista ADD CONSTRAINT concert_artista_fk1 FOREIGN KEY (concert_id) REFERENCES concert(id);
ALTER TABLE concert_artista ADD CONSTRAINT concert_artista_fk2 FOREIGN KEY (artista_id) REFERENCES artista(id);
ALTER TABLE composer_music ADD CONSTRAINT composer_music_fk1 FOREIGN KEY (artista_id) REFERENCES artista(id);
ALTER TABLE composer_music ADD CONSTRAINT composer_music_fk2 FOREIGN KEY (music_id) REFERENCES music(id);
ALTER TABLE utilizador_filearchive ADD CONSTRAINT utilizador_filearchive_fk1 FOREIGN KEY (utilizador_id) REFERENCES utilizador(id);
ALTER TABLE utilizador_filearchive ADD CONSTRAINT utilizador_filearchive_fk2 FOREIGN KEY (filearchive_utilizador_id,filearchive_music_id) REFERENCES filearchive(utilizador_id,music_id);
