CREATE TABLE users (
   id SERIAL PRIMARY KEY,
   username VARCHAR(50) UNIQUE NOT NULL,
   password VARCHAR(100) NOT NULL
);

CREATE TABLE todos (
   id BIGSERIAL PRIMARY KEY,
   user_specific_id INTEGER NOT NULL, 
   user_id INTEGER REFERENCES users(id),
   title VARCHAR(20) NOT NULL,
   description VARCHAR(50) NOT NULL,
   due_date DATE NOT NULL,
   priority VARCHAR(10) NOT NULL,
   tag VARCHAR(10) NOT NULL,
   completed BOOLEAN DEFAULT FALSE,
   status VARCHAR(10) DEFAULT 'PENDING'
);